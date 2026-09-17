/*
 * iptables_setup.c - ArielOS DNS enforcement rules for dnscrypt-proxy
 *
 * Overview
 * --------
 * All plain DNS traffic (port 53) originating on the device is forced through
 * the local dnscrypt-proxy instance, which listens on 127.0.0.1:55 / [::1]:55.
 *
 *   IPv4: nat/OUTPUT -> ariel_dns
 *         Every locally generated DNS packet is REDIRECTed to port 55.
 *         Only dnscrypt-proxy itself (uid 0) may reach its bootstrap
 *         resolvers directly.
 *
 *   IPv6: filter/OUTPUT -> oem_out -> ariel_dns6
 *         Plain IPv6 DNS is REJECTed (except dnscrypt-proxy's bootstrap
 *         resolver), so resolvers fall back to IPv4, where the redirect
 *         above applies.
 *
 *   IPv6: filter/FORWARD -> oem_fwd -> ariel_dns6_fwd
 *         Hotspot clients sending IPv6 DNS directly to an external server
 *         are REJECTed.
 *
 *   Hotspot clients using the default (DHCP-provided) DNS are covered by the
 *   IPv4 rule as well: their queries are answered by the tethering dnsmasq,
 *   which forwards them upstream from the device and therefore passes
 *   through nat/OUTPUT.
 *
 * Design rules
 * ------------
 *   - Only chains owned by this tool are created, flushed or deleted
 *     (ariel_dns, ariel_dns6, ariel_dns6_fwd). Tables are never flushed as a
 *     whole: netd owns the rest of the ruleset (tethering, firewall,
 *     bandwidth control) and relies on it being intact.
 *   - IPv6 rules are attached through netd's OEM hook chains (oem_out,
 *     oem_fwd), which netd creates for exactly this purpose. nat/OUTPUT has
 *     no OEM hook and is not used by netd, so ariel_dns is attached there
 *     directly.
 *   - Every iptables call uses -w so it waits for the xtables lock instead of
 *     failing while netd is updating the ruleset.
 *   - Applying is idempotent: chains are flushed and refilled, and jumps are
 *     added only if they are not already present.
 *
 * Usage
 * -----
 *   iptables_setup           apply the rules
 *   iptables_setup --flush   remove the rules
 *   iptables_setup --sync    apply or remove according to ariel.online
 *                            (used by init, see the example at the end)
 *
 * All iptables invocations go through the ariel_iptables wrapper, which takes
 * the tool name ("iptables" / "ip6tables") as its first argument.
 */

#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/file.h>
#include <sys/system_properties.h>
#include <sys/wait.h>
#include <unistd.h>

#include <android/log.h>

#define LOG_TAG "iptables_setup"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/* ------------------------------------------------------------------------- */
/* Configuration                                                             */
/* ------------------------------------------------------------------------- */

#define WRAPPER       "/system_ext/bin/ariel_iptables"
#define V4            "iptables"
#define V6            "ip6tables"

/* Chains owned by this tool. */
#define CHAIN_V4      "ariel_dns"       /* nat table,    IPv4 local traffic   */
#define CHAIN_V6      "ariel_dns6"      /* filter table, IPv6 local traffic   */
#define CHAIN_V6_FWD  "ariel_dns6_fwd"  /* filter table, IPv6 hotspot clients */

/* Local port dnscrypt-proxy listens on. */
#define DNSCRYPT_PORT "55"

/*
 * uid of the dnscrypt-proxy process. Only this uid may send DNS directly to
 * the bootstrap resolvers; dnscrypt-proxy needs that to resolve its upstream
 * servers before it can serve queries. Any other process querying these
 * resolvers is handled like all other DNS traffic (redirected on IPv4,
 * rejected on IPv6).
 *
 * dnscrypt-proxy runs as root (init.dnscrypt.rc: "user root", and no
 * user_name is set in dnscrypt-proxy.toml). Keep this in sync if that
 * changes, otherwise dnscrypt-proxy cannot bootstrap.
 */
#define DNSCRYPT_UID  "0"

/* Property that tells whether DNS enforcement should be active ("1"). */
#define ONLINE_PROP   "ariel.online"

/* Set to 0 to leave IPv6 DNS of hotspot clients unfiltered. */
#define ARIEL_FILTER_TETHER_V6 1

/* ------------------------------------------------------------------------- */
/* Command execution                                                         */
/* ------------------------------------------------------------------------- */

/*
 * Forks and execs argv (argv[0] is the full binary path) and waits for it.
 * Returns the child's exit code, or -1 if it could not be run or did not
 * exit normally.
 */
static int run_cmd(const char *const argv[]) {
    pid_t pid = fork();
    if (pid < 0) {
        LOGE("fork failed");
        return -1;
    }
    if (pid == 0) {
        execv(argv[0], (char *const *)argv);
        _exit(127);
    }
    int status;
    if (waitpid(pid, &status, 0) < 0) {
        LOGE("waitpid failed");
        return -1;
    }
    return WIFEXITED(status) ? WEXITSTATUS(status) : -1;
}

/*
 * IPT(tool, args...) runs:  ariel_iptables <tool> -w <args...>
 * and evaluates to the exit code. Use it where a non-zero result is expected
 * and handled by the caller (existence checks, best-effort cleanup).
 */
#define IPT(tool, ...) \
    run_cmd((const char *const[]){ WRAPPER, tool, "-w", __VA_ARGS__, NULL })

/* Same as IPT, but logs an error if the command fails. */
#define IPT_CHECKED(tool, ...)                                            \
    do {                                                                  \
        int _rc = IPT(tool, __VA_ARGS__);                                 \
        if (_rc != 0) LOGE("%s %s failed (rc=%d)", tool,                  \
                           #__VA_ARGS__, _rc);                            \
    } while (0)

/* ------------------------------------------------------------------------- */
/* Chain helpers                                                             */
/* ------------------------------------------------------------------------- */

/*
 * Makes sure `chain` exists in `table` and is empty.
 * -N fails with rc=1 if the chain already exists, which is fine.
 */
static void ensure_empty_chain(const char *tool, const char *table, const char *chain) {
    IPT(tool, "-t", table, "-N", chain);
    IPT_CHECKED(tool, "-t", table, "-F", chain);
}

/*
 * Makes sure `parent` contains exactly one "-j target" rule.
 * The rule is added only if -C reports it missing. With first=1 it is
 * inserted at the top of `parent`, otherwise appended.
 */
static void ensure_jump(const char *tool, const char *table,
                        const char *parent, const char *target, int first) {
    if (IPT(tool, "-t", table, "-C", parent, "-j", target) == 0)
        return;
    int rc = first
        ? IPT(tool, "-t", table, "-I", parent, "1", "-j", target)
        : IPT(tool, "-t", table, "-A", parent, "-j", target);
    if (rc != 0)
        LOGE("%s: jump %s -> %s failed (rc=%d)", tool, parent, target, rc);
}

/*
 * Removes every "-j target" rule from `parent`. -D deletes one match per
 * call, so it is repeated until it fails (no more matches, or the chain does
 * not exist). The loop is bounded as a safety net.
 */
static void remove_jumps(const char *tool, const char *table,
                         const char *parent, const char *target) {
    for (int i = 0; i < 16; i++) {
        if (IPT(tool, "-t", table, "-D", parent, "-j", target) != 0)
            break;
    }
}

/* Removes all jumps to `chain` from `parent`, then flushes and deletes it. */
static void remove_chain(const char *tool, const char *table,
                         const char *parent, const char *chain) {
    remove_jumps(tool, table, parent, chain);
    IPT(tool, "-t", table, "-F", chain);
    IPT(tool, "-t", table, "-X", chain);
}

/* ------------------------------------------------------------------------- */
/* IPv4                                                                      */
/* ------------------------------------------------------------------------- */

/*
 * nat/OUTPUT -> ariel_dns
 *
 *   1. Bootstrap resolvers (8.8.8.8, 9.9.9.11), dnscrypt-proxy only:
 *      RETURN, i.e. leave the packet untouched so it reaches the real server.
 *   2. Everything else to port 53 (UDP and TCP): REDIRECT to dnscrypt-proxy.
 *
 * The nat table only evaluates the first packet of a connection; the rest of
 * the flow follows the same translation via conntrack.
 *
 * The jump is inserted at the top of nat/OUTPUT so no other rule can
 * translate DNS traffic before it.
 */
static void apply_v4(void) {
    ensure_empty_chain(V4, "nat", CHAIN_V4);

    IPT_CHECKED(V4, "-t", "nat", "-A", CHAIN_V4, "-m", "owner", "--uid-owner", DNSCRYPT_UID,
                "-p", "udp", "-d", "8.8.8.8",  "--dport", "53", "-j", "RETURN");
    IPT_CHECKED(V4, "-t", "nat", "-A", CHAIN_V4, "-m", "owner", "--uid-owner", DNSCRYPT_UID,
                "-p", "tcp", "-d", "8.8.8.8",  "--dport", "53", "-j", "RETURN");
    IPT_CHECKED(V4, "-t", "nat", "-A", CHAIN_V4, "-m", "owner", "--uid-owner", DNSCRYPT_UID,
                "-p", "udp", "-d", "9.9.9.11", "--dport", "53", "-j", "RETURN");
    IPT_CHECKED(V4, "-t", "nat", "-A", CHAIN_V4, "-m", "owner", "--uid-owner", DNSCRYPT_UID,
                "-p", "tcp", "-d", "9.9.9.11", "--dport", "53", "-j", "RETURN");

    IPT_CHECKED(V4, "-t", "nat", "-A", CHAIN_V4,
                "-p", "udp", "--dport", "53", "-j", "REDIRECT", "--to-ports", DNSCRYPT_PORT);
    IPT_CHECKED(V4, "-t", "nat", "-A", CHAIN_V4,
                "-p", "tcp", "--dport", "53", "-j", "REDIRECT", "--to-ports", DNSCRYPT_PORT);

    ensure_jump(V4, "nat", "OUTPUT", CHAIN_V4, 1);
}

/* ------------------------------------------------------------------------- */
/* IPv6                                                                      */
/* ------------------------------------------------------------------------- */

/*
 * filter/OUTPUT -> oem_out -> ariel_dns6   (traffic generated on the device)
 *
 *   1. Bootstrap resolver (2001:4860:4860::8888), dnscrypt-proxy only:
 *      RETURN, so the packet continues through netd's remaining OUTPUT
 *      chains (firewall, bandwidth) instead of being accepted outright.
 *   2. Everything else to port 53: REJECT (TCP with a reset) so clients fail
 *      fast and retry over IPv4.
 *
 * filter/FORWARD -> oem_fwd -> ariel_dns6_fwd   (hotspot clients)
 *
 *   All port 53 traffic is REJECTed; there is no bootstrap exception.
 *   This needs its own chain: the owner match used in ariel_dns6 is only
 *   valid for locally generated packets, and the kernel refuses owner rules
 *   in any chain that is reachable from FORWARD.
 *
 * oem_out is the first chain netd hooks into filter/OUTPUT, and oem_fwd the
 * first in filter/FORWARD, so these rules are evaluated before netd's own.
 */
static void apply_v6(void) {
    ensure_empty_chain(V6, "filter", CHAIN_V6);

    IPT_CHECKED(V6, "-A", CHAIN_V6, "-m", "owner", "--uid-owner", DNSCRYPT_UID,
                "-d", "2001:4860:4860::8888", "-p", "udp", "--dport", "53", "-j", "RETURN");
    IPT_CHECKED(V6, "-A", CHAIN_V6, "-m", "owner", "--uid-owner", DNSCRYPT_UID,
                "-d", "2001:4860:4860::8888", "-p", "tcp", "--dport", "53", "-j", "RETURN");
    IPT_CHECKED(V6, "-A", CHAIN_V6, "-p", "udp", "--dport", "53", "-j", "REJECT");
    IPT_CHECKED(V6, "-A", CHAIN_V6, "-p", "tcp", "--dport", "53",
                "-j", "REJECT", "--reject-with", "tcp-reset");

    ensure_jump(V6, "filter", "oem_out", CHAIN_V6, 0);

#if ARIEL_FILTER_TETHER_V6
    ensure_empty_chain(V6, "filter", CHAIN_V6_FWD);

    IPT_CHECKED(V6, "-A", CHAIN_V6_FWD, "-p", "udp", "--dport", "53", "-j", "REJECT");
    IPT_CHECKED(V6, "-A", CHAIN_V6_FWD, "-p", "tcp", "--dport", "53",
                "-j", "REJECT", "--reject-with", "tcp-reset");

    ensure_jump(V6, "filter", "oem_fwd", CHAIN_V6_FWD, 0);
#endif
}

/* ------------------------------------------------------------------------- */
/* Removal                                                                   */
/* ------------------------------------------------------------------------- */

/*
 * Detaches and deletes every chain owned by this tool. Nothing else in the
 * ruleset is touched. Safe to call when the rules are not installed.
 */
static void remove_all(void) {
    remove_chain(V4, "nat",    "OUTPUT",  CHAIN_V4);
    remove_chain(V6, "filter", "oem_out", CHAIN_V6);
    remove_chain(V6, "filter", "oem_fwd", CHAIN_V6_FWD);
}

/* ------------------------------------------------------------------------- */
/* Property sync                                                             */
/* ------------------------------------------------------------------------- */

static int enforcement_wanted(void) {
    char val[PROP_VALUE_MAX] = "";
    __system_property_get(ONLINE_PROP, val);
    return strcmp(val, "1") == 0;
}

/*
 * --sync: brings the ruleset in line with ariel.online.
 *
 * Locking: init may start this service again while a previous run is still
 * active. An exclusive flock on our own executable serializes such runs, so
 * their iptables commands never interleave. If the lock cannot be taken, the
 * run continues unlocked and logs an error.
 *
 * Re-check loop: init ignores "start" for a oneshot service that is still
 * running, so a property change during a run would otherwise be missed.
 * After each pass the property is read again, and the work is repeated until
 * the applied state matches it (bounded as a safety net).
 */
static void sync_with_property(void) {
    int fd = open("/proc/self/exe", O_RDONLY | O_CLOEXEC);
    if (fd >= 0 && flock(fd, LOCK_EX) != 0)
        LOGE("flock failed, continuing without lock");

    int applied = -1;
    for (int i = 0; i < 8; i++) {
        int want = enforcement_wanted();
        if (want == applied)
            break;
        if (want) {
            LOGI("sync: " ONLINE_PROP "=1, applying DNS rules");
            apply_v4();
            apply_v6();
        } else {
            LOGI("sync: " ONLINE_PROP "!=1, removing DNS rules");
            remove_all();
        }
        applied = want;
    }

    if (fd >= 0)
        close(fd);  /* releases the lock */
}

/* ------------------------------------------------------------------------- */
/* Entry point                                                               */
/* ------------------------------------------------------------------------- */

int main(int argc, char *argv[]) {
    const char *mode = argc > 1 ? argv[1] : "";

    if (strcmp(mode, "--sync") == 0) {
        sync_with_property();
    } else if (strcmp(mode, "--flush") == 0) {
        LOGI("removing DNS rules");
        remove_all();
    } else {
        LOGI("applying DNS rules");
        apply_v4();
        apply_v6();
    }
    return 0;
}

/*
 * Init integration (init.dnscrypt.rc)
 * -----------------------------------
 *
 * service iptables-sync /system_ext/bin/iptables_setup --sync
 *     class late_start
 *     user root
 *     group root
 *     capabilities NET_ADMIN NET_RAW
 *     seclabel u:r:netd:s0
 *     oneshot
 *     disabled
 *
 * # Re-sync whenever the enforcement state changes.
 * on property:ariel.online=*
 *     start iptables-sync
 *
 * # A netd (re)start recreates oem_out / oem_fwd empty, so the jumps
 * # into our chains must be restored.
 * on property:init.svc.netd=running
 *     start dnscrypt_proxy
 *     start iptables-sync
 *
 * SELinux: the service runs as netd and takes a flock on this binary, which
 * requires the "lock" permission on the binary's file type.
 */
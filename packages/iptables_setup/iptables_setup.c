/*
 * iptables_setup.c - ArielOS DNS redirect rules for dnscrypt-proxy
 *
 * Pravila:
 *   - dira ISKLJUCIVO sopstvene lance (ariel_dns, ariel_dns6)
 *   - nikad ne radi -F / -X nad celom tabelom (to brise netd lance: tetherctrl_*, fw_*, bw_*, oem_*)
 *   - IPv6 hook ide preko netd-ovog oem_out (i opciono oem_fwd), ne direktno u OUTPUT
 *   - svi pozivi koriste -w (xtables lock) jer netd paralelno menja tabele
 *   - idempotentno: moze da se pokrene vise puta bez dupliranja jump-ova
 *
 * Upotreba:
 *   iptables_setup           -> primeni pravila
 *   iptables_setup --flush   -> ukloni SAMO ariel pravila
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/file.h>
#include <sys/wait.h>
#include <sys/system_properties.h>

#include <android/log.h>

#define LOG_TAG "iptables_setup"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define WRAPPER    "/system_ext/bin/ariel_iptables"
#define V4         "iptables"
#define V6         "ip6tables"

#define CHAIN_V4   "ariel_dns"      /* nat tabela  */
#define CHAIN_V6   "ariel_dns6"     /* filter tabela */
#define DNSCRYPT_PORT "55"

/* 1 = i hotspot klijenti prolaze kroz IPv6 DNS filter (netd oem_fwd hook) */
#define ARIEL_FILTER_TETHER_V6 1

/* Fork + exec; vraca exit code ili -1 */
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
 * IPT(tool, "-t", "nat", ...) -> WRAPPER tool -w <args>
 * -w ceka xtables lock umesto da odmah padne (netd drzi lock tokom boot-a).
 */
#define IPT(tool, ...) \
    run_cmd((const char *const[]){ WRAPPER, tool, "-w", __VA_ARGS__, NULL })

/* Loguje gresku samo kad je neocekivana */
#define IPT_CHECKED(tool, ...)                                            \
    do {                                                                  \
        int _rc = IPT(tool, __VA_ARGS__);                                 \
        if (_rc != 0) LOGE("%s %s failed (rc=%d)", tool,                  \
                           #__VA_ARGS__, _rc);                            \
    } while (0)

/* Napravi lanac ako ne postoji, pa ga isprazni (samo NAS lanac). */
static void ensure_empty_chain(const char *tool, const char *table, const char *chain) {
    IPT(tool, "-t", table, "-N", chain);            /* rc=1 ako vec postoji: OK */
    IPT_CHECKED(tool, "-t", table, "-F", chain);
}

/* Dodaj "-j target" u parent tacno jednom. first=1 -> na pocetak lanca. */
static void ensure_jump(const char *tool, const char *table,
                        const char *parent, const char *target, int first) {
    if (IPT(tool, "-t", table, "-C", parent, "-j", target) == 0)
        return;                                     /* vec postoji */
    int rc = first
        ? IPT(tool, "-t", table, "-I", parent, "1", "-j", target)
        : IPT(tool, "-t", table, "-A", parent, "-j", target);
    if (rc != 0)
        LOGE("%s: jump %s -> %s failed (rc=%d)", tool, parent, target, rc);
}

/* Ukloni SVE kopije "-j target" iz parent lanca (ciscenje duplikata/starih verzija). */
static void remove_jumps(const char *tool, const char *table,
                         const char *parent, const char *target) {
    for (int i = 0; i < 16; i++) {
        if (IPT(tool, "-t", table, "-D", parent, "-j", target) != 0)
            break;
    }
}

static void apply_v4(void) {
    ensure_empty_chain(V4, "nat", CHAIN_V4);

    /* whitelist bootstrap resolvera (dnscrypt-proxy ih koristi) */
    IPT_CHECKED(V4, "-t", "nat", "-A", CHAIN_V4, "-p", "udp", "-d", "8.8.8.8",  "--dport", "53", "-j", "RETURN");
    IPT_CHECKED(V4, "-t", "nat", "-A", CHAIN_V4, "-p", "tcp", "-d", "8.8.8.8",  "--dport", "53", "-j", "RETURN");
    IPT_CHECKED(V4, "-t", "nat", "-A", CHAIN_V4, "-p", "udp", "-d", "9.9.9.11", "--dport", "53", "-j", "RETURN");
    IPT_CHECKED(V4, "-t", "nat", "-A", CHAIN_V4, "-p", "tcp", "-d", "9.9.9.11", "--dport", "53", "-j", "RETURN");

    /* sav ostali DNS -> dnscrypt-proxy */
    IPT_CHECKED(V4, "-t", "nat", "-A", CHAIN_V4, "-p", "udp", "--dport", "53", "-j", "REDIRECT", "--to-ports", DNSCRYPT_PORT);
    IPT_CHECKED(V4, "-t", "nat", "-A", CHAIN_V4, "-p", "tcp", "--dport", "53", "-j", "REDIRECT", "--to-ports", DNSCRYPT_PORT);

    /*
     * nat OUTPUT netd ne koristi (nema oem hook-a), pa je direktan jump OK.
     * Hotspot DNS ide preko dnsmasq-a na uredjaju (legacy DNS proxy) -> OUTPUT,
     * tako da su i klijenti pokriveni.
     */
    ensure_jump(V4, "nat", "OUTPUT", CHAIN_V4, 1);
}

static void apply_v6(void) {
    /* migracija: stare verzije su skakale direktno iz filter OUTPUT */
    remove_jumps(V6, "filter", "OUTPUT", CHAIN_V6);

    ensure_empty_chain(V6, "filter", CHAIN_V6);

    IPT_CHECKED(V6, "-A", CHAIN_V6, "-d", "2001:4860:4860::8888", "-p", "udp", "--dport", "53", "-j", "RETURN");
    IPT_CHECKED(V6, "-A", CHAIN_V6, "-d", "2001:4860:4860::8888", "-p", "tcp", "--dport", "53", "-j", "RETURN");
    IPT_CHECKED(V6, "-A", CHAIN_V6, "-p", "udp", "--dport", "53", "-j", "REJECT");
    IPT_CHECKED(V6, "-A", CHAIN_V6, "-p", "tcp", "--dport", "53", "-j", "REJECT", "--reject-with", "tcp-reset");

    /*
     * oem_out je netd-ov OEM hook, prvi u filter OUTPUT (pre fw_/st_/bw_ lanaca).
     * RETURN (umesto ACCEPT) za whitelist -> netd-ova pravila (firewall, data saver)
     * i dalje vaze za taj saobracaj.
     */
    ensure_jump(V6, "filter", "oem_out", CHAIN_V6, 0);

#if ARIEL_FILTER_TETHER_V6
    /* hotspot klijenti sa IPv6 koji pitaju DNS direktno (mimo dnsmasq-a) */
    ensure_jump(V6, "filter", "oem_fwd", CHAIN_V6, 0);
#endif
}

static void flush_ariel(void) {
    /* v4 */
    remove_jumps(V4, "nat", "OUTPUT", CHAIN_V4);
    IPT(V4, "-t", "nat", "-F", CHAIN_V4);
    IPT(V4, "-t", "nat", "-X", CHAIN_V4);

    /* v6 */
    remove_jumps(V6, "filter", "OUTPUT",  CHAIN_V6);   /* stare verzije */
    remove_jumps(V6, "filter", "oem_out", CHAIN_V6);
    remove_jumps(V6, "filter", "oem_fwd", CHAIN_V6);
    IPT(V6, "-F", CHAIN_V6);
    IPT(V6, "-X", CHAIN_V6);
}

static int prop_online(void) {
    char val[PROP_VALUE_MAX] = "";
    __system_property_get("ariel.online", val);
    return strcmp(val, "1") == 0;
}

/*
 * --sync: prati ariel.online dok se ne stabilizuje.
 * flock serijalizuje paralelne instance, a petlja hvata promene svojstva
 * koje stignu dok radimo (init "start" ne pokrece servis koji vec radi).
 */
static void sync_with_property(void) {
    int fd = open("/proc/self/exe", O_RDONLY | O_CLOEXEC);
    if (fd >= 0 && flock(fd, LOCK_EX) != 0)
        LOGE("flock failed, continuing without lock");

    int applied = -1;
    for (int i = 0; i < 8; i++) {
        int want = prop_online();
        if (want == applied)
            break;
        if (want) {
            LOGI("sync: ariel.online=1 -> applying ariel DNS rules");
            apply_v4();
            apply_v6();
        } else {
            LOGI("sync: ariel.online!=1 -> removing ariel DNS rules");
            flush_ariel();
        }
        applied = want;
    }

    if (fd >= 0) close(fd);   /* oslobadja lock */
}

int main(int argc, char *argv[]) {
    const char *mode = argc > 1 ? argv[1] : "";

    if (strcmp(mode, "--sync") == 0) {
        sync_with_property();
    } else if (strcmp(mode, "--flush") == 0) {
        LOGI("removing ariel DNS rules");
        flush_ariel();
    } else {
        LOGI("applying ariel DNS rules");
        apply_v4();
        apply_v6();
    }
    return 0;
}

/*
 * init.dnscrypt.rc (zamena za iptables-setup + iptables-cleanup):
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
 * on property:ariel.online=*
 *     start iptables-sync
 *
 * # netd pri restartu ponovo pravi oem_out/oem_fwd (prazne) -> vrati jump-ove
 * on property:init.svc.netd=running
 *     start dnscrypt_proxy
 *     start iptables-sync
 */
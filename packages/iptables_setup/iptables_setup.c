#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/wait.h>
#include <string.h>

#include <android/log.h>

#define LOG_TAG "iptables_setup"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Forks and execs a command; returns its exit code or -1 on error
static int run_cmd(char *const argv[]) {
    pid_t pid = fork();
    if (pid < 0) {
        perror("fork");
        return -1;
    } else if (pid == 0) {
        execv(argv[0], argv);
        perror("execv");
        _exit(EXIT_FAILURE);
    } else {
        int status;
        if (waitpid(pid, &status, 0) < 0) {
            perror("waitpid");
            return -1;
        }
        return (WIFEXITED(status) ? WEXITSTATUS(status) : -1);
    }
}

int main(int argc, char *argv[]) {
    int rc;
    // Determine mode: apply rules (default) or flush all rules (--flush)
    int flush = (argc > 1 && strcmp(argv[1], "--flush") == 0);

    if (flush) {
        // Flush NAT table (all chains)
        char *const flush1[] = {"/system_ext/bin/ariel_iptables", "iptables", "-t", "nat", "-F", NULL};
        // Delete any user-defined NAT chains
        char *const flush2[] = {"/system_ext/bin/ariel_iptables", "iptables", "-t", "nat", "-X", NULL};
        // Flush IPv6 filter table
        char *const flush3[] = {"/system_ext/bin/ariel_iptables", "ip6tables", "-F", NULL};
        // Delete any user-defined IPv6 chains
        char *const flush4[] = {"/system_ext/bin/ariel_iptables", "ip6tables", "-X", NULL};

        char *const *flush_cmds[] = {flush1, flush2, flush3, flush4};
        size_t flush_count = sizeof(flush_cmds) / sizeof(flush_cmds[0]);

        for (size_t i = 0; i < flush_count; i++) {
            rc = run_cmd((char *const *)flush_cmds[i]);
            if (rc != 0) {
                fprintf(stderr, "Flush command %zu failed with exit code %d\n", i + 1, rc);
            }
        }
    } else {
        // Apply rules
        char *const cmd1[] = {"/system_ext/bin/ariel_iptables", "iptables",
            "-t", "nat", "-I", "OUTPUT",
            "-p", "udp", "--dport", "53",
            "-j", "REDIRECT", "--to-ports", "55", NULL};
        char *const cmd2[] = {"/system_ext/bin/ariel_iptables", "iptables",
            "-t", "nat", "-I", "OUTPUT",
            "-p", "tcp", "--dport", "53",
            "-j", "REDIRECT", "--to-ports", "55", NULL};
        char *const cmd3[] = {"/system_ext/bin/ariel_iptables", "iptables",
            "-t", "nat", "-I", "OUTPUT",
            "-p", "udp", "--dport", "53",
            "-j", "DNAT", "--to-destination", "127.0.0.1:55", NULL};
        char *const cmd4[] = {"/system_ext/bin/ariel_iptables", "iptables",
            "-t", "nat", "-I", "OUTPUT",
            "-p", "tcp", "--dport", "53",
            "-j", "DNAT", "--to-destination", "127.0.0.1:55", NULL};
        char *const cmd5[] = {"/system_ext/bin/ariel_iptables", "iptables",
            "-t", "nat", "-I", "PREROUTING",
            "-p", "udp", "--dport", "53",
            "-j", "DNAT", "--to", "127.0.0.1:55", NULL};
        char *const cmd6[] = {"/system_ext/bin/ariel_iptables", "iptables",
            "-t", "nat", "-I", "PREROUTING",
            "-p", "tcp", "--dport", "53",
            "-j", "DNAT", "--to", "127.0.0.1:55", NULL};
        char *const cmd7[] = {"/system_ext/bin/ariel_iptables", "ip6tables",
            "-A", "OUTPUT",
            "-p", "tcp", "--dport", "53",
            "-j", "REJECT", NULL};
        char *const cmd8[] = {"/system_ext/bin/ariel_iptables", "ip6tables",
            "-A", "OUTPUT",
            "-p", "udp", "--dport", "53",
            "-j", "REJECT", NULL};
        char *const cmd9[] = {"/system_ext/bin/ariel_iptables", "iptables",
            "-t", "nat", "-I", "OUTPUT",
            "-p", "udp", "--dport", "53", "-d", "9.9.9.11",
            "-j", "ACCEPT", NULL};
        char *const cmd10[] = {"/system_ext/bin/ariel_iptables", "iptables",
            "-t", "nat", "-I", "OUTPUT",
            "-p", "tcp", "--dport", "53", "-d", "9.9.9.11",
            "-j", "ACCEPT", NULL};
        char *const cmd11[] = {"/system_ext/bin/ariel_iptables", "iptables",
            "-t", "nat", "-I", "OUTPUT",
            "-p", "udp", "--dport", "53", "-d", "8.8.8.8",
            "-j", "ACCEPT", NULL};
        char *const cmd12[] = {"/system_ext/bin/ariel_iptables", "iptables",
            "-t", "nat", "-I", "OUTPUT",
            "-p", "tcp", "--dport", "53", "-d", "8.8.8.8",
            "-j", "ACCEPT", NULL};

        // char *const * const cmds[] = {cmd1, cmd2, cmd7, cmd8, cmd1, cmd11, cmd12};
        // size_t count = sizeof(cmds) / sizeof(cmds[0]);

        // for (size_t i = 0; i < count; i++) {
        //     rc = run_cmd((char *const *)cmds[i]);
        //     if (rc != 0) {
        //         LOGE("Command %zu failed with exit code %d\n", i + 1, rc);
        //     } else {
        //         LOGI("Command %zu executed with exit code %d\n", i + 1, rc);
        //     }
        // }

        /* --- chain management --------------------------------------------------- */

        /* Create chain (ignore "exists" error = rc 1) */
        char *const new_chain[] = {
            "/system_ext/bin/ariel_iptables","iptables",
            "-t","nat","-N","ariel_dns", NULL};

        /* Flush chain in case it already had old rules */
        char *const flush_chain[] = {
            "/system_ext/bin/ariel_iptables","iptables",
            "-t","nat","-F","ariel_dns", NULL};

        /* Jump from OUTPUT → ariel_dns  (insert only if not present) */
        char *const test_jump[] = {
            "/system_ext/bin/ariel_iptables","iptables",
            "-t","nat","-C","OUTPUT","-j","ariel_dns", NULL};
        char *const add_jump[]  = {
            "/system_ext/bin/ariel_iptables","iptables",
            "-t","nat","-A","OUTPUT","-j","ariel_dns", NULL};

        /* --- whitelist (bootstrap resolvers) ------------------------------------ */

        char *const acc_udp_8888[] = {
            "/system_ext/bin/ariel_iptables","iptables",
            "-t","nat","-A","ariel_dns",
            "-p","udp","-d","8.8.8.8","--dport","53",
            "-j","RETURN", NULL};

        char *const acc_tcp_8888[] = {
            "/system_ext/bin/ariel_iptables","iptables",
            "-t","nat","-A","ariel_dns",
            "-p","tcp","-d","8.8.8.8","--dport","53",
            "-j","RETURN", NULL};

        char *const acc_udp_99911[] = {
            "/system_ext/bin/ariel_iptables","iptables",
            "-t","nat","-A","ariel_dns",
            "-p","udp","-d","9.9.9.11","--dport","53",
            "-j","RETURN", NULL};

        char *const acc_tcp_99911[] = {
            "/system_ext/bin/ariel_iptables","iptables",
            "-t","nat","-A","ariel_dns",
            "-p","tcp","-d","9.9.9.11","--dport","53",
            "-j","RETURN", NULL};

        /* --- main redirect ------------------------------------------------------- */

        char *const redir_udp[] = {
            "/system_ext/bin/ariel_iptables","iptables",
            "-t","nat","-A","ariel_dns",
            "-p","udp","--dport","53",
            "-j","REDIRECT","--to-ports","55", NULL};

        char *const redir_tcp[] = {
            "/system_ext/bin/ariel_iptables","iptables",
            "-t","nat","-A","ariel_dns",
            "-p","tcp","--dport","53",
            "-j","REDIRECT","--to-ports","55", NULL};

        /* --- IPv6 RULES --------------------------------------------------------- */
        /* --- IPv6 reject (unchanged) -------------------------------------------- */

        /* 1 ── create chain (ignore rc==1 if it already exists) */
        char *const new_chain6[] = {
            "/system_ext/bin/ariel_iptables", "ip6tables",
            "-N", "ariel_dns6",
            NULL };

        /* 2 ── flush chain (safe even if just created) */
        char *const flush_chain6[] = {
            "/system_ext/bin/ariel_iptables", "ip6tables",
            "-F", "ariel_dns6",
            NULL };

        /* 3 ── test jump OUTPUT→ariel_dns6 (rc==0 means already present) */
        char *const test_jump6[] = {
            "/system_ext/bin/ariel_iptables", "ip6tables",
            "-C", "OUTPUT", "-j", "ariel_dns6",
            NULL };

        /* 4 ── add jump at position 1 if test_jump6 fails (rc==1) */
        char *const add_jump6[] = {
            "/system_ext/bin/ariel_iptables", "ip6tables",
            "-I", "OUTPUT", "1", "-j", "ariel_dns6",
            NULL };

        /* 5 ── ALLOW Google DNS v6 :53  (UDP) */
        char *const acc6_udp_8888[] = {
            "/system_ext/bin/ariel_iptables", "ip6tables",
            "-A", "ariel_dns6",
            "-d", "2001:4860:4860::8888",
            "-p", "udp", "--dport", "53",
            "-j", "ACCEPT",
            NULL };

        /* 6 ── ALLOW Google DNS v6 :53  (TCP) */
        char *const acc6_tcp_8888[] = {
            "/system_ext/bin/ariel_iptables", "ip6tables",
            "-A", "ariel_dns6",
            "-d", "2001:4860:4860::8888",
            "-p", "tcp", "--dport", "53",
            "-j", "ACCEPT",
            NULL };

        /* (repeat for 2001:4860:4860::8844 if you want the second address) */

        char *const flush_rej6_tcp[] = {
            "/system_ext/bin/ariel_iptables","ip6tables",
            "-D","ariel_dns6","-p","tcp","--dport","53","-j","REJECT", NULL};

        char *const flush_rej6_udp[] = {
            "/system_ext/bin/ariel_iptables","ip6tables",
            "-D","ariel_dns6","-p","udp","--dport","53","-j","REJECT", NULL};

        /* 7 ── REJECT all other UDP :53 */
        char *const rej6_udp[] = {
            "/system_ext/bin/ariel_iptables", "ip6tables",
            "-A", "ariel_dns6",
            "-p", "udp", "--dport", "53",
            "-j", "REJECT",
            NULL };

        /* 8 ── REJECT all other TCP :53 */
        char *const rej6_tcp[] = {
            "/system_ext/bin/ariel_iptables", "ip6tables",
            "-A", "ariel_dns6",
            "-p", "tcp", "--dport", "53",
            "-j", "REJECT",
            NULL };

        /* --- execution list ------------------------------------------------------ */

        char *const * const cmds[] = {
            /* chain setup */
            new_chain,
            flush_chain,
            test_jump,
            add_jump,      /* add_jump runs only if test_jump fails */

            /* whitelist first  */
            acc_udp_8888, acc_tcp_8888,
            acc_udp_99911, acc_tcp_99911,

            /* then catch-all redirect */
            redir_udp, redir_tcp,

            /* IPv6 filter table rules */
            /* chain setup */
            new_chain6,
            flush_chain6,
            test_jump6,
            add_jump6,      /* add_jump runs only if test_jump fails */
            acc6_udp_8888, acc6_tcp_8888,
            flush_rej6_tcp, flush_rej6_udp,
            rej6_tcp, rej6_udp
        };

        size_t count = sizeof(cmds) / sizeof(cmds[0]);
        for (size_t i = 0; i < count; ++i) {
            int rc = run_cmd((char *const *)cmds[i]);
            /* ignore rc == 1 for 'chain exists' and rc == 0 for successful check */
            if (rc != 0 && !(i == 0 && rc == 1) && !(cmds[i] == test_jump && rc == 0)) {
                LOGE("cmd %zu failed (rc=%d)\n", i + 1, rc);
            }
        }

    }
    return 0;
}

/*
Example init script to launch:

on late_start
    # Apply rules:
    exec u:r:dnscrypt_proxy:s0 root root -- /system/bin/firewall_setup

    # To flush rules instead:
    # exec u:r:dnscrypt_proxy:s0 root root -- /system/bin/firewall_setup --flush
*/

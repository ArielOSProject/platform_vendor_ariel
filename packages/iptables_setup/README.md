# iptables binary

This iptables binary is exactly the same as the one that is installed under system/bin. We use it to install it to system_ext/bin and to be able to
label it properly within SELinux context so that we can set iptable rules for dnscrypt. The original iptables binary under system/bin is labeled
as system_file and we can't execute it without hitting neverallow rules. So, this was our way of bending the rules, just a bit :)

# iptables_setup

Simple binary that sets several iptables and ip6tables rules to force the DNS traffic to go througn dnscrypt-proxy.

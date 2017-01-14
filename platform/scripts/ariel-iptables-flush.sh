#!/system/bin/sh

IPTABLES=iptables
GREP=grep

$IPTABLES --version || exit 1
# Create the droidwall chains if necessary
$IPTABLES -L droidwall >/dev/null 2>/dev/null || $IPTABLES --new droidwall || exit 2
$IPTABLES -L droidwall-3g >/dev/null 2>/dev/null || $IPTABLES --new droidwall-3g || exit 3
$IPTABLES -L droidwall-wifi >/dev/null 2>/dev/null || $IPTABLES --new droidwall-wifi || exit 4
$IPTABLES -L droidwall-reject >/dev/null 2>/dev/null || $IPTABLES --new droidwall-reject || exit 5
# Add droidwall chain to OUTPUT chain if necessary
$IPTABLES -L OUTPUT | $GREP -q droidwall || $IPTABLES -A OUTPUT -j droidwall || exit 6
# Flush existing rules
$IPTABLES -F droidwall || exit 7
$IPTABLES -F droidwall-3g || exit 8
$IPTABLES -F droidwall-wifi || exit 9
$IPTABLES -F droidwall-reject || exit 10

#!/usr/bin/env python3
"""
Convert a StevenBlack-style hosts file into a dnscrypt-proxy
blocked-names.txt file.

Source format (per line):   0.0.0.0 some.domain.com
Output format (per line):   some.domain.com

In dnscrypt-proxy a bare domain blocks that name *and* all of its
subdomains, so no wildcards are needed.

Usage:
    python3 build_blocked_names.py
    python3 build_blocked_names.py --url <hosts_url> --out blocked-names.txt
"""

import argparse
import datetime
import sys
import urllib.request

DEFAULT_URL = (
    "https://raw.githubusercontent.com/StevenBlack/hosts/master/"
    "alternates/fakenews-gambling-porn/hosts"
)
DEFAULT_OUT = "blocked-names.txt"

# Hostnames that appear in hosts files but are not real blockable domains.
SKIP_HOSTS = {
    "localhost",
    "localhost.localdomain",
    "local",
    "broadcasthost",
    "ip6-localhost",
    "ip6-loopback",
    "ip6-localnet",
    "ip6-mcastprefix",
    "ip6-allnodes",
    "ip6-allrouters",
    "ip6-allhosts",
    "0.0.0.0",
}

# Only lines that redirect to one of these "null" IPs are treated as blocks.
NULL_IPS = {"0.0.0.0", "127.0.0.1", "::1"}


def fetch(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": "blocked-names-builder"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read().decode("utf-8", errors="replace")


def extract_source_date(text: str) -> str:
    """Pull the upstream '# Date:' line from the hosts file header, if present."""
    for raw in text.splitlines():
        line = raw.strip()
        if not line.startswith("#"):
            # Header comments are all at the top; stop once past them.
            if line:
                break
            continue
        if line.lower().startswith("# date:"):
            return line[len("# date:"):].strip()
    return "unknown"


def extract_domains(text: str):
    """Yield unique domains, preserving first-seen order."""
    seen = set()
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue

        parts = line.split()
        # Expect: <ip> <hostname> [comment...]
        if len(parts) < 2:
            continue

        ip, host = parts[0], parts[1].lower().rstrip(".")

        if ip not in NULL_IPS:
            continue
        if host in SKIP_HOSTS or "." not in host:
            continue
        if host in seen:
            continue

        seen.add(host)
        yield host


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", default=DEFAULT_URL, help="Source hosts file URL")
    parser.add_argument("--out", default=DEFAULT_OUT, help="Output file path")
    args = parser.parse_args()

    print(f"Fetching {args.url} ...", file=sys.stderr)
    try:
        text = fetch(args.url)
    except Exception as e:
        print(f"Failed to fetch hosts file: {e}", file=sys.stderr)
        return 1

    domains = list(extract_domains(text))
    source_date = extract_source_date(text)
    generated_at = datetime.datetime.now().astimezone().strftime("%Y-%m-%d %H:%M:%S %Z")

    header = [
        "# dnscrypt-proxy blocked names",
        f"# Generated:      {generated_at}",
        f"# Source updated: {source_date}",
        f"# Source URL:     {args.url}",
        f"# Unique domains: {len(domains)}",
        "#",
        "# A bare domain blocks that name and all subdomains.",
        "",
    ]

    with open(args.out, "w", encoding="utf-8") as f:
        f.write("\n".join(header))
        f.write("\n".join(domains))
        f.write("\n")

    print(f"Wrote {len(domains)} domains to {args.out}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

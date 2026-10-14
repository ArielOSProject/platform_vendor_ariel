#!/usr/bin/env python3
"""Generate the Updater index.json next to the OTA zip in a product out dir.

Usage: gen_ota_json.py <product_out_dir>
Env:   ARIEL_OTA_BASE_URL (default https://ota.arielos.com)
"""
import glob
import json
import os
import re
import sys
import zipfile

ROMTYPE = "UNOFFICIAL"
NAME_RE = re.compile(r"^lineage-(?P<version>\d+\.\d+)-\d{8}-[A-Za-z]+-(?P<device>.+)\.zip$")


def parse_metadata(text):
    meta = {}
    for line in text.splitlines():
        if "=" in line:
            k, v = line.split("=", 1)
            meta[k.strip()] = v.strip()
    return meta


def main():
    if len(sys.argv) != 2:
        sys.exit("usage: gen_ota_json.py <product_out_dir>")
    out = os.path.abspath(sys.argv[1])
    if not os.path.isdir(out):
        sys.exit("not a directory: %s" % out)

    zips = [p for p in glob.glob(os.path.join(out, "lineage-*.zip"))
            if NAME_RE.match(os.path.basename(p))]
    if not zips:
        sys.exit("no lineage-*.zip found in %s" % out)
    zip_path = max(zips, key=os.path.getmtime)
    filename = os.path.basename(zip_path)
    m = NAME_RE.match(filename)
    version, device = m.group("version"), m.group("device")

    meta_path = os.path.join(out, "ota_metadata")
    if os.path.isfile(meta_path):
        with open(meta_path) as f:
            meta = parse_metadata(f.read())
    else:
        with zipfile.ZipFile(zip_path) as z:
            meta = parse_metadata(
                z.read("META-INF/com/android/metadata").decode())

    for key in ("post-timestamp", "post-build-incremental"):
        if key not in meta:
            sys.exit("%s missing from OTA metadata" % key)

    base = os.environ.get("ARIEL_OTA_BASE_URL", "https://ota.arielos.com").rstrip("/")
    entry = {
        "datetime": int(meta["post-timestamp"]),
        "filename": filename,
        "id": meta["post-build-incremental"],
        "romtype": ROMTYPE,
        "size": os.path.getsize(zip_path),
        "url": "%s/%s/%s/%s/%s" % (base, device, version, ROMTYPE.lower(), filename),
        "version": version,
    }

    target = os.path.join(out, "index.json")
    with open(target, "w") as f:
        json.dump({"response": [entry]}, f, indent=2)
        f.write("\n")
    print("OTA JSON: %s" % target)


if __name__ == "__main__":
    main()

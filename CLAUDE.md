# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`vendor/ariel` is the ArielOS vendor layer inside an AOSP tree (ArielOS 18.1, a privacy-focused LineageOS 18.1 fork, Android 11). It lives at `<tree-root>/vendor/ariel` and is not built standalone: device trees inherit `config/ariel_common.mk`, which pulls it into the Android build. Branch convention is `ariel-<lineage ref>` (e.g. `ariel-lineage-18.1`). `utils/` has its own `CLAUDE.md` for the upstream-merge tooling.

There is no test or lint suite at this level. Build and verify from the tree root with the usual AOSP flow (`source build/envsetup.sh`, `lunch`/`breakfast`, `m <module>` / `mm` in a module directory, `brunch`).

## Build wiring (read these together)

- Unlike newer branches, there is **no product makefile here** (no `build/target/product`). Products come from device trees (e.g. `device/samsung/a5y17lte`) that inherit `config/ariel_common.mk`.
- `config/ariel_common.mk` is the central hook: `PRODUCT_PACKAGES` (dnscrypt, iptables setup, platform library, ArielSettingsProvider, arielsettings), `PRODUCT_COPY_FILES` (permission/sysconfig XMLs from `prebuilt/common/etc` into `system_ext`; `gps.conf`, `init.ariel.rc` and the backuptool scripts into `system`/`install`), `include vendor/ariel/sepolicy/sepolicy.mk`, `config/ariel_packages.mk`, `PRODUCT_PACKAGE_OVERLAYS += overlay/common/`, and `-include vendor/ariel-priv/keys.mk` (private, optional). A module not named here is not in the image.
- `config/ariel_packages.mk` lists apps (AuroraStore, GmsCore, MicrogVending, ArielGuardian, ArielParental, ArielProtector, DuckDuckGo, SkyTubeExtra, ...). **Their sources are not in this branch** (no `packages/apps`); they come from other repos in the tree.
- `build/envsetup.sh` adds `addlineageremote`, `ariel_ota_json` (runs `build/tools/gen_ota_json.py`; base URL via `ARIEL_OTA_BASE_URL`) and wraps `brunch` to set `BUILD_NUMBER` to a UTC timestamp, print the fingerprint, and generate the OTA JSON. It also has **18.1-only host fixes**: a `MKE2FS_CONFIG` copy that strips `orphan_file` (e2fsprogs 1.47+ vs the bundled mke2fs 1.45.4) and `ALLOW_NINJA_ENV=true` so soong keeps it. Don't remove them.

## Components

- `ariel-sdk/` — ArielOS platform SDK cloned from the LineageOS platform SDK (`com.arielos.platform`, `com.arielos.platform-res`, `com.arielos.platform.xml`). Public API in `sdk/public` (built via `sdk/public/Android.mk`), internal in `sdk/internal`, implementation and resources in `ariel/`. `Android.bp` here is the older Lineage-style file; `Android.mkXX` is intentionally disabled. Its `README.md` is still upstream Lineage text.
- `packages/ArielSettingsProvider` — privileged settings provider app.
- `packages/arielfw` — native firewall daemon (`Android.mk`, `-Werror`): `CommandListener`/`ArielfwCommand` handle a socket command protocol, `FirewallManager` drives rules.
- `packages/iptables_setup` + `ariel_iptables` — boot-time iptables rules.
- `packages/dnscrypt` — prebuilt `dnscrypt-proxy` (v2.1.16), `dnscrypt-proxy.toml`, `init.dnscrypt.rc`, `blocked-names.txt`. Regenerate the blocklist with `python3 build_blocked_names.py [--url <hosts_url>] [--out blocked-names.txt]` (converts a StevenBlack-style hosts file; a bare domain blocks its subdomains).
- `cmds/arielsettings` — command-line settings tool.
- `bootanimation/` — the zip is prebuilt as `prebuilt/bootanimation_ariel.zip` and copied to `product/media/bootanimation.zip` (not an APEX on this branch). `bootanimation/Android.mk` only builds the zip from `desc.txt`/`bootanimation.tar`.
- `overlay/common` — resource overlays (here: `packages/apps/Updater` strings).
- `prebuilt/common` — permission XMLs, default-permissions, sysconfig, `init.ariel.rc`, `gps.conf`, `bin/backuptool.*`, `bin/50-lineage.sh`.
- `sepolicy/` — only `sepolicy/private` exists and is wired via `BOARD_PLAT_PRIVATE_SEPOLICY_DIR` in `sepolicy.mk` (the `BOARD_PLAT_PUBLIC_SEPOLICY_DIR` entry points at a `public` dir that does not exist). Policy for arielfw, dnscrypt, iptables_setup, and platform_app/system_app/system_server tweaks. `sepolicy/private/README.md` mentions the Android 15 `make sepolicy` problem; that does not apply to Android 11, where the normal `make sepolicy` flow works.

## Gotchas

- `ariel-sdk/Android.mkXX` and similar suffixed files are disabled on purpose; don't "fix" them unless asked.
- `ariel_common.mk` has commented-out debug properties (`ro.debuggable`, `ro.adb.secure`, ...) that must not ship in release builds; keep them commented.
- The dnscrypt blocklist, TOML, `init.dnscrypt.rc` and `sepolicy/private/dnscrypt.te` must stay consistent when paths change.
- Some comments and log strings in `envsetup.sh` are in Serbian; leave them as is.

## Custom commits in forked AOSP repos

`utils/custom-commits.md` lists, per project in `utils/lineage-forked-list`, the ArielOS-only commits (with hashes, in cherry-pick order) plus the procedure for porting them to a new LineageOS version. Use it when asked to migrate to a new Lineage release, and refresh it after adding commits to a forked project.

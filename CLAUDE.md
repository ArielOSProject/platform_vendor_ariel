# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`vendor/ariel` is the ArielOS vendor layer inside an AOSP tree (ArielOS 22.2, a privacy-focused LineageOS 22.2 fork). It lives at `<tree-root>/vendor/ariel` and is not built standalone: it is pulled into the Android build through the product makefiles. Branch convention is `ariel-<lineage ref>` (e.g. `ariel-lineage-22.2`). `utils/` has its own `CLAUDE.md` for the upstream-merge tooling.

There is no test or lint suite at this level. Build and verify from the tree root with the usual AOSP flow (`source build/envsetup.sh`, `lunch`/`breakfast`, `m <module>` / `mm` in a module directory, `brunch`).

## Build wiring (read these together)

- `build/target/product/AndroidProducts.mk` registers the only product here, `ariel_sdk_phone_x86_64.mk`, which inherits `config/ariel_common.mk`.
- `config/ariel_common.mk` is the central hook: it lists `PRODUCT_PACKAGES` (dnscrypt, iptables setup, bootanimation APEX, platform library, ArielSettingsProvider, ...), `PRODUCT_COPY_FILES` for permission/sysconfig XMLs from `prebuilt/common/etc` into `system_ext`, and includes `config/ariel_packages.mk`. A module that is not named here (or in `ariel_packages.mk`) is not in the image.
- Files placed outside `system_ext`/`product` (e.g. the platform library jar/res under `/system/framework`) must be added to `PRODUCT_ARTIFACT_PATH_REQUIREMENT_ALLOWED_LIST`, otherwise the artifact-path check fails the build.
- `build/envsetup.sh` extends the Lineage shell helpers: `addlineageremote` and `ariel_ota_json` (runs `build/tools/gen_ota_json.py` to write the Updater `index.json` next to the OTA zip in `$OUT`; base URL via `ARIEL_OTA_BASE_URL`). `ariel_*` lunch targets set `BUILD_NUMBER` to a UTC timestamp.

## Components

- `ariel-sdk/` — ArielOS platform SDK, cloned from the LineageOS platform SDK (`com.arielos.platform`, `com.arielos.platform-res`, `com.arielos.platform.xml`). Public/internal API surface is under `sdk/`, implementation and resources under `ariel/`. Its `README.md` is still the upstream Lineage text.
- `packages/ArielSettingsProvider` — privileged settings provider app (with `privapp_whitelist_*.xml`).
- `packages/arielfw` — native firewall daemon (`Android.mk`, built with `-Werror`): `CommandListener`/`ArielfwCommand` handle a socket command protocol, `FirewallManager` drives rules.
- `packages/iptables_setup` + `ariel_iptables` — boot-time iptables rules.
- `packages/dnscrypt` — prebuilt `dnscrypt-proxy` (v2.1.16), `dnscrypt-proxy.toml`, `init.dnscrypt.rc`, and `blocked-names.txt`. Regenerate the blocklist with `python3 build_blocked_names.py [--url <hosts_url>] [--out blocked-names.txt]` (converts a StevenBlack-style hosts file; a bare domain blocks its subdomains).
- `packages/apps/*` — mostly prebuilt APKs (ArielGuardian, ArielParental, ArielProtector, AuroraStore, DuckDuckGo, SkyTubeExtra) wrapped by `Android.mk`/`Android.bp`. Updating one means replacing the APK binary. `GsfProxy` is declared obsolete.
- `cmds/arielsettings` — command-line settings tool.
- `bootanimation/` — builds a bootanimation zip that is **not** part of the build; the zip is placed under `bootanimation/apex` and shipped as the `com.android.bootanimation` APEX.
- `overlay/common` — framework/package resource overlays.
- `prebuilt/common` — permission XMLs, default-permissions, sysconfig, `init.ariel.rc`, `gps.conf`.
- `sepolicy/` — only `sepolicy/private` is wired in (via `SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS` in `sepolicy.mk`; the `public` dir it references does not exist). Policy for arielfw, dnscrypt, iptables_setup, and platform_app/system_app/system_server tweaks. Since Android 15 `make sepolicy` doesn't work; rebuild with `make clean-system_ext_sepolicy.cil && make system_ext_sepolicy.cil` to surface errors in these files.

## Gotchas

- Several files are stale or disabled by naming (`Android.mkXX`, `Android.bpXXX`, `Android.mkBU`, `Android.bp.backup`); they are intentionally not built. Don't "fix" them without being asked.
- `ariel_common.mk` contains commented-out debug properties (`ro.debuggable=0`, `ro.adb.secure=0`, ...) marked as must-not-ship in release builds; keep them commented.
- Commit history shows frequent changes to dnscrypt blocklists/config; the blocked names and the TOML must stay consistent with `sepolicy/private/dnscrypt.te` and `init.dnscrypt.rc` when paths change.

## Custom commits in forked AOSP repos

`utils/custom-commits.md` lists, per project in `utils/lineage-forked-list`, the ArielOS-only commits (with hashes, in cherry-pick order) plus the procedure for porting them to a new LineageOS version. Use it when asked to migrate to a new Lineage release, and refresh it after adding commits to a forked project.

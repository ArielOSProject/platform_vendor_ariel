# ArielOS custom commits per forked repo

Commits unique to ArielOS (on `ariel-lineage-22.2`, not in `lineage/lineage-22.2`, merges excluded), oldest first, which is the cherry-pick order. Repos come from `lineage-forked-list`. Snapshot taken 2026-10-22. Author is always mikalackis@gmail.com (appears as "Sava Mikalacki" and "Sava Mikalački").

Regenerate for a repo (run in the project dir):

    git log lineage/<ref>..HEAD --no-merges --reverse --format='%h|%ad|%s' --date=short

Migrating to a new Lineage version `<new>`: in each project, `git checkout -b ariel-<new> lineage/<new>`, then `git cherry-pick -x <hashes below, in order>`. Resolve conflicts per commit. Run `lineage-merge.sh`-style fetch first so the `lineage` remote has `<new>`. Update this file afterwards with the new hashes.

## build/make  (list entry: `build`)
| Hash | Date | Subject |
|---|---|---|
| 9e0af87254 | 2023-06-30 | ArielOS: Introduce ArielOS (envsetup.sh, media_system.mk) |

## build/soong
| Hash | Date | Subject |
|---|---|---|
| 8941bc14c | 2025-07-11 | ArielOS: Introduce Ariel Platform (java/*.go, boot jar allowed list) |

## frameworks/base
| Hash | Date | Subject |
|---|---|---|
| 59b437ee660e | 2023-06-30 | ArielOS: Introduce Ariel platform (SystemServer, AssetManager, aapt) |
| 0dba67916bca | 2023-11-02 | Ariel Security Services features (keyguard, biometrics, GestureLauncher, LockSettings) |
| 631fde73b3fc | 2023-11-21 | IntentFirewall: Support blocking apps and their components using package name |
| 2f32c64f139b | 2024-10-22 | ArielOS: Introduce indeterminate lockout (keyguard) |
| 3720c8f14b48 | 2025-04-22 | ArielOS: Fix biometric service crashing; move Ariel SystemServer init to last step |
| 402c51b6d435 | 2025-08-05 | ArielOS: Update code base for lineage 22.2 (idmap2, OverlayConfig, androidfw, keyguard, fingerprint) |

Note: 0dba679, 2f32c64, 3720c8f and 402c51b touch the same keyguard/biometric/SystemServer files, so expect conflicts there. 402c51b is a 22.2-specific port and likely needs rework rather than a plain pick for 23.

## frameworks/opt/telephony
| Hash | Date | Subject |
|---|---|---|
| dd7da9f44b | 2024-10-22 | ArielOS: Send SMS broadcast to Ariel Guardian even if user is not unlocked |
| 53c88ad11f | 2026-09-15 | ArielOS: Fix issue with Samsung RIL returning garbage for SMS service center |

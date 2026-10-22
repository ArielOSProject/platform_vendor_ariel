# ArielOS custom commits per forked repo (lineage-18.1)

Commits unique to ArielOS on `ariel-lineage-18.1`, not in `lineage/lineage-18.1`, merges excluded, oldest first (cherry-pick order). Repos come from this branch's `lineage-forked-list`. Snapshot taken 2026-10-22. Author is mikalackis@gmail.com ("Sava Mikalacki" / "Sava Mikalački"), plus two other emails under the same name in `device/samsung/a5y17lte` (marked below). The 22.2 and 20.0 equivalents are on `ariel-lineage-22.2` and `ariel-lineage-20.0`.

Regenerate for a repo (run in the project dir):

    git log lineage/lineage-18.1..HEAD --no-merges --reverse --format='%h|%ad|%an|%s' --date=short

Then drop commits not authored by you (see "Excluded" below). Migrating to a new Lineage version `<new>`: in each project, `git checkout -b ariel-<new> lineage/<new>`, then `git cherry-pick -x <hashes below, in order>`. Resolve conflicts per commit. Fetch the `lineage` remote first so it has `<new>`. Update this file afterwards with the new hashes.

## build/make  (list entry: `build`)
| Hash | Date | Subject |
|---|---|---|
| 31406e3484 | 2018-08-08 | Introduce Ariel product to build |

## frameworks/base
| Hash | Date | Subject |
|---|---|---|
| 16040855ec2f | 2020-12-03 | Signature spoofing enabled, for microg setup (port only the config.xml gms entry; see microG note) |
| 707b6f3e2d3e | 2022-09-09 | ArielOS: Support security features |
| e9cbb17e727f | 2022-10-12 | ArielOS: Support panic mode |
| 85700b5b25a1 | 2023-11-21 | IntentFirewall: Support blocking apps and their components using package name |
| 00f0c023ad95 | 2024-04-29 | Remove custom signature spoofing, lineageos supports it now (skip; see microG note) |
| 84de0d8ecb2d | 2024-10-22 | ArielOS: Introduce indeterminate lockout, fix issues with refreshing lockout |
| 4f4996cf5a6a | 2024-10-22 | ArielOS: Clear keyguard |
| bc4da29ae0fe | 2025-01-15 | Allow spoofing signingInfo for microG Companion/Services (duplicates Lineage; skip) |

Notes:
- **microG on 18.1 (checked against `lineage/lineage-18.1`):** Lineage 18.1 has its own microG support in `PackageManagerService.java` (`isMicrogSigned()`, `MICROG_FAKE_SIGNATURE`, `fake-signature` metadata; upstream commits 438d9feacfca and ecb003c33be4). ArielOS adds on top:
  - `core/res/res/values/config.xml`: a `com.google.android.gms` entry (microG fused location provider). Lineage has none, so this is your own customization and is still needed. It comes from 16040855ec2f.
  - `mayFakeSignature()` in `PackageManagerService.java` (also from 16040855ec2f): a generic `FAKE_PACKAGE_SIGNATURE`-permission mechanism. 00f0c023ad95 removed that permission from the manifest, so it is most likely dead code (inferred, not tested) that duplicates Lineage's version. Left in place; harmless.
  - bc4da29ae0fe duplicates Lineage's ecb003c33be4 (signingInfo spoofing), so it no longer shows as a diff against Lineage.
  - When porting to 20.0+, Lineage ships the spoofing itself: skip 00f0c023ad95 and bc4da29ae0fe, and from 16040855ec2f carry over only the `config.xml` gms entry (not `mayFakeSignature()`).
- Excluded (not yours): 11 Google/AOSP security-merge commits by Google authors (9349839ad9d0, 4c7fa241dcfc, 40debded9ebd, e33b7b324b42, 27978e536a66, 5e10c8562f46, 669c54e0fb47, 6f7793ac54f5, 68558a0ae5dd, ce2c55fb6236, e93a4b6f95d9). They sit on the branch because Lineage 18.1 had not merged that AOSP tag yet.
- 707b6f3e2d3e, e9cbb17e727f, 84de0d8ecb2d and 4f4996cf5a6a overlap in keyguard/SystemUI files, so expect conflicts.

## device/google/muskie, device/google/wahoo, device/google/walleye
No ArielOS fork found on GitHub (no `ariel-lineage-18.1` branch). Nothing to record. They are in `lineage-forked-list`, so `lineage-merge.sh` will still try to merge them.

## packages/apps/SetupWizard
| Hash | Date | Subject |
|---|---|---|
| dfb248d | 2022-03-22 | Ariel: Simple preparation for beta |
| 0234426 | 2025-08-29 | Remove custom dns setup |

## device/samsung/a5y17lte
| Hash | Date | Subject |
|---|---|---|
| 5134e80 | 2022-03-17 | Introduce Ariel (author email sava@codekrafters.com) |
| e25531e | 2022-03-25 | Include GPS conf file |
| 8eeb065 | 2022-10-17 | Removing gps conf, added by vendor (undoes e25531e; skip both when porting) |
| 23adda0 | 2025-09-17 | Add ariel.dependencies (author email sava.mikalacki@navicogroup.com) |

## packages/apps/Updater
| Hash | Date | Subject |
|---|---|---|
| 3c416f0 | 2022-03-25 | Ariel: Use our own OTA hosting |

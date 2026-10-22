# ArielOS custom commits per forked repo (lineage-20.0)

Commits unique to ArielOS on `ariel-lineage-20.0`, not in `lineage/lineage-20.0`, merges excluded, oldest first (cherry-pick order). Repos come from this branch's `lineage-forked-list`. Snapshot taken 2026-10-22. Author is always mikalackis@gmail.com (appears as "Sava Mikalacki" and "Sava Mikalački"). The 22.2 equivalents are on `ariel-lineage-22.2`.

Regenerate for a repo (run in the project dir):

    git log lineage/lineage-20.0..HEAD --no-merges --reverse --format='%h|%ad|%s' --date=short

Migrating to a new Lineage version `<new>`: in each project, `git checkout -b ariel-<new> lineage/<new>`, then `git cherry-pick -x <hashes below, in order>`. Resolve conflicts per commit. Fetch the `lineage` remote first so it has `<new>`. Update this file afterwards with the new hashes.

## build/make  (list entry: `build`)
| Hash | Date | Subject |
|---|---|---|
| c03c6db1cb | 2023-06-30 | ArielOS: Introduce ArielOS |

## build/soong
| Hash | Date | Subject |
|---|---|---|
| e4e2ccf17 | 2023-06-30 | ArielOS: Introduce ArielOS |

## frameworks/base
| Hash | Date | Subject |
|---|---|---|
| 79df6cd83598 | 2023-06-30 | ArielOS: Introduce Ariel platform |
| be9c01432b9a | 2023-11-02 | Ariel Security Services features |
| ea66209276ef | 2023-11-21 | IntentFirewall: Support blocking apps and their components using package name |
| ebc5cd44cbe9 | 2024-10-22 | ArielOS: Introduce indeterminate lockout |
| 16059d6de105 | 2025-04-22 | Fix biometric service crashing; move Ariel SystemServer init to last step |

Notes:
- The branch contains duplicate copies of three commits (identical patch-ids): `cc4f89c6bff5` = `79df6cd83598`, `4fc22ab7cd17` = `be9c01432b9a`, and the microG pair below. Only one copy of each is listed above; skip the twins.
- Not to be ported: `2e25847b1b99` / `ecb634f7a37a` (microG: Support signature spoofing, 2023-06-30) were undone by `c984e08efbe2` (2024-04-29, "Remove custom microg spoofing, use only lineage"). The branch relies on Lineage's own spoofing.
- Hashes are from the `ariel/ariel-lineage-20.0` fork branch of `android_frameworks_base`.
- The same keyguard/biometric/SystemServer files are touched by several of these, so expect conflicts.

## frameworks/opt/telephony
| Hash | Date | Subject |
|---|---|---|
| 1615db9dcc | 2024-10-22 | ArielOS: Send SMS broadcast to Ariel Guardian even if user is not unlocked |
| f9c880172c | 2025-08-07 | ArielOS: remove duplicate code (follow-up to 1615db9dcc; fold into it when porting) |
| 1e7400574a | 2026-09-15 | ArielOS: Fix issue with Samsung RIL returning garbage for SMS service center |

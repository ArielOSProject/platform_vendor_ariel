# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`vendor/ariel/utils` is a small git repo inside the ArielOS 22.2 AOSP tree (a LineageOS 22.2 fork), at `<tree-root>/vendor/ariel/utils`. It contains only the tooling for pulling upstream LineageOS changes into the ArielOS-forked AOSP projects. There is no build system, lint, or test suite. Sibling directories under `vendor/ariel/` (`build`, `config`, `overlay`, `packages`, `sepolicy`, etc.) are separate concerns.

## Files

- `lineage-forked-list` — one AOSP tree-relative path per line (e.g. `build/soong`, `frameworks/base`) for each project ArielOS forks from LineageOS. Read by the merge script. Add or remove a line to change which projects get merged.
- `lineage-merge.sh` — interactive script. Prompts for a LineageOS ref (e.g. `lineage-22.2`), then for each path in the list:
  1. `rm -fr` the project directory and `repo sync -d -f --force-sync` it, which resets it to the manifest state.
  2. Adds a `lineage` remote at `https://github.com/LineageOS/android_<path with / → _>`, fetches `<ref>`, checks out the local branch `ariel-<ref>`, and merges `lineage/<ref>` into it.
  3. Prints `WARNING!: MERGE CONFLICT` if the merge reports a conflict. Conflicts are left unresolved in that project for manual resolution and are not pushed.
  4. On a clean merge, pushes `ariel-<ref>` to the `ariel` remote (plain push, no force). Each project is left on `ariel-<ref>`, and a summary of pushed / conflicted / failed projects is printed at the end.

## Running

Run from this directory, since the script does `cd ../../../` to reach the tree root and reads `vendor/ariel/utils/lineage-forked-list` relative to that:

```
./lineage-merge.sh
```

## Gotchas

- The script **deletes each listed project directory** (`rm -fr`) before re-syncing. Uncommitted or unpushed work in those projects is lost, so commit and push it first.
- The special case `build` → `build/make` is applied after the existence check and the GitHub project name is computed from the original `build` path. That gives `android_build`, which is the correct LineageOS repo name for `build/make`. Keep this ordering when editing.
- Most command output is captured into `ret` and echoed as `RET:`, so failures (e.g. a failed `git checkout ariel-<ref>`) don't stop the loop. Check the `RET:` lines.
- The local branch convention is `ariel-<lineage ref>`, matching the branch naming of this tree (e.g. `ariel-lineage-22.2`).

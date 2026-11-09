#!/bin/bash
#
# Port the ArielOS-only commits listed in custom-commits.md onto a new LineageOS ref.
#
# For every project in lineage-forked-list: create ariel-<ref> from lineage/<ref> and
# cherry-pick (-x) that project's commits from custom-commits.md, oldest first.
#
# Stops per project on a conflict and again after the conflict was resolved, so that a
# human can review what was changed before the next commit is picked. Progress is kept
# in .port-state/<ref>/, so the script can be re-run any number of times.
#
# Usage:
#   ./lineage-port.sh <ref> [--only <path>] [--dry-run] [--push]   run / resume
#   ./lineage-port.sh <ref> --status                                show progress
#   ./lineage-port.sh <ref> --approve <path>                        accept reviewed resolution
#   ./lineage-port.sh <ref> --approve-all
#
# Env: SRC_REF  ref the hashes in custom-commits.md live on (default lineage-22.2)
#
# Does NOT delete or re-sync anything and never pushes unless --push is given.

set -u
export GIT_TERMINAL_PROMPT=0

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
TREE_ROOT=$(cd "${SCRIPT_DIR}/../../.." && pwd)
MD="${SCRIPT_DIR}/custom-commits.md"
LIST="${SCRIPT_DIR}/lineage-forked-list"
SKIP_FILE="${SCRIPT_DIR}/port-skip.txt"   # "<hash>  # reason": picked by hand, not by the script
SRC_REF="${SRC_REF:-lineage-22.2}"

usage() { sed -n '2,22p' "$0" | sed 's/^# \{0,1\}//'; exit 1; }

[ $# -ge 1 ] || usage
ref="$1"; shift
case "$ref" in -*) usage ;; esac

mode=run; only=""; dry=0; push=0; target=""
while [ $# -gt 0 ]; do
    case "$1" in
        --only)        only="${2:-}"; shift ;;
        --dry-run)     dry=1 ;;
        --push)        push=1 ;;
        --status)      mode=status ;;
        --approve)     mode=approve; target="${2:-}"; shift ;;
        --approve-all) mode=approve_all ;;
        *) usage ;;
    esac
    shift
done

STATE_DIR="${SCRIPT_DIR}/.port-state/${ref}"
BRANCH="ariel-${ref}"

norm_path() { [ "$1" = "build" ] && echo "build/make" || echo "$1"; }
safe_name() { echo "$1" | tr '/' '_'; }

# ---- state ------------------------------------------------------------------
# NEXT: index of the next commit to pick; GATE: none | conflict | review
state_load() {
    NEXT=0; GATE=none; GATE_HASH=""; GATE_BASE=""; GATE_NEW=""
    # shellcheck disable=SC1090
    [ -f "$(state_file "$1")" ] && . "$(state_file "$1")"
}
state_file() { echo "${STATE_DIR}/$(safe_name "$1").state"; }
state_save() {
    mkdir -p "${STATE_DIR}"
    printf 'NEXT=%s\nGATE=%s\nGATE_HASH=%s\nGATE_BASE=%s\nGATE_NEW=%s\n' \
        "$NEXT" "$GATE" "$GATE_HASH" "$GATE_BASE" "$GATE_NEW" > "$(state_file "$1")"
}

# ---- commit list from custom-commits.md -------------------------------------
# prints "<hash>|<subject>" per line for the section "## <path>"
list_commits() {
    awk -v want="$1" -F'|' '
        /^## /     { split($0, a, " "); insec = (a[2] == want); next }
        insec && $2 ~ /^ *[0-9a-f]{7,40} *$/ {
            h = $2; gsub(/ /, "", h)
            s = $4; sub(/^ +/, "", s); sub(/ +$/, "", s)
            print h "|" s
        }' "$MD"
}

is_skipped() {
    [ -f "$SKIP_FILE" ] || return 1
    local s
    while read -r s _; do
        case "$s" in ""|\#*) continue ;; esac
        case "$1" in "$s"*) return 0 ;; esac
        case "$s" in "$1"*) return 0 ;; esac
    done < "$SKIP_FILE"
    return 1
}

projects() {
    local p
    while read -r p || [ -n "$p" ]; do
        case "$p" in ""|\#*) continue ;; esac
        echo "$p"
    done < "$LIST"
}

in_pick() { git -C "$1" rev-parse -q --verify CHERRY_PICK_HEAD >/dev/null 2>&1; }

write_review() { # <path> <dir>
    local f="${STATE_DIR}/$(safe_name "$1").review.txt"
    {
        echo "# Review: ${1}  (${GATE_HASH} -> ${GATE_NEW})"
        git -C "$2" log -1 --format='%s' "$GATE_NEW"
        echo
        echo "## What the resolution changed vs. the original commit (range-diff)"
        git -C "$2" range-diff "${GATE_HASH}^..${GATE_HASH}" "${GATE_NEW}^..${GATE_NEW}" 2>&1
        echo
        echo "## Resulting commit"
        git -C "$2" show --stat --format='%h %s' "$GATE_NEW"
    } > "$f"
    echo "$f"
}

print_gate() { # <path> <dir>
    echo " -> PAUSED for review: ${1}"
    echo "    original: ${GATE_HASH}   resolved as: ${GATE_NEW}"
    echo "    inspect:  git -C ${2} show ${GATE_NEW}"
    echo "              less $(write_review "$1" "$2")"
    echo "    approve:  $0 ${ref} --approve ${1}"
}

# ---- per project ------------------------------------------------------------
# return: 0 done, 1 failed, 2 conflict (open), 3 awaiting review
process_project() {
    local path="$1" gh="$2" dir="${TREE_ROOT}/$1"
    local project url ret rc line h subj base files n

    [ -d "$dir" ] || { echo "Path ${path} does not exist"; return 1; }
    project="android_$(echo "$gh" | tr '/' '_')"

    mapfile -t commits < <(list_commits "$path")
    n=${#commits[@]}
    if [ "$n" -eq 0 ]; then echo " -> no custom commits listed for ${path}"; return 0; fi

    state_load "$path"

    # ---- gates from an earlier run
    if [ "$GATE" = conflict ]; then
        if in_pick "$dir"; then
            echo " -> STILL IN CONFLICT: ${path} (${GATE_HASH}); resolve, 'git add', then 'git cherry-pick --continue'"
            git -C "$dir" diff --name-only --diff-filter=U | sed 's/^/      /'
            return 2
        fi
        if [ "$(git -C "$dir" rev-parse HEAD)" = "$GATE_BASE" ]; then
            echo " -> cherry-pick of ${GATE_HASH} was aborted/skipped; it will be retried (add it to port-skip.txt to skip for good)"
            NEXT=$((NEXT - 1)); GATE=none; state_save "$path"
        else
            GATE=review; GATE_NEW=$(git -C "$dir" rev-parse HEAD); state_save "$path"
        fi
    fi
    if [ "$GATE" = review ]; then print_gate "$path" "$dir"; return 3; fi

    [ "$NEXT" -ge "$n" ] && [ -f "$(state_file "$path")" ] && { echo " -> already complete"; finish_project "$path" "$dir" "$project"; return $?; }

    # ---- set up branch (first run only)
    if [ "$dry" -eq 0 ]; then
        url="https://github.com/LineageOS/${project}"
        git -C "$dir" remote add lineage "$url" 2>/dev/null || git -C "$dir" remote set-url lineage "$url"
        ret=$(git -C "$dir" fetch lineage "+refs/heads/${ref}:refs/remotes/lineage/${ref}" 2>&1 </dev/null) \
            || { echo "RET: $ret"; echo " -> FETCH FAILED (does lineage/${ref} exist for ${project}?)"; return 1; }

        if [ ! -f "$(state_file "$path")" ]; then
            if [ -n "$(git -C "$dir" status --porcelain --untracked-files=no)" ]; then
                echo " -> working tree of ${path} is dirty, refusing"; return 1
            fi
            if git -C "$dir" rev-parse -q --verify "refs/heads/${BRANCH}" >/dev/null; then
                echo " -> branch ${BRANCH} already exists in ${path} (merge result?). Remove it first if you want to start the port."
                return 1
            fi
            ret=$(git -C "$dir" checkout -b "$BRANCH" "lineage/${ref}" 2>&1) || { echo "RET: $ret"; return 1; }
            state_save "$path"
        elif [ "$(git -C "$dir" rev-parse --abbrev-ref HEAD)" != "$BRANCH" ]; then
            ret=$(git -C "$dir" checkout "$BRANCH" 2>&1) || { echo "RET: $ret"; return 1; }
        fi
    fi

    # ---- pick
    while [ "$NEXT" -lt "$n" ]; do
        line="${commits[$NEXT]}"; h="${line%%|*}"; subj="${line#*|}"

        if is_skipped "$h"; then
            echo " -> [$((NEXT+1))/${n}] ${h} SKIPPED (port-skip.txt): ${subj}"
            NEXT=$((NEXT + 1)); [ "$dry" -eq 0 ] && state_save "$path"; continue
        fi
        if [ "$dry" -eq 1 ]; then
            echo " -> [$((NEXT+1))/${n}] would pick ${h}: ${subj}"; NEXT=$((NEXT + 1)); continue
        fi

        if ! git -C "$dir" cat-file -e "${h}^{commit}" 2>/dev/null; then
            git -C "$dir" fetch ariel "$SRC_REF" >/dev/null 2>&1 </dev/null
            git -C "$dir" fetch ariel "ariel-${SRC_REF}" >/dev/null 2>&1 </dev/null
            git -C "$dir" cat-file -e "${h}^{commit}" 2>/dev/null \
                || { echo " -> commit ${h} not found in ${path} (tried fetching ariel remote)"; return 1; }
        fi

        base=$(git -C "$dir" rev-parse HEAD)
        echo " -> [$((NEXT+1))/${n}] picking ${h}: ${subj}"
        ret=$(GIT_EDITOR=true git -C "$dir" cherry-pick -x "$h" 2>&1 </dev/null); rc=$?
        if [ $rc -eq 0 ]; then
            NEXT=$((NEXT + 1)); state_save "$path"
        elif in_pick "$dir"; then
            files=$(git -C "$dir" diff --name-only --diff-filter=U)
            if [ -z "$files" ]; then   # nothing to resolve: change already upstream
                git -C "$dir" cherry-pick --skip >/dev/null 2>&1
                echo "    already in lineage/${ref} (empty pick), dropped"
                NEXT=$((NEXT + 1)); state_save "$path"
            else
                echo "    CONFLICT in ${path}:"; echo "$files" | sed 's/^/      /'
                NEXT=$((NEXT + 1)); GATE=conflict; GATE_HASH="$h"; GATE_BASE="$base"; GATE_NEW=""
                state_save "$path"
                return 2
            fi
        else
            echo "RET: $ret"; echo " -> CHERRY-PICK FAILED"; return 1
        fi
    done

    [ "$dry" -eq 1 ] && return 0
    finish_project "$path" "$dir" "$project"
}

finish_project() {
    local path="$1" dir="$2" project="$3" ret
    echo " -> all commits of ${path} applied on ${BRANCH}"
    if [ "$push" -eq 1 ]; then
        git -C "$dir" remote set-url --push ariel "git@github.com:ArielOSProject/${project}.git"
        ret=$(git -C "$dir" push ariel "${BRANCH}:${BRANCH}" 2>&1 </dev/null) \
            || { echo "RET: $ret"; echo " -> PUSH FAILED"; return 1; }
        echo " -> pushed ${BRANCH}"
    else
        echo "    (not pushed; re-run with --push after review)"
    fi
    return 0
}

# ---- modes ------------------------------------------------------------------
case "$mode" in
status)
    while read -r p; do
        path=$(norm_path "$p"); state_load "$path"
        total=$(list_commits "$path" | wc -l)
        printf '%-45s %s/%s  gate=%s\n' "$path" "$NEXT" "$total" "$GATE"
    done < <(projects)
    exit 0 ;;
approve|approve_all)
    while read -r p; do
        path=$(norm_path "$p")
        if [ "$mode" = approve ] && [ "$path" != "$(norm_path "$target")" ]; then continue; fi
        state_load "$path"
        if [ "$GATE" = review ]; then
            GATE=none; state_save "$path"; echo "approved ${path} (${GATE_HASH})"
        elif [ "$mode" = approve ]; then
            echo "${path}: nothing to approve (gate=${GATE})"
        fi
    done < <(projects)
    exit 0 ;;
esac

ok=(); conflict=(); review=(); failed=()
while read -r p; do
    path=$(norm_path "$p")
    if [ -n "$only" ] && [ "$path" != "$(norm_path "$only")" ]; then continue; fi
    echo ""; echo "====================================================================="
    echo " PROJECT: ${path}"
    process_project "$path" "$p"
    case $? in
        0) ok+=("$path") ;;
        2) conflict+=("$path") ;;
        3) review+=("$path") ;;
        *) failed+=("$path") ;;
    esac
done < <(projects)

echo ""; echo "====================================================================="
echo " SUMMARY (${BRANCH}$([ "$dry" -eq 1 ] && echo ', dry-run'))"
echo " Done:                ${ok[*]:-none}"
echo " Conflict (resolve):  ${conflict[*]:-none}"
echo " Awaiting review:     ${review[*]:-none}"
echo " Failed:              ${failed[*]:-none}"
if [ "$dry" -eq 0 ] && [ ${#ok[@]} -gt 0 ]; then
    echo ""
    echo " New hashes for custom-commits.md:"
    for path in "${ok[@]}"; do
        echo "## ${path}"
        git -C "${TREE_ROOT}/${path}" log "lineage/${ref}..${BRANCH}" --no-merges --reverse --format='| %h | %ad | %s |' --date=short
    done
fi
[ ${#conflict[@]} -eq 0 ] && [ ${#review[@]} -eq 0 ] && [ ${#failed[@]} -eq 0 ]

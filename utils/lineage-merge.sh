#!/bin/bash
#
# Copyright (C) 2016 OmniROM Project
# Copyright (C) 2018 ArielOS Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

read -r -p "Enter the LineageOS ref to merge: " ref;
if [ -z "${ref}" ]; then
    echo "No ref given, aborting";
    exit 1
fi

# never block on a credential prompt
export GIT_TERMINAL_PROMPT=0

cd ../../../ || exit 1

# sync the whole tree first, so the forked projects are fixed up on top of a current manifest state
echo "";
echo "=====================================================================";
echo " -> repo sync (whole tree)";
if ! repo sync -d -f --force-sync -j"$(nproc)" </dev/null; then
    echo " -> WARNING!: GLOBAL REPO SYNC FAILED, aborting";
    exit 1
fi

pushed=();
uptodate=();
conflicts=();
failed=();
dev_switched=();
dev_kept=();

# the list is read on fd 3 so repo/git can't swallow it via stdin
while read -r -u3 path || [ -n "$path" ];
    do

    # skip blank lines and comments
    case "${path}" in
        ""|\#*) continue ;;
    esac

    project=$(echo "android_${path}" | sed -e 's/\//_/g');

    if [ ! -d "${path}" ] ; then
        echo "Path ${path} does not exist, skipping";
        continue
    fi

    if [ "${path}" == "build" ] ; then
        path="build/make";
    fi

    echo "";
    echo "=====================================================================";
    echo " PROJECT: ${project} -> [ ${path}/ ]";
    echo "";

    rm -fr "${path}";
    echo " -> repo sync ${path}";
    ret=$(repo sync -d -f --force-sync "${path}" 2>&1 </dev/null);
    sync_rc=$?
    if [ $sync_rc -ne 0 ] || ! git -C "${path}" rev-parse --git-dir &> /dev/null; then
        echo "RET: $ret"
        echo " -> WARNING!: SYNC FAILED";
        failed+=("${path} (sync)");
        continue
    fi

    # make sure that environment is clean (repo keeps the gitdir across rm -fr)
    echo "Cleaning out repo..."
    git -C "${path}" merge --abort &> /dev/null

    echo "Setting up lineage remote..."
    url="https://github.com/LineageOS/${project}"
    git -C "${path}" remote add lineage "${url}" 2> /dev/null \
        || git -C "${path}" remote set-url lineage "${url}"

    echo "Fetching $ref..."
    ret=$(git -C "${path}" fetch lineage "+refs/heads/${ref}:refs/remotes/lineage/${ref}" 2>&1 </dev/null);
    fetch_rc=$?
    echo "RET: $ret"

    if [ $fetch_rc -ne 0 ]; then
        echo " -> WARNING!: FETCH FAILED";
        failed+=("${path} (fetch)");
    else
        echo "Checkout ariel-$ref..."
        ret=$(git -C "${path}" checkout "ariel-${ref}" 2>&1);
        if [ $? -ne 0 ]; then
            echo "RET: $ret"
            ret=$(git -C "${path}" checkout -B "ariel-${ref}" "ariel/ariel-${ref}" 2>&1);
        fi
        checkout_rc=$?
        echo "RET: $ret"

        if [ $checkout_rc -ne 0 ]; then
            echo " -> WARNING!: CHECKOUT FAILED";
            failed+=("${path} (checkout)");
        else
            # repo keeps the gitdir, so the local branch can be stale: bring in what is already on the ariel remote
            echo "Syncing ariel-$ref with ariel/ariel-$ref..."
            ret=$(git -C "${path}" fetch ariel "+refs/heads/ariel-${ref}:refs/remotes/ariel/ariel-${ref}" 2>&1 </dev/null) \
                && ret=$(git -C "${path}" merge "ariel/ariel-${ref}" --no-edit 2>&1)
            ariel_rc=$?
            echo "RET: $ret"

            if [ $ariel_rc -ne 0 ]; then
                echo " -> WARNING!: SYNC WITH ariel REMOTE FAILED, not merging lineage";
                conflicts+=("${path} (ariel remote)");
            else
                echo " -> Merging remote: ${url} ${ref}";
                ret=$(git -C "${path}" merge "lineage/${ref}" --no-edit 2>&1);
                merge_rc=$?
                echo "RET: $ret"

                if [ $merge_rc -ne 0 ]; then
                    echo " -> WARNING!: MERGE CONFLICT";
                    conflicts+=("${path}");
                elif echo "$ret" | grep -q "Already up to date"; then
                    echo " -> Already up to date, nothing to push";
                    uptodate+=("${path}");
                else
                    echo " -> Pushing ariel-${ref} to ariel remote...";
                    # repo sync resets the remote to the manifest's https url, push over ssh instead
                    git -C "${path}" remote set-url --push ariel "git@github.com:ArielOSProject/${project}.git"
                    ret=$(git -C "${path}" push ariel "ariel-${ref}:ariel-${ref}" 2>&1 </dev/null);
                    push_rc=$?
                    echo "RET: $ret"
                    if [ $push_rc -ne 0 ]; then
                        echo " -> WARNING!: PUSH FAILED";
                        failed+=("${path} (push)");
                    else
                        echo " -> DONE!";
                        pushed+=("${path}");
                    fi
                fi
            fi
        fi
    fi

    # leave the project on the branch the manifest defines for it, not detached.
    # skipped while a merge is pending, so the conflict state stays intact for manual resolution
    if git -C "${path}" rev-parse -q --verify MERGE_HEAD &> /dev/null; then
        echo " -> merge pending, leaving project as is";
        continue
    fi
    mrev=$(repo forall "${path}" -c 'echo $REPO_RREV' 2>/dev/null </dev/null | tail -n1);
    mremote=$(repo forall "${path}" -c 'echo $REPO_REMOTE' 2>/dev/null </dev/null | tail -n1);
    mbranch="${mrev#refs/heads/}";
    if [ "${mbranch}" == "${mrev}" ] || [ -z "${mbranch}" ]; then
        echo " -> manifest revision '${mrev}' is not a branch, leaving project as is";
    else
        ret=$(git -C "${path}" checkout "${mbranch}" 2>&1 \
            || git -C "${path}" checkout -B "${mbranch}" "${mremote}/${mbranch}" 2>&1);
        cur=$(git -C "${path}" rev-parse --abbrev-ref HEAD 2>/dev/null);
        if [ "${cur}" != "${mbranch}" ]; then
            echo " -> WARNING!: NOT ON manifest branch ${mbranch}";
            echo "RET: $ret"
            failed+=("${path} (manifest branch)");
        else
            echo " -> On manifest branch ${mbranch}";
        fi
    fi

done 3< vendor/ariel/utils/lineage-forked-list;

# device source trees (local manifests: device/kernel/hardware/vendor): put them on ariel-<ref> where that branch
# exists (locally or on the ariel remote), otherwise leave them exactly as they are
echo "";
echo "=====================================================================";
echo " DEVICE TREES -> ariel-${ref} where available";
while read -r -u3 dpath || [ -n "$dpath" ];
    do

    # projects from the forked list were handled above
    if grep -qxF "${dpath}" vendor/ariel/utils/lineage-forked-list; then
        continue
    fi
    if [ ! -d "${dpath}" ] || ! git -C "${dpath}" rev-parse --git-dir &> /dev/null; then
        continue
    fi

    have_local=0; have_remote=0;
    if git -C "${dpath}" remote | grep -qx ariel; then
        git -C "${dpath}" fetch ariel "+refs/heads/ariel-${ref}:refs/remotes/ariel/ariel-${ref}" &> /dev/null </dev/null \
            && have_remote=1
    fi
    git -C "${dpath}" rev-parse -q --verify "refs/heads/ariel-${ref}" &> /dev/null && have_local=1

    if [ $have_local -eq 0 ] && [ $have_remote -eq 0 ]; then
        echo " ${dpath}: no ariel-${ref}, leaving as is";
        dev_kept+=("${dpath}");
        continue
    fi

    if [ $have_local -eq 1 ]; then
        ret=$(git -C "${dpath}" checkout "ariel-${ref}" 2>&1);
        rc=$?
        # bring a stale local branch up to date with the ariel remote (fast-forward only)
        if [ $rc -eq 0 ] && [ $have_remote -eq 1 ]; then
            git -C "${dpath}" merge --ff-only "ariel/ariel-${ref}" &> /dev/null
        fi
    else
        ret=$(git -C "${dpath}" checkout -B "ariel-${ref}" "ariel/ariel-${ref}" 2>&1);
        rc=$?
    fi

    if [ $rc -ne 0 ]; then
        echo " ${dpath}: WARNING!: CHECKOUT ariel-${ref} FAILED";
        echo "RET: $ret"
        failed+=("${dpath} (device checkout)");
    else
        echo " ${dpath}: on ariel-${ref}";
        dev_switched+=("${dpath}");
    fi

done 3< <(grep -hEo '<project [^>]*path="[^"]+"' .repo/local_manifests/*.xml 2>/dev/null \
            | sed -e 's/.*path="\([^"]*\)"/\1/' | grep -E '^(device|kernel|hardware|vendor)/' | sort -u);

echo "";
echo "=====================================================================";
echo " SUMMARY";
echo " Merged and pushed: ${pushed[*]:-none}";
echo " Already up to date: ${uptodate[*]:-none}";
echo " Merge conflicts (not pushed): ${conflicts[*]:-none}";
echo " Device trees on ariel-${ref}: ${dev_switched[*]:-none}";
echo " Device trees left as is (no ariel-${ref}): ${dev_kept[*]:-none}";
echo " Failed: ${failed[*]:-none}";

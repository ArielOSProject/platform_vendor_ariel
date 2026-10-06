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

pushed=();
uptodate=();
conflicts=();
failed=();

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

    # always leave the project on ariel-${ref}
    if [ "$(git -C "${path}" rev-parse --abbrev-ref HEAD 2>/dev/null)" != "ariel-${ref}" ]; then
        ret=$(git -C "${path}" checkout "ariel-${ref}" 2>&1);
        if [ "$(git -C "${path}" rev-parse --abbrev-ref HEAD 2>/dev/null)" != "ariel-${ref}" ]; then
            echo " -> WARNING!: NOT ON ariel-${ref}";
            echo "RET: $ret"
        fi
    fi

done 3< vendor/ariel/utils/lineage-forked-list;

echo "";
echo "=====================================================================";
echo " SUMMARY";
echo " Merged and pushed: ${pushed[*]:-none}";
echo " Already up to date: ${uptodate[*]:-none}";
echo " Merge conflicts (not pushed): ${conflicts[*]:-none}";
echo " Failed: ${failed[*]:-none}";

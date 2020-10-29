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

echo -e "Enter the LineageOS ref to merge";
read ref;

cd ../../../

while read path || [ -n "$path" ];
    
    do

    project=`echo android_${path} | sed -e 's/\//\_/g'`;

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

    rm -fr $path;
    echo " -> repo sync ${path}";
    ret=$(repo sync -d -f --force-sync ${path} 2>&1);
    cd $path;


    if ! git rev-parse --git-dir &> /dev/null
    then
        echo ".git directory not found. Please run this from the root directory of the Android repository you wish to sync."
    else
        # make sure that environment is clean
        echo "Cleaning out repo..."
        ret=$(git merge --abort 2>&1);
        echo "RET: $ret"

        echo "Removing lineage repo..."
        ret=$(git remote rm lineage 2>&1)
        echo "RET: $ret"

        echo "Adding lineage repo..."
        ret=$(git remote add lineage https://github.com/LineageOS/$project 2>&1)
        echo "RET: $ret"

        echo "Fetching $ref..."
        ret=$(git pull lineage ${ref} 2>&1);
        echo "RET: $ret"

        echo "Checkout ariel-$ref..."
        ret=$(git checkout ariel-${ref} 2>&1);
        echo "RET: $ret"

        echo " -> Merging remote: https://github.com/LineageOS/$project ${ref}";
        ret=$(git merge lineage/$ref --no-edit 2>&1);
        echo "RET: $ret"

        if echo $ret | grep "CONFLICT (content)" > /dev/null ; then
            echo " -> WARNING!: MERGE CONFLICT";
        else
            ret=$(git checkout ariel-${ref} 2>&1);
            echo " -> DONE!";
        fi

        cd - > /dev/null;
    fi

done < vendor/ariel/utils/lineage-forked-list;

# echo " -> Performing full sync again...";
# ret=$(repo sync 2>&1);
# echo "Sync complete: $ret"
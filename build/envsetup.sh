function __print_ariel_functions_help() {
cat <<EOF
Additional ArielOS functions:
- addlineageremote:   Add git remote for LineageOS.
EOF
}

function addlineageremote()
{
    if ! git rev-parse --git-dir &> /dev/null
    then
        echo ".git directory not found. Please run this from the root directory of the Android repository you wish to set up."
        return 1
    fi
    git remote rm lineage 2> /dev/null
    local PROJECT=$(pwd -P | sed -e "s#$ANDROID_BUILD_TOP\/##; s#-caf.*##; s#\/default##")
    # Google moved the repo location in Oreo
    if [ $PROJECT = "build/make" ]
    then
        PROJECT="build"
    fi
    if (echo $PROJECT | grep -qv "^device")
    then
        local PFX="android_"
    fi

    PROJECT="${PROJECT//\//_}"

    git remote add lineage https://github.com/LineageOS/$PFX$PROJECT
    echo "Remote 'lineage' created"
}
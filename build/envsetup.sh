
# we need to export this as a default fallback branch since device repos, kernels
# and other dependencies use different format for branch names
export ROOMSERVICE_BRANCHES=lineage-20

function __print_ariel_functions_help() {
cat <<EOF
Additional ArielOS functions:
- addlineageremote:   Add git remote for LineageOS.
EOF
}

# check to see if the supplied product is one we can build
function check_product()
{
    local T=$(gettop)
    if [ ! "$T" ]; then
        echo "Couldn't locate the top of the tree.  Try setting TOP." >&2
        return
    fi
    if (echo -n $1 | grep -q -e "^lineage_") ; then
        LINEAGE_BUILD=$(echo -n $1 | sed -e 's/^lineage_//g')
    else
        if (echo -n $1 | grep -q -e "^ariel_") ; then
           LINEAGE_BUILD=$(echo -n $1 | sed -e 's/^ariel_//g')
        else
           LINEAGE_BUILD=
        fi
    fi
    export LINEAGE_BUILD

        TARGET_PRODUCT=$1 \
        TARGET_RELEASE=$2 \
        TARGET_BUILD_VARIANT= \
        TARGET_BUILD_TYPE= \
        TARGET_BUILD_APPS= \
        get_build_var TARGET_DEVICE > /dev/null
    # hide successful answers, but allow the errors to show
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
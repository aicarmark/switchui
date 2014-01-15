#!/bin/bash
#-------------------------------------------------------------------------------
#  build.bash - phoenix application build script
#-------------------------------------------------------------------------------

# to make bash scripts behave like makefiles, exit on any error
set -e

script_name=$(basename ${0})

function usage {
    echo ""
    echo "Usage: ${script_name} -d -p"
    echo "      d: debug"
    echo "      p: use mmi proxy"
    echo ""
    exit 1
}


while getopts dp o
do
    case "$o" in
        d)  set -x ;;
        p)  mmi_proxy=true ;;
        [?]) usage ;;
    esac
done

script_dir=$(cd $(dirname ${0}); pwd)
top_dir=$(cd ${script_dir}/..; pwd)

if [ ! -e ${script_dir}/build.conf ]; then
    cd ${script_dir}
    if [ -d ${script_dir}/build ]; then
        rm -rf ${script_dir}/build
    fi
    git clone ssh://git.blurdev.com/home/repo/dev/apps/build.git -b main-dev-ics
    source ${script_dir}/build/build-common.conf
else
    source ${script_dir}/build.conf
fi

application_dir=${top_dir}/SmartActions
lib_android_wrapper_dir=${top_dir}/AndroidWrapper
lib_market_checkin_dir=${top_dir}/MarketCheckin
mmi_apps_sdk_dir=${android_sdk_dir}/add-ons/mmi_apps_sdk

# If we are in the mmi network use a proxy
if [ "${mmi_proxy}" == "true" ];then
    wget_proxy_argument="--execute=http_proxy=wwwgate0.mot.com:1080"
    android_proxy_argument="--proxy-host wwwgate0.mot.com --proxy-port 1080"
fi

if [ ! -d ${android_sdk_dir} ]; then
    if [ "${mmi_proxy}" == "true" ];then
        wget_command="wget"
        wget_command="${wget_command} ${wget_proxy_argument}"
        ${wget_command} http://dl.google.com/android/${android_sdk_starter_pkg} -O ${top_dir}/${android_sdk_starter_pkg}
        if [ "${operating_system}" == "Darwin" ]; then
            unzip ${top_dir}/${android_sdk_starter_pkg} -d ${top_dir}
        elif [ "${operating_system}" == "Linux" ]; then
            tar xfz ${top_dir}/${android_sdk_starter_pkg} --directory ${top_dir}
        fi
    else
        android_sdk_branch=${android_sdk_starter_pkg_prefix}
        if [ "${operating_system}" == "Darwin" ]; then
            android_sdk_branch=${android_sdk_branch}-macosx
            git clone ssh://gerrit.pcs.mot.com/home/repo/dev/AndroidSDK.git -b ${android_sdk_branch}
            mv AndroidSDK ${android_sdk_dir}
        elif [ "${operating_system}" == "Linux" ]; then
            android_sdk_branch=${android_sdk_branch}-linux
            git clone ssh://gerrit.pcs.mot.com/home/repo/dev/AndroidSDK.git -b ${android_sdk_branch}
            mv AndroidSDK ${android_sdk_dir}
        fi
    fi
fi

if [ "${mmi_proxy}" == "true" ];then
    android_list_command="${android_sdk_dir}/tools/android list sdk"
    android_list_command="${android_list_command} ${android_proxy_argument}"

    android_sdk_package_index=$(${android_list_command} | grep "${android_platform_sdk_version}" | cut -f1 -d "-")
    android_sdk_package_index=${android_sdk_package_index//[[:space:]]}
    if [ ! -z "${android_google_api_version}" ]; then
        android_sdk_package_index="${android_sdk_package_index}, $(${android_list_command} | grep "${android_google_api_version}" | cut -f1 -d "-")"
        android_sdk_package_index=${android_sdk_package_index//[[:space:]]}
    fi

    android_update_command="${android_sdk_dir}/tools/android update sdk"
    android_update_command="${android_update_command} ${android_proxy_argument}"

    ${android_update_command} --no-ui --filter "tool, platform-tool, ${android_sdk_package_index}"

    # install Google maps corresponding  to the sdk rev level
    PLATFORM_SDK_VERSION=$(echo ${android_platform_sdk_version} | cut -f4 -d " ")
    google_apis_name="addon-google_apis-google-"
    ${android_update_command} --no-ui --filter ${google_apis_name}$PLATFORM_SDK_VERSION

    ${android_sdk_dir}/platform-tools/adb kill-server || true
fi

# check to see if the current mmi apps sdk is present, if not install it
if [ ! -f ${top_dir}/${mmi_apps_sdk} ]; then
    rm -rf ${mmi_apps_sdk_dir} ${top_dir}/mmi_apps_sdk_*
    scp ${sdk_path}/${mmi_apps_sdk} ${top_dir}
    unzip ${top_dir}/${mmi_apps_sdk} -d ${top_dir}
    mv ${top_dir}/${mmi_apps_sdk%.zip} ${mmi_apps_sdk_dir}
fi

# HACK: remove library while it is zero length
#       delete this hack when the issue has been resolved
rm -f ${mmi_apps_sdk_dir}/libs/com.motorola.blur.library.app.service.jar

# Have android tool properly set sdk.dir property in local.properties file
${android_sdk_dir}/tools/android update project --path ${application_dir}
${android_sdk_dir}/tools/android update lib-project --path ${lib_android_wrapper_dir}
${android_sdk_dir}/tools/android update lib-project --path ${lib_market_checkin_dir}
cd ${application_dir}

ant clean release

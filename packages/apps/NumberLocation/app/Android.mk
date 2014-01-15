LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng


LOCAL_PROJ_DEPS := $(call intermediates-dir-for,APPS,blur-res,,COMMON)/src/R.stamp

# LOCAL_AAPT_INCLUDES are required for all blur apps to compile against blur.res
LOCAL_AAPT_INCLUDES := $(call intermediates-dir-for,APPS,blur-res,,COMMON)/package-export.apk

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += \
    src/com/motorola/numberlocation/INumberLocationService.aidl \
    src/com/motorola/numberlocation/INumberLocationCallback.aidl


## Filter out the plugin and samples when build AtCmd.apk
#LOCAL_SRC_FILES := $(filter-out \
#                       pluginMgr/% samples/% pulgins/% base/% \
#                       ,$(LOCAL_SRC_FILES))

#LOCAL_JAVA_LIBRARIES := com.motorola.atcmd.base com.motorola.atcmd.pluginMgr com.motorola.android.telephony
#LOCAL_REQUIRED_MODULES := com.motorola.atcmd.base com.motorola.atcmd.pluginMgr com.motorola.android.telephony
#LOCAL_JNI_SHARED_LIBRARIES := libqnutil
LOCAL_PACKAGE_NAME := ChinaNumberLocation

LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags

LOCAL_CERTIFICATE := platform

LOCAL_DEX_PREOPT := false

include $(BUILD_PACKAGE)


LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_DEX_PREOPT := false
LOCAL_MODULE_TAGS := optional eng

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_SRC_FILES += \
        src/com/motorola/numberlocation/INumberLocationService.aidl \
        src/com/motorola/numberlocation/INumberLocationCallback.aidl \
        src/com/motorola/firewall/CallFirewallCallback.aidl \
        src/com/motorola/firewall/SmsFirewallCallback.aidl \
        src/com/android/phone/PhoneHub.aidl \
        src/com/android/mms/SmsHub.aidl


LOCAL_PACKAGE_NAME := Firewall

#LOCAL_PROJ_DEPS := $(call intermediates-dir-for,APPS,blur-res,,COMMON)/src/R.stamp

# LOCAL_AAPT_INCLUDES are required for all blur apps to compile against blur.res
#LOCAL_AAPT_INCLUDES := $(call intermediates-dir-for,APPS,blur-res,,COMMON)/package-export.apk

# MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
# LOCAL_JAVA_LIBRARIES := \
        com.motorola.blur.library.utilities
# MOT Calling Code End 

include $(BUILD_PACKAGE)

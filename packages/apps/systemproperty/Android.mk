LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CERTIFICATE := common

LOCAL_MODULE_TAGS := eng 

LOCAL_DEX_PREOPT := false

LOCAL_PACKAGE_NAME := PerformanceMaster
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += \
    src/android/content/pm/IPackageDataObserver.aidl \
    src/android/content/pm/IPackageStatsObserver.aidl \
    src/android/content/pm/IPackageManager.aidl \
    src/android/content/pm/IPackageDeleteObserver.aidl


#LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

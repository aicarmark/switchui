LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng

LOCAL_SRC_FILES := $(call all-subdir-java-files) \
					src/com/motorola/batterymanager/IBatteryManager.aidl \
					src/com/motorola/batterymanager/IWlMonitor.aidl
VAR_PACKAGE_NAME := BatteryManager

LOCAL_PACKAGE_NAME := $(VAR_PACKAGE_NAME)
LOCAL_CERTIFICATE := platform
LOCAL_DEX_PREOPT := false

include $(BUILD_PACKAGE)

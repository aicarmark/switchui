LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng
LOCAL_SRC_FILES := $(call all-java-files-under, ../src)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/overlay $(LOCAL_PATH)/../res

LOCAL_PACKAGE_NAME := FileManagerWithDrm

LOCAL_DEX_PREOPT := false

include $(BUILD_PACKAGE)


LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_PACKAGE_NAME := ActivityGraphEx
LOCAL_MODULE_TAGS := optional eng
LOCAL_DEX_PREOPT := false
LOCAL_CERTIFICATE := shared
LOCAL_SRC_FILES := $(call all-java-files-under,src)
#LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res_hdpi
include $(BUILD_PACKAGE)

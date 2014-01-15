LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_PACKAGE_NAME := SDBackup
LOCAL_CERTIFICATE := platform

LOCAL_STATIC_JAVA_LIBRARIES := com.android.vcard
LOCAL_DEX_PREOPT := false

include $(BUILD_PACKAGE)


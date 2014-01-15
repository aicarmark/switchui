LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng

LOCAL_SRC_FILES := $(call all-subdir-java-files) 
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/overlay/fullLoad $(LOCAL_PATH)/res
LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags

LOCAL_JNI_SHARED_LIBRARIES := libqnutil
LOCAL_PACKAGE_NAME := QuickNote
LOCAL_DEX_PREOPT := false

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_PACKAGE_NAME := QuickNoteText
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/overlay/textLoad $(LOCAL_PATH)/res
LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags

LOCAL_DEX_PREOPT := false

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)


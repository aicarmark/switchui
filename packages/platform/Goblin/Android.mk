LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE:= pluginframework

LOCAL_MODULE_TAGS := optional

LOCAL_DEX_PREOPT := false

LOCAL_SRC_FILES := $(call all-java-files-under,com)

include $(BUILD_JAVA_LIBRARY)


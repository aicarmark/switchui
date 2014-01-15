LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

#LOCAL_MODULE_TAGS := eng user development
LOCAL_MODULE_TAGS := optional eng

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := WorldClock

LOCAL_CERTIFICATE := platform

LOCAL_SDK_VERSION := current

LOCAL_DEX_PREOPT := false

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

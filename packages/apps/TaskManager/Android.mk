LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-subdir-java-files)


VAR_PACKAGE_NAME := TaskManager

ifneq ($(MMSP_PRODUCT),)
  VAR_PACKAGE_NAME := $(strip $(VAR_PACKAGE_NAME)_$(DENSITY_ASSETS))
endif

LOCAL_PACKAGE_NAME := $(VAR_PACKAGE_NAME)

LOCAL_CERTIFICATE := platform

LOCAL_DEX_PREOPT := false

# LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

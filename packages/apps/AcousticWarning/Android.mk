#    Copyright (C) 2011 Motorola, Inc.
#    All Rights Reserved

LOCAL_PATH:= $(call my-dir)
#########################20hr
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng

$(shell rm -fr $(LOCAL_PATH)/res20)
$(shell mkdir -p $(LOCAL_PATH)/res20)
$(shell cp -a $(LOCAL_PATH)/res/* $(LOCAL_PATH)/res20/)
$(shell rm -fr $(LOCAL_PATH)/res20/values/config.xml)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res20

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := MMCPAcousticWarning
LOCAL_DEX_PREOPT := false

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

#########################10mins
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng

$(shell rm -fr $(LOCAL_PATH)/res10)
$(shell mkdir -p $(LOCAL_PATH)/res10)
$(shell cp -a $(LOCAL_PATH)/res/* $(LOCAL_PATH)/res10/)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res10

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := MMCPAcousticWarning10
LOCAL_DEX_PREOPT := false

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

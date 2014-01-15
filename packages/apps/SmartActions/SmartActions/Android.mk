#
# Copyright (C) 2011 Motorola Mobility, Inc
#
###############################################################################

###############################################################################
# Build the SmartActions app
###############################################################################

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_PACKAGE_NAME := SmartActions

LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags

LOCAL_REQUIRED_MODULES := com.motorola.smartaction.xml \
                          com.motorola.smartactions_whitelist.xml

LOCAL_STATIC_JAVA_LIBRARIES := AndroidWrapper \
                               LoaderInterface \
                               MarketCheckin

LOCAL_JAVA_LIBRARIES := com.google.android.maps

LOCAL_ROOT_DIR := $(shell (cd $(LOCAL_PATH); pwd))

LOCAL_SRC_FILES := \
                src/com/motorola/contracts/messaging/IMessagingServiceCallback.aidl \
                src/com/motorola/contracts/messaging/IMessagingService.aidl \
                $(call all-java-files-under, src)

LOCAL_CERTIFICATE := common

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

# Export libraries for use by the app
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
         AndroidWrapper:libs/AndroidWrapper.jar\
         LoaderInterface:libs/LoaderInterface.jar \
         MarketCheckin:libs/MarketCheckin.jar

LOCAL_MODULE_TAGS := optional

include $(BUILD_MULTI_PREBUILT)

###############################################################################

# ========================================================
# Install the permissions file into system/etc/permissions
# to ensure Market Upgrades can only occur to devices which
# have preloaded this application
# ========================================================
include $(CLEAR_VARS)
LOCAL_MODULE := com.motorola.smartaction.xml
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC

LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions
LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)

###############################################################################

# ========================================================
# Install the permissions file into system/etc/permissions
# to ensure Market Upgrades can only occur to devices which
# have preloaded this application
# ========================================================
include $(CLEAR_VARS)
LOCAL_MODULE := com.motorola.smartactions_whitelist.xml
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC

LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/smartactions
LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)

###############################################################################


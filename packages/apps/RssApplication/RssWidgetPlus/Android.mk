# ============================================================
# This is generated based on build.xml. Please don't edit it.
# Author: Frank Liu <prnq63@motorola.com>
# ============================================================
LOCAL_PATH:= $(call my-dir)

#############hdpi
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := RssWidgetEx
LOCAL_CERTIFICATE := common
LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_DEX_PREOPT := false

$(shell rm -fr $(LOCAL_PATH)/res_hdpi)
$(shell mkdir -p $(LOCAL_PATH)/res_hdpi)
$(shell cp -a $(LOCAL_PATH)/res/* $(LOCAL_PATH)/res_hdpi/)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-ldpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-land-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-land-ldpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-port-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-port-ldpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/values-land-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/values-land-ldpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/values-port-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/values-port-ldpi)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res_hdpi

include $(BUILD_PACKAGE)

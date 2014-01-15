# ============================================================
# This is generated based on build.xml. Please don't edit it.
# Author: Frank Liu <prnq63@motorola.com>
# ============================================================
LOCAL_PATH:= $(call my-dir)

#############hdpi
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := ToggleWidgetsGlobal_Micro_hdpi
LOCAL_CERTIFICATE := common
LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_DEX_PREOPT := false

$(shell rm -fr $(LOCAL_PATH)/res_hdpi)
$(shell mkdir -p $(LOCAL_PATH)/res_hdpi)
$(shell cp -a $(LOCAL_PATH)/res/* $(LOCAL_PATH)/res_hdpi/)
$(shell cp -a $(LOCAL_PATH)/res/drawable/* $(LOCAL_PATH)/res_hdpi/drawable-hdpi/)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable)
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

#############mdpi
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := ToggleWidgetsGlobal_Micro_mdpi
LOCAL_CERTIFICATE := common
LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_DEX_PREOPT := false

$(shell rm -fr $(LOCAL_PATH)/res_mdpi)
$(shell mkdir -p $(LOCAL_PATH)/res_mdpi)
$(shell cp -a $(LOCAL_PATH)/res/* $(LOCAL_PATH)/res_mdpi/)
$(shell cp -a $(LOCAL_PATH)/res/drawable/* $(LOCAL_PATH)/res_mdpi/drawable-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_mdpi/drawable)
$(shell rm -fr $(LOCAL_PATH)/res_mdpi/drawable-hdpi)
$(shell rm -fr $(LOCAL_PATH)/res_mdpi/drawable-ldpi)
$(shell rm -fr $(LOCAL_PATH)/res_mdpi/drawable-land-hdpi)
$(shell rm -fr $(LOCAL_PATH)/res_mdpi/drawable-land-ldpi)
$(shell rm -fr $(LOCAL_PATH)/res_mdpi/drawable-port-hdpi)
$(shell rm -fr $(LOCAL_PATH)/res_mdpi/drawable-port-ldpi)
$(shell rm -fr $(LOCAL_PATH)/res_mdpi/values-land-hdpi)
$(shell rm -fr $(LOCAL_PATH)/res_mdpi/values-land-ldpi)
$(shell rm -fr $(LOCAL_PATH)/res_mdpi/values-port-hdpi)
$(shell rm -fr $(LOCAL_PATH)/res_mdpi/values-port-ldpi)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res_mdpi

include $(BUILD_PACKAGE)

#############ldpi
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := ToggleWidgetsGlobal_Micro_ldpi
LOCAL_CERTIFICATE := common
LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_DEX_PREOPT := false

$(shell rm -fr $(LOCAL_PATH)/res_ldpi)
$(shell mkdir -p $(LOCAL_PATH)/res_ldpi)
$(shell cp -a $(LOCAL_PATH)/res/* $(LOCAL_PATH)/res_ldpi/)
$(shell cp -a $(LOCAL_PATH)/res/drawable/* $(LOCAL_PATH)/res_ldpi/drawable-ldpi)
$(shell rm -fr $(LOCAL_PATH)/res_ldpi/drawable)
$(shell rm -fr $(LOCAL_PATH)/res_ldpi/drawable-hdpi)
$(shell rm -fr $(LOCAL_PATH)/res_ldpi/drawable-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_ldpi/drawable-land-hdpi)
$(shell rm -fr $(LOCAL_PATH)/res_ldpi/drawable-land-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_ldpi/drawable-port-hdpi)
$(shell rm -fr $(LOCAL_PATH)/res_ldpi/drawable-port-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_ldpi/values-land-hdpi)
$(shell rm -fr $(LOCAL_PATH)/res_ldpi/values-land-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_ldpi/values-port-hdpi)
$(shell rm -fr $(LOCAL_PATH)/res_ldpi/values-port-mdpi)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res_ldpi

include $(BUILD_PACKAGE)

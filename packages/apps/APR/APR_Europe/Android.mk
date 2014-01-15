LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional


LOCAL_SRC_FILES := $(call all-subdir-java-files) \
                   src/com/motorola/motoapr/service/IAPRService.aidl

LOCAL_STATIC_JAVA_LIBRARIES := \
                   com.motorola.motoapr.services/commons-net-2.0 \
                   com.motorola.motoapr.services/commons-net-ftp-2.0 

LOCAL_PACKAGE_NAME := APR_Europe

LOCAL_CERTIFICATE := platform


include $(BUILD_PACKAGE)


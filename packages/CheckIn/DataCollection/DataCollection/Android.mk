LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files) \
                   $(call all-logtags-files-under, src)
LOCAL_CERTIFICATE := platform

LOCAL_PACKAGE_NAME := DataCollection

LOCAL_DEX_PREOPT := false

LOCAL_JAVA_LIBRARIES := android.test.runner

LOCAL_STATIC_JAVA_LIBRARIES := \
    com.motorola.collectionservice

LOCAL_REQUIRED_MODULES := DeviceStatistics MotoCare

LOCAL_SRC_FILES := $(filter-out $(foreach f,$(LOCAL_SRC_FILES),$(if $(strip $(foreach b,/perfstats/ /meter/,$(if $(findstring $(b),$(f)),$(f),))),$(f),)),$(LOCAL_SRC_FILES))

 include $(BUILD_PACKAGE)
#LOCAL_MODULE := DataCollection
#LOCAL_REQUIRED_MODULES := MotoCare
#include $(BUILD_PHONY_PACKAGE)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files) $(call all-logtags-files-under, src)

LOCAL_PACKAGE_NAME := DeviceStatistics 
LOCAL_CERTIFICATE := platform
LOCAL_DEX_PREOPT := false
LOCAL_REQUIRED_MODULES := libdevicestats
#LOCAL_STATIC_JAVA_LIBRARIES := com.motorola.frameworks.core.datacollection
include $(BUILD_PACKAGE)

# Build jni
include $(CLEAR_VARS)
include $(call all-makefiles-under,$(LOCAL_PATH))

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES :=  devicestats_readEvents.cpp

# Header files path
LOCAL_C_INCLUDES += $(JNI_H_INCLUDE)

LOCAL_C_INCLUDES += $(call include-path-for, system-core)/cutils

LOCAL_SHARED_LIBRARIES +=   \
  libandroid_runtime \
  libnativehelper \
  libdl \
  liblog

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE := libdevicestats
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

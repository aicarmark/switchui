########################################################
# [QuickNote] For QuickNote
# Creator : hbg683 (Younghyung Cho)
# Main History
#  - first created : 2009. Dec. 04
#########################################################

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng

ifeq ($(TARGET_ARCH), arm)
	LOCAL_CFLAGS := -DPACKED="__attribute__ ((packed))"
else
	LOCAL_CFLAGS := -DPACKED=""
endif

LOCAL_SRC_FILES := \
	QNUtil.cpp

#2012-11-12, add by amt_sunzhao for SWITCHUITWOV-315 
#LOCAL_SHARED_LIBRARIES := \
#	libskia \
#        libbinder \
#	libandroid_runtime \
#        libsurfaceflinger_client \
#        libutils \
#	libcutils
LOCAL_SHARED_LIBRARIES := \
        libskia \
        libbinder \
        libandroid_runtime \
        libutils \
        libcutils
#2012-11-12, add end

LOCAL_LDLIBS := \
#	-lskia -lc

########################################################
# This is important!
# Shared library's default configuration is "LOCAL_PRELINK_MOUDLE". !
# If I want to set this as prelinked-module, I should modify prelink-map!
# In my opinion, for the performance issue, prelinked-module is used.
# So, in this case, I can set LOCAL_PRELINK_MODULE as 'false' explicitly!
#########################################################
LOCAL_PRELINK_MODULE := false

LOCAL_C_INCLUDES := \
	$(JNI_H_INCLUDE) \
	external/skia/include/core/ \
        external/skia/include/effects \
	external/skia/include/images \
	external/skia/src/ports \
	external/skia/include/utils \
	frameworks/base/core/jni/android/graphics/

LOCAL_MODULE:= libqnutil

include $(BUILD_SHARED_LIBRARY)

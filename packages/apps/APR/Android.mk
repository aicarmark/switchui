LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
                   com.motorola.motoapr.services/commons-net-2.0:libs/commons-net-2.0.jar \
                   com.motorola.motoapr.services/commons-net-ftp-2.0:libs/commons-net-ftp-2.0.jar 

include $(BUILD_MULTI_PREBUILT)
include $(call all-makefiles-under,$(LOCAL_PATH))


LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := WeatherEx

LOCAL_STATIC_JAVA_LIBRARIES := android-common libTaurenX

LOCAL_CERTIFICATE := shared
LOCAL_MODULE_TAGS := eng
LOCAL_DEX_PREOPT := false

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += $(call all-java-files-under, ./src_config/ironmax/)
LOCAL_SRC_FILES += $(call all-java-files-under, ./config/ironmax/Config.java)

$(shell rm -rf $(LOCAL_PATH)/src/com/motorola/mmsp/utility/Config.java)
$(shell rm -rf $(LOCAL_PATH)/res)
$(shell cp -pfr $(LOCAL_PATH)/raw/res_utility/ $(LOCAL_PATH)/res)
$(shell cp -pfr $(LOCAL_PATH)/raw/res_hdpi/* $(LOCAL_PATH)/res)
$(shell cp -pfr $(LOCAL_PATH)/res_ironmax/myres/* $(LOCAL_PATH)/res)
#$(shell cp -pfr $(LOCAL_PATH)/res_default/values* $(LOCAL_PATH)/res)

#LOCAL_STATIC_JAVA_LIBRARIES := android-common
LOCAL_CLASSPATH := $(LOCAL_PATH)/../../../platform/Goblin/out/pluginframework.jar 
#LOCAL_CLASSPATH := $(LOCAL_PATH)/../../../platform/Goblin/out/pluginframework.jar $(LOCAL_PATH)/../../../platform/Tauren/out/Tauren.jar

$(info (================ RWM748 Default hdpi =============== ) $(call all-java-files-under, ./src_config/ironmax/))
$(info $(LOCAL_SRC_FILES))


include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := eng
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libTaurenX:../../../platform/Tauren/out/Tauren.jar
include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))



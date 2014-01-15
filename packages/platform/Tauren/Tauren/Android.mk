LOCAL_PATH := $(call my-dir)


ifeq ($(update_jar),true)
    $(shell rm -rf $(LOCAL_PATH)/../out)
    $(shell mkdir $(LOCAL_PATH)/../out)
    $(shell cp -pfr $(LOCAL_PATH)/../../../../../out/target/common/obj/JAVA_LIBRARIES/Tauren_intermediates/classes.jar $(LOCAL_PATH)/../out/Tauren.jar)

else 

include $(CLEAR_VARS)

LOCAL_MODULE:= Tauren

LOCAL_MODULE_TAGS := optional

LOCAL_DEX_PREOPT := false

LOCAL_SRC_FILES := $(call all-java-files-under,src)

include $(BUILD_JAVA_LIBRARY)

endif

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

# Build all java files in the java subdirectory
LOCAL_SRC_FILES := \
    $(call all-java-files-under, src) \
    $(call all-Iaidl-files-under, src) \
    src/com/motorola/collectionservice/Callback_Client.aidl

# The name of the jar file to create
LOCAL_MODULE := com.motorola.collectionservice
LOCAL_MODULE_TAGS := optional

LOCAL_AIDL_INCLUDES += $(FRAMEWORKS_BASE_JAVA_SRC_DIRS)

LOCAL_STATIC_JAVA_LIBRARIES := \
    com.motorola.meter

# Build a static jar file.
include $(BUILD_STATIC_JAVA_LIBRARY)

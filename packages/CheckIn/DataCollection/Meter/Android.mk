LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

# Build all java files in the java subdirectory
LOCAL_SRC_FILES := \
    $(call all-java-files-under, src) \
    $(call all-Iaidl-files-under, src) \
    src/com/motorola/meter/Callback_Meter.aidl

# The name of the jar file to create
LOCAL_MODULE := com.motorola.meter
LOCAL_MODULE_TAGS := optional

LOCAL_AIDL_INCLUDES += $(FRAMEWORKS_BASE_JAVA_SRC_DIRS)

LOCAL_STATIC_JAVA_LIBRARIES := \
    gson-1.6

# Build a static jar file.
include $(BUILD_STATIC_JAVA_LIBRARY)

# Export 3rd party libraries for use by the app
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    gson-1.6:libs/google-gson-1.6/gson-1.6.jar

LOCAL_MODULE_TAGS := optional

include $(BUILD_MULTI_PREBUILT)

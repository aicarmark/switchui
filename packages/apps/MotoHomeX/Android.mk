#
# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)

##########################hdpi
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional eng

LOCAL_STATIC_JAVA_LIBRARIES := android-common android-support-v13 libGoblin

$(shell rm -fr $(LOCAL_PATH)/res_hdpi)
$(shell mkdir -p $(LOCAL_PATH)/res_hdpi)
$(shell cp -a $(LOCAL_PATH)/res/* $(LOCAL_PATH)/res_hdpi/)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-land-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-land-xhdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-xhdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-sw600dp-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-sw600dp-xhdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-sw600dp-land-hdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-sw600dp-land-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-sw720dp-land-hdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-sw720dp-land-mdpi)
$(shell rm -fr $(LOCAL_PATH)/res_hdpi/drawable-sw720dp-nodpi)

LOCAL_SRC_FILES := $(call all-java-files-under, src) $(call all-renderscript-files-under, src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res_hdpi

LOCAL_PACKAGE_NAME := MotoHomeEx
LOCAL_CERTIFICATE := shared

LOCAL_OVERRIDES_PACKAGES := Home
LOCAL_DEX_PREOPT := false

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)
#2012-11-5, add by bvq783 for plugin
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := eng
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libGoblin:jar/pluginframework.jar
include $(BUILD_MULTI_PREBUILT)
#2012-11-5, add end
include $(call all-makefiles-under,$(LOCAL_PATH))

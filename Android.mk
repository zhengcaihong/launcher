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
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := homeshell_hwdroid android-common android-support-v4 themeicon homeshell_aliyun-aml yunosui 



LOCAL_STATIC_JAVA_LIBRARIES += homeshell_utility
ifeq ($(PLATFORM_SDK_VERSION),23)
LOCAL_STATIC_JAVA_LIBRARIES += org.apache.http.legacy
endif


LOCAL_PACKAGE_NAME := AliHomeShell
LOCAL_CERTIFICATE := platform

LOCAL_OVERRIDES_PACKAGES := Home Launcher2

ifeq ($(YUNOS_SUPPORT_COLORTEST),yes)
aui_color_test_file := $(LOCAL_PATH)/aui_color_test.sh
aui_color_test_file := $(wildcard $(aui_color_test_file))
ifneq ($(aui_color_test_file),)
$(info aui_color_test_file=$(aui_color_test_file))
$(shell $(SHELL) $(aui_color_test_file) $(LOCAL_PATH))
endif
endif

hwdroid_dir := AppControls/HWDroid
utility_dir := AppControls/Utility
#use android L new feature
hwdroid_res_dir := $(hwdroid_dir)/res
ifeq "21" "$(word 1, $(sort 21 $(PLATFORM_SDK_VERSION)))"
    hwdroid_res_dir := $(hwdroid_dir)/res_5.0 $(hwdroid_dir)/res
    aui_l_feature_file := $(LOCAL_PATH)/aui_l_feature.sh
    aui_l_feature_file := $(wildcard $(aui_l_feature_file))
    ifneq ($(aui_l_feature_file),)
        $(info aui_l_feature_file=$(aui_l_feature_file))
        $(shell $(SHELL) $(aui_l_feature_file) $(LOCAL_PATH))
    endif
endif

res_dirs := res $(hwdroid_res_dir) $(utility_dir)/res

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += $(call all-renderscript-files-under, src) \
    src/com/yunos/alifinger/IEventNotifyService.aidl

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay\
    --extra-packages com.storeaui \
    --extra-packages com.hw.droid \
    --extra-packages com.aliyun.utility

LOCAL_PROGUARD_ENABLED := custom
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
ifeq "21" "$(word 1, $(sort 21 $(PLATFORM_SDK_VERSION)))"
    LOCAL_PROGUARD_ENABLED := disabled
endif
LOCAL_STATIC_JAVA_LIBRARIES += fingerprint

ifneq ($(YUNOS_SUPPORT_MULTIUSERAPPS),yes)
	LOCAL_STATIC_JAVA_LIBRARIES += multi_user
endif
LOCAL_JNI_SHARED_LIBRARIES := libicongenerator
ifeq ($(strip $(YUNOS_SUPPORT_PICK)), yes)
LOCAL_MULTILIB := both
endif

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
ifeq ($(strip $(YUNOS_SUPPORT_CTA)), yes)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES :=  themeicon:libs/iconcache-jar.jar \
                                        homeshell_aliyun-aml:libs/aliyun-aml_cta.jar
else
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES :=  themeicon:libs/iconcache-jar.jar \
                                        homeshell_aliyun-aml:libs/aliyun-aml.jar
endif                             
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += fingerprint:libs/fingerprint.jar
ifneq ($(YUNOS_SUPPORT_MULTIUSERAPPS),yes)
	LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += multi_user:libs/multi_user.jar
endif


include $(BUILD_MULTI_PREBUILT)
include $(call all-makefiles-under,$(LOCAL_PATH))

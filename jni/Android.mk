LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libicongenerator
LOCAL_SRC_FILES := com_aliyun_icon_IconGenerator.cpp IconGenerator.cpp LinearAlgebraic.cpp QuadraticCurveFitting.cpp com_aliyun_icon_ImageUtils.cpp
#LOCAL_LDLIBS    := -llog -ljnigraphics
LOCAL_SHARED_LIBRARIES += \
liblog libjnigraphics

include $(BUILD_SHARED_LIBRARY)

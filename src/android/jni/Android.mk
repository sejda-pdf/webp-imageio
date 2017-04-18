MY_LOCAL_PATH := $(call my-dir)

LIBWEBP_PATH := $(MY_LOCAL_PATH)/../../../libwebp

include $(LIBWEBP_PATH)/Android.mk

include $(CLEAR_VARS)

LOCAL_PATH := $(MY_LOCAL_PATH)

LOCAL_MODULE    := webp-imageio
LOCAL_SRC_FILES := ../../main/c/webp-imageio.c
LOCAL_CPPFLAGS  := -std=c11
LOCAL_C_INCLUDES := $(JNI_SRC_PATH) $(LIBWEBP_PATH)/src
LOCAL_STATIC_LIBRARIES := webp

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/native_app_glue)
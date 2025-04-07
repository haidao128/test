LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := seccomp_filter
LOCAL_SRC_FILES := seccomp_filter.cpp
LOCAL_LDLIBS    := -llog

include $(BUILD_SHARED_LIBRARY) 
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_CFLAGS := -Werror -Wall -Wno-missing-field-initializers -Wno-unused-variable -Wno-unused-parameter

LOCAL_LDLIBS := -llog

LOCAL_CONLYFLAGS := -std=c11

LOCAL_SRC_FILES:= arielfw.cpp \
                  CommandListener.cpp \
                  ArielfwCommand.cpp \
                  ResponseCode.cpp \
                  FirewallManager.cpp

LOCAL_SHARED_LIBRARIES := liblog \
                          libsysutils \
                          libbase

LOCAL_CLANG := true

LOCAL_MODULE:= arielfw

#LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
#LOCAL_MODULE_TAGS := debug

include $(BUILD_EXECUTABLE)

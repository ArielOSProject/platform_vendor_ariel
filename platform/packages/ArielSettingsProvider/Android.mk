LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

src_dir := src
res_dir := res

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dir))
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dir))

LOCAL_PACKAGE_NAME := ArielSettingsProvider
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_STATIC_JAVA_LIBRARIES := \
    com.ariel.platform.internal

include $(BUILD_PACKAGE)

########################
include $(call all-makefiles-under,$(LOCAL_PATH))

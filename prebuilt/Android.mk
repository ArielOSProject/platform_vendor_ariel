# copy prebuild files to their locations

# to avoid forking system/core just to replace hosts file
# we are manually going to copy our custom hosts file
# to its correct location
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
$(shell mkdir -p $(TARGET_OUT)/etc/)
$(shell cp -rf $(LOCAL_PATH)/common/etc/hosts `pwd`/$(TARGET_OUT)/etc/)
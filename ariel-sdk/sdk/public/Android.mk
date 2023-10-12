LOCAL_PATH := $(call my-dir)

# the sdk as an aar for publish, not built as part of full target
# DO NOT LINK AGAINST THIS IN BUILD
# ============================================================
include $(CLEAR_VARS)

LOCAL_MODULE := com.arielos.platform.sdk.aar

LOCAL_JACK_ENABLED := disabled

# setting min SDK to 28, so NEON apps can still compile on AOSP 9
LOCAL_MIN_SDK_VERSION := 28

# just need to define this, $(TOP)/dummy should not exist
# LOCAL_SRC_FILES := $(call all-java-files-under, dummy)
LOCAL_CONSUMER_PROGUARD_FILE := $(LOCAL_PATH)/proguard.txt

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, res/res)
LOCAL_MANIFEST_FILE := AndroidManifest.xml


LOCAL_STATIC_JAVA_LIBRARIES := com.arielos.platform.sdk

include $(BUILD_STATIC_JAVA_LIBRARY)
$(LOCAL_MODULE) : $(built_aar)

# $(LOCAL_MODULE) : $(built_aar) \
#      $(shell cp -rf $(built_aar) `pwd`/$(PRODUCT_OUT)/$(LOCAL_MODULE))


# ===========================================================
# Neon SDK docs

include $(CLEAR_VARS)

# disable docs for now, compile issues for DroidDoc being obsolete
# neon_platform_docs_src_files := \
#     $(call all-java-files-under, $(neon_sdk_src))

# neon_platform_docs_LOCAL_ADDITIONAL_JAVA_DIR := \
#     $(call intermediates-dir-for,JAVA_LIBRARIES,com.navico.neon.platform.sdk,,COMMON)

# neon_framework_built := $(call java-lib-deps, com.navico.neon.platform)

# neon_platform_docs_java_libraries := \
#     android-support-v4 \
#     com.navico.neon.platform.sdk

# LOCAL_MODULE := com.navico.neon.platform.sdk
# LOCAL_INTERMEDIATE_SOURCES:= $(neon_platform_LOCAL_INTERMEDIATE_SOURCES)
# LOCAL_MODULE_CLASS := JAVA_LIBRARIES
# LOCAL_MODULE_TAGS := optional

# LOCAL_SRC_FILES := $(neon_platform_docs_src_files)
# LOCAL_ADDITONAL_JAVA_DIR := $(neon_platform_docs_LOCAL_ADDITIONAL_JAVA_DIR)

# LOCAL_IS_HOST_MODULE := false
# LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR := external/doclava/res/assets/templates-sdk

# LOCAL_ADDITIONAL_DEPENDENCIES := \
#     services

# LOCAL_JAVA_LIBRARIES := $(neon_platform_docs_java_libraries)

# LOCAL_DROIDDOC_OPTIONS := \
#         -android \
#         -offlinemode \
#         -exclude com.navico.neon.platform.internal \
#         -exclude com.navico.neon.platform.internal.common \
#         -hidePackage com.navico.neon.platform.internal \
#         -hidePackage com.navico.neon.platform.internal.common \
#         -hdf android.whichdoc offline \
#         -hdf sdk.preview 0

# $(full_target): $(neon_framework_built) $(gen)
# include $(BUILD_DROIDDOC)

# include $(call first-makefiles-under,$(LOCAL_PATH))
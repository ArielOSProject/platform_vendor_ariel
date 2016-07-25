# Copyright (C) 2015 The CyanogenMod Project
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
LOCAL_PATH := $(call my-dir)

# We have a special case here where we build the library's resources
# independently from its code, so we need to find where the resource
# class source got placed in the course of building the resources.
# Thus, the magic here.
# Also, this module cannot depend directly on the R.java file; if it
# did, the PRIVATE_* vars for R.java wouldn't be guaranteed to be correct.
# Instead, it depends on the R.stamp file, which lists the corresponding
# R.java file as a prerequisite.
ariel_platform_res := APPS/com.ariel.platform-res_intermediates/src

# List of packages used in cm-api-stubs and cm-system-api-stubs
ariel_stub_packages := ariel.providers

# The CyanogenMod Platform Framework Library
# ============================================================
include $(CLEAR_VARS)

ariel_sdk_src := sdk/src/java/ariel

LOCAL_MODULE := com.ariel.platform
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
    $(call all-java-files-under, $(ariel_sdk_src))

arielplat_LOCAL_INTERMEDIATE_SOURCES := \
    $(ariel_platform_res)/ariel/platform/R.java \
    $(ariel_platform_res)/ariel/platform/Manifest.java

LOCAL_INTERMEDIATE_SOURCES := \
    $(arielplat_LOCAL_INTERMEDIATE_SOURCES)


include $(BUILD_JAVA_LIBRARY)
ariel_framework_module := $(LOCAL_INSTALLED_MODULE)

# Make sure that R.java and Manifest.java are built before we build
# the source for this library.
cm_framework_res_R_stamp := \
    $(call intermediates-dir-for,APPS,com.ariel.platform-res,,COMMON)/src/R.stamp
$(full_classes_compiled_jar): $(cm_framework_res_R_stamp)
$(built_dex_intermediate): $(cm_framework_res_R_stamp)

$(ariel_framework_module): | $(dir $(ariel_framework_module))com.ariel.platform-res.apk

cm_framework_built := $(call java-lib-deps, com.ariel.platform)

# ====  com.ariel.platform.xml lib def  ========================
include $(CLEAR_VARS)

LOCAL_MODULE := com.ariel.platform.xml
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_CLASS := ETC

# This will install the file in /system/etc/permissions
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions

LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)

# the sdk
# ============================================================
include $(CLEAR_VARS)

LOCAL_MODULE:= com.ariel.platform.sdk
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
    $(call all-java-files-under, $(ariel_sdk_src))

cmsdk_LOCAL_INTERMEDIATE_SOURCES := \
    $(ariel_platform_res)/ariel/platform/R.java \
    $(ariel_platform_res)/ariel/platform/Manifest.java

LOCAL_INTERMEDIATE_SOURCES := \
    $(cmsdk_LOCAL_INTERMEDIATE_SOURCES)

# Make sure that R.java and Manifest.java are built before we build
# the source for this library.
cm_framework_res_R_stamp := \
    $(call intermediates-dir-for,APPS,com.ariel.platform-res,,COMMON)/src/R.stamp
$(full_classes_compiled_jar): $(cm_framework_res_R_stamp)
$(built_dex_intermediate): $(cm_framework_res_R_stamp)
$(full_target): $(cm_framework_built) $(gen)

include $(BUILD_STATIC_JAVA_LIBRARY)

# the sdk as an aar for publish, not built as part of full target
# DO NOT LINK AGAINST THIS IN BUILD
# ============================================================
include $(CLEAR_VARS)

LOCAL_MODULE := com.ariel.platform.sdk.aar

LOCAL_JACK_ENABLED := disabled

# just need to define this, $(TOP)/dummy should not exist
LOCAL_SRC_FILES := $(call all-java-files-under, dummy)
#LOCAL_CONSUMER_PROGUARD_FILE := $(LOCAL_PATH)/sdk/proguard.txt

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, sdk/res/res)
LOCAL_MANIFEST_FILE := sdk/AndroidManifest.xml

cmsdk_exclude_files := 'ariel/library'
LOCAL_JAR_EXCLUDE_PACKAGES := $(cmsdk_exclude_files)
LOCAL_JAR_EXCLUDE_FILES := none

LOCAL_STATIC_JAVA_LIBRARIES := com.ariel.platform.sdk

include $(BUILD_STATIC_JAVA_LIBRARY)
$(LOCAL_MODULE) : $(built_aar)

# full target for use by platform apps
#
include $(CLEAR_VARS)

LOCAL_MODULE:= com.ariel.platform.internal
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
    $(call all-java-files-under, $(ariel_sdk_src))

cmsdk_LOCAL_INTERMEDIATE_SOURCES := \
    $(ariel_platform_res)/ariel/platform/R.java \
    $(ariel_platform_res)/ariel/platform/Manifest.java

LOCAL_INTERMEDIATE_SOURCES := \
    $(cmsdk_LOCAL_INTERMEDIATE_SOURCES)

$(full_target): $(cm_framework_built) $(gen)

include $(BUILD_STATIC_JAVA_LIBRARY)

include $(call first-makefiles-under,$(LOCAL_PATH))
#include $(call all-makefiles-under, $(LOCAL_PATH))

# Cleanup temp vars
# ===========================================================
cmplat.docs.src_files :=
cmplat.docs.java_libraries :=
intermediates.COMMON :=

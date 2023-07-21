# Copyright (C) 2021-2023 The LineageOS Project
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

# Inherit some common stuff.
# We inherit Ariel first to be able to override some
# AOSP default params (like PRODUCT_COPY_FILES)
$(call inherit-product, vendor/ariel/config/ariel_common.mk)

$(call inherit-product, vendor/lineage/build/target/product/lineage_sdk_phone_x86_64.mk)

# Overrides
PRODUCT_NAME := ariel_sdk_phone_x86_64
PRODUCT_MODEL := ArielOS Android SDK built for x86_64

PRODUCT_SDK_ADDON_NAME := ariel
#PRODUCT_SDK_ADDON_SYS_IMG_SOURCE_PROP := $(LOCAL_PATH)/source.properties

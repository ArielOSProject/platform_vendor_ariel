# PRODUCT_PACKAGES += \
#     bootanimation_ariel.zip

PRODUCT_COPY_FILES += \
    vendor/ariel/prebuilt/common/etc/default-permissions/ariel-permissions.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/default-permissions/ariel-permissions.xml \
    vendor/ariel/prebuilt/common/etc/permissions/privapp-permissions-ariel.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/privapp-permissions-ariel.xml \
    vendor/ariel/prebuilt/common/etc/permissions/com.arielos.android.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/com.arielos.android.xml \
    vendor/ariel/prebuilt/common/etc/permissions/com.arielos.security.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/com.arielos.security.xml \
    vendor/ariel/prebuilt/common/etc/sysconfig/ariel-sysconfig.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/sysconfig/ariel-sysconfig.xml \
    vendor/ariel/prebuilt/common/etc/hosts:$(TARGET_COPY_OUT_SYSTEM)/etc/hosts \
    vendor/ariel/prebuilt/common/etc/microg.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/microg.xml \
    vendor/ariel/prebuilt/common/etc/gps.conf:$(TARGET_COPY_OUT_SYSTEM)/etc/gps.conf \
    vendor/ariel/prebuilt/bootanimation_ariel.zip:${TARGET_COPY_OUT_PRODUCT}/media/bootanimation.zip


#    vendor/ariel/prebuilt/common/etc/permissions/com.arielos.firewall.xml:system/etc/permissions/com.arielos.firewall.xml \

# Copy .rc files
PRODUCT_COPY_FILES += \
    vendor/ariel/prebuilt/common/bin/backuptool.sh:install/bin/backuptool.sh \
    vendor/ariel/prebuilt/common/bin/backuptool.functions:install/bin/backuptool.functions \
    vendor/ariel/prebuilt/common/etc/init/init.ariel.rc:system/etc/init/init.ariel.rc

# Backup Tool
# ArielOS: overwriting LineageOS script which performs a backup of the hosts file.
# We need to be able to provide a new hosts via OTA so this is the reason why we
# are replacing the original script. Until we figure out a better way to do it,
# it stays like this.
PRODUCT_COPY_FILES += \
    vendor/ariel/prebuilt/common/bin/50-lineage.sh:$(TARGET_COPY_OUT_SYSTEM)/addon.d/50-lineage.sh

# Make sure data roaming is off!
# PRODUCT_PROPERTY_OVERRIDES += \
#     ro.com.android.dataroaming=false

# use this for user builds:
# ro.control_privapp_permissions=enforce
# PRODUCT_PROPERTY_OVERRIDES += \
#     ro.control_privapp_permission=log

# REMOVE FROM RELEASE BULDS, ALLOWS
# LOGCAT TO START FROM BEGINNING!!!!!!!!!!
# PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
#    ro.adb.secure=0 \
#    ro.debuggable=1 \
#    ro.secure=0 \
#    persist.sys.usb.config=adb

# Bootanimation
#PRODUCT_PACKAGES += \
#    bootanimation.zip

# Copy IntentFirewall configuration file
#PRODUCT_COPY_FILES += \
#    vendor/ariel/firewall/ifw.config:data/system/ifw/ifw.config

# GAPPS_VARIANT := mini
# $(call inherit-product, vendor/google/build/opengapps-packages.mk)

# Ariel Platform Library
PRODUCT_PACKAGES += \
    com.arielos.platform-res \
    com.arielos.platform \
    com.arielos.platform.xml

PRODUCT_PACKAGES += \
    ArielSettingsProvider \
    arielsettings

include vendor/ariel/sepolicy/sepolicy.mk

include vendor/ariel/config/ariel_packages.mk

PRODUCT_PACKAGE_OVERLAYS += vendor/ariel/overlay/common/

-include vendor/ariel-priv/keys.mk

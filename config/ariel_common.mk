

PRODUCT_COPY_FILES += \
    vendor/ariel/prebuilt/common/etc/default-permissions/ariel-permissions.xml:system/etc/default-permissions/ariel-permissions.xml \
    vendor/ariel/prebuilt/common/etc/permissions/privapp-permissions-ariel.xml:system/etc/permissions/privapp-permissions-ariel.xml \
    vendor/ariel/prebuilt/common/etc/permissions/com.arielos.android.xml:system/etc/permissions/com.arielos.android.xml \
    vendor/ariel/prebuilt/common/etc/permissions/com.arielos.security.xml:system/etc/permissions/com.arielos.security.xml \
    vendor/ariel/prebuilt/common/etc/sysconfig/ariel-sysconfig.xml:system/etc/sysconfig/ariel-sysconfig.xml \
    vendor/ariel/prebuilt/common/etc/hosts:system/etc/hosts \
    vendor/ariel/prebuilt/common/etc/microg.xml:system/etc/microg.xml \
    vendor/ariel/prebuilt/bootanimation_ariel.zip:${TARGET_COPY_OUT_PRODUCT}/media/bootanimation.zip


#    vendor/ariel/prebuilt/common/etc/permissions/com.arielos.firewall.xml:system/etc/permissions/com.arielos.firewall.xml \

# Copy .rc files
PRODUCT_COPY_FILES += \
    vendor/ariel/prebuilt/common/etc/init/init.ariel.rc:system/etc/init/init.ariel.rc

# Make sure data roaming is off!
# PRODUCT_PROPERTY_OVERRIDES += \
#     ro.com.android.dataroaming=false

# use this for user builds:
# ro.control_privapp_permissions=enforce
# PRODUCT_PROPERTY_OVERRIDES += \
#     ro.control_privapp_permission=log

# REMOVE FROM RELEASE BULDS, ALLOWS
# LOGCAT TO START FROM BEGINNING!!!!!!!!!!
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
   ro.adb.secure=0 \
   ro.debuggable=1 \
   ro.secure=0 \
   persist.sys.usb.config=adb

# Bootanimation
#PRODUCT_PACKAGES += \
#    bootanimation.zip

# Copy IntentFirewall configuration file
#PRODUCT_COPY_FILES += \
#    vendor/ariel/firewall/ifw.config:data/system/ifw/ifw.config

# GAPPS_VARIANT := mini
# $(call inherit-product, vendor/google/build/opengapps-packages.mk)

include vendor/ariel/sepolicy/sepolicy.mk

include vendor/ariel/config/ariel_packages.mk

PRODUCT_PACKAGE_OVERLAYS += vendor/ariel/overlay/common/

-include vendor/ariel-priv/keys.mk

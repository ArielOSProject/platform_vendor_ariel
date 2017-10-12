
include vendor/ariel/sepolicy/sepolicy.mk

PRODUCT_PACKAGE_OVERLAYS :=  vendor/ariel/overlay $(PRODUCT_PACKAGE_OVERLAYS)

# Ariel Platform Library
PRODUCT_PACKAGES += \
    com.ariel.platform-res \
    com.ariel.platform \
    com.ariel.platform.xml \
    com.ariel.hardware \
    com.ariel.hardware.xml

PRODUCT_COPY_FILES += \
    vendor/ariel/system/etc/default-permissions/ariel-permissions.xml:system/etc/default-permissions/ariel-permissions.xml \
    vendor/ariel/system/etc/permissions/com.ariel.android.xml:system/etc/permissions/com.ariel.android.xml \
    vendor/ariel/system/etc/permissions/com.ariel.firewall.xml:system/etc/permissions/com.ariel.firewall.xml \
    vendor/ariel/system/etc/permissions/com.ariel.security.xml:system/etc/permissions/com.ariel.security.xml \
    vendor/ariel/system/etc/permissions/privapp-permissions-ariel.xml:system/etc/permissions/privapp-permissions-ariel.xml \
    vendor/ariel/system/etc/sysconfig/ariel.xml:system/etc/sysconfig/ariel.xml

PRODUCT_PACKAGES += \
   ArielGuardian \
   ArielSettingsProvider \
   ArielSetupWizard \
   arielfw

# Make sure data roaming is off!
PRODUCT_PROPERTY_OVERRIDES += \
    ro.com.android.dataroaming=false

PRODUCT_PROPERTY_OVERRIDES += \
    ro.control_privapp_permission=log

# Copy IntentFirewall configuration file
#PRODUCT_COPY_FILES += \
#    vendor/ariel/firewall/ifw.config:data/system/ifw/ifw.config

# Copy .rc files
PRODUCT_COPY_FILES += \
    vendor/ariel/root/init.ariel.rc:root/init.ariel.rc

include $(call first-makefiles-under,$(LOCAL_PATH))

# mozda probati i sa include <Putanja do mk fajla>



PRODUCT_COPY_FILES += \
    vendor/ariel/prebuilt/common/etc/default-permissions/ariel-permissions.xml:system/etc/default-permissions/ariel-permissions.xml \
    vendor/ariel/prebuilt/common/etc/permissions/privapp-permissions-ariel.xml:system/etc/permissions/privapp-permissions-ariel.xml \
    vendor/ariel/prebuilt/common/etc/sysconfig/ariel.xml:system/etc/sysconfig/ariel.xml \
    vendor/ariel/prebuilt/common/etc/init.d/00banner:system/etc/init.d/00banner \
    vendor/ariel/system/etc/hosts.ariel:system/etc/hosts.ariel


# Copy .rc files
PRODUCT_COPY_FILES += \
    vendor/ariel/prebuilt/common/etc/init/init.ariel.rc:system/etc/init/init.ariel.rc

# Make sure data roaming is off!
PRODUCT_PROPERTY_OVERRIDES += \
    ro.com.android.dataroaming=false

PRODUCT_PROPERTY_OVERRIDES += \
    ro.control_privapp_permission=log

# Copy IntentFirewall configuration file
#PRODUCT_COPY_FILES += \
#    vendor/ariel/firewall/ifw.config:data/system/ifw/ifw.config


include vendor/ariel/sepolicy/sepolicy.mk

include vendor/ariel/config/ariel_packages.mk

PRODUCT_PACKAGE_OVERLAYS :=  vendor/ariel/overlay $(PRODUCT_PACKAGE_OVERLAYS)

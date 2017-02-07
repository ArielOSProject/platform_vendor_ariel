
include vendor/ariel/sepolicy/sepolicy.mk

# Ariel Platform Library
PRODUCT_PACKAGES += \
    com.ariel.platform-res \
    com.ariel.platform \
    com.ariel.platform.xml

PRODUCT_COPY_FILES += \
    vendor/ariel/config/permissions/com.ariel.android.xml:system/etc/permissions/com.ariel.android.xml \
    vendor/ariel/config/permissions/com.ariel.firewall.xml:system/etc/permissions/com.ariel.firewall.xml

#Eleven
PRODUCT_PACKAGES += \
   ArielGuardian \
   CMFileManager \
   ArielSettingsProvider \
   arielfw

# Make sure data roaming is off!
PRODUCT_PROPERTY_OVERRIDES += \
    ro.com.android.dataroaming=false

# Copy IntentFirewall configuration file
#PRODUCT_COPY_FILES += \
#    vendor/ariel/firewall/ifw.config:data/system/ifw/ifw.config

# Copy .rc files
PRODUCT_COPY_FILES += \
    vendor/ariel/init.ariel.rc:root/init.ariel.rc

include $(call first-makefiles-under,$(LOCAL_PATH))

# mozda probati i sa include <Putanja do mk fajla>


$(call inherit-product, vendor/ariel/ariel-sdk.mk)
$(call inherit-product, vendor/ariel/sepolicy/sepolicy.mk)

PRODUCT_PACKAGES += \
   ArielGuardian \
#   Eleven \
   CMFileManager \
   ArielSettingsProvider

# Make sure data roaming is off!
PRODUCT_PROPERTY_OVERRIDES += \
    ro.com.android.dataroaming=false

# Copy IntentFirewall configuration file
#PRODUCT_COPY_FILES += \
#    vendor/ariel/firewall/ifw.config:data/system/ifw/ifw.config

# Copy .rc files
PRODUCT_COPY_FILES += \
    vendor/ariel/init.ariel.rc:root/init.ariel.rc


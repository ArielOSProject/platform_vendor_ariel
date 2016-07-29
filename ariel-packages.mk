$(call inherit-product, vendor/ariel/ariel-sdk.mk)

PRODUCT_PACKAGES += \
   ArielGuardian \
   Eleven \
   CMFileManager \
   ArielSettingsProvider

# Make sure data roaming is off!
PRODUCT_PROPERTY_OVERRIDES += \
    ro.com.android.dataroaming=false


# Inherit full common Lineage stuff
$(call inherit-product, vendor/ariel/config/common_full.mk)

# Required packages
PRODUCT_PACKAGES += \
    LatinIME

# Include Lineage LatinIME dictionaries
PRODUCT_PACKAGE_OVERLAYS += vendor/ariel/overlay/dictionaries

$(call inherit-product, vendor/ariel/config/telephony.mk)

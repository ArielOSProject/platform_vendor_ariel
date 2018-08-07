# Inherit full common Lineage stuff
$(call inherit-product, vendor/ariel/config/common_full.mk)

PRODUCT_PACKAGES += AppDrawer

DEVICE_PACKAGE_OVERLAYS += vendor/ariel/overlay/tv

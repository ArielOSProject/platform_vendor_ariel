PRODUCT_BRAND ?= ArielOS

PRODUCT_BUILD_PROP_OVERRIDES += BUILD_UTC_DATE=0

ifeq ($(PRODUCT_GMS_CLIENTID_BASE),)
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    ro.com.google.clientidbase=android-google
else
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    ro.com.google.clientidbase=$(PRODUCT_GMS_CLIENTID_BASE)
endif

PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    keyguard.no_require_sim=true

PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    ro.build.selinux=1

# Default notification/alarm sounds
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    ro.config.notification_sound=Argon.ogg \
    ro.config.alarm_alert=Hassium.ogg

ifneq ($(TARGET_BUILD_VARIANT),user)
# Thank you, please drive thru!
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += persist.sys.dun.override=0
endif

ifneq ($(TARGET_BUILD_VARIANT),eng)
# Enable ADB authentication
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += ro.adb.secure=1
endif

ifeq ($(BOARD_CACHEIMAGE_FILE_SYSTEM_TYPE),)
  PRODUCT_DEFAULT_PROPERTY_OVERRIDES += \
    ro.device.cache_dir=/data/cache
else
  PRODUCT_DEFAULT_PROPERTY_OVERRIDES += \
    ro.device.cache_dir=/cache
endif

# Backup Tool
PRODUCT_COPY_FILES += \
    vendor/ariel/prebuilt/common/bin/backuptool.sh:install/bin/backuptool.sh \
    vendor/ariel/prebuilt/common/bin/backuptool.functions:install/bin/backuptool.functions \
    vendor/ariel/prebuilt/common/bin/50-lineage.sh:system/addon.d/50-lineage.sh \
    vendor/ariel/prebuilt/common/bin/blacklist:system/addon.d/blacklist

ifeq ($(AB_OTA_UPDATER),true)
PRODUCT_COPY_FILES += \
    vendor/ariel/prebuilt/common/bin/backuptool_ab.sh:system/bin/backuptool_ab.sh \
    vendor/ariel/prebuilt/common/bin/backuptool_ab.functions:system/bin/backuptool_ab.functions \
    vendor/ariel/prebuilt/common/bin/backuptool_postinstall.sh:system/bin/backuptool_postinstall.sh
endif

# Backup Services whitelist
PRODUCT_COPY_FILES += \
    vendor/ariel/config/permissions/backup.xml:system/etc/sysconfig/backup.xml

# Lineage-specific broadcast actions whitelist
PRODUCT_COPY_FILES += \
    vendor/ariel/config/permissions/lineage-sysconfig.xml:system/etc/sysconfig/lineage-sysconfig.xml

# Signature compatibility validation
PRODUCT_COPY_FILES += \
    vendor/ariel/prebuilt/common/bin/otasigcheck.sh:install/bin/otasigcheck.sh

# init.d support
PRODUCT_COPY_FILES += \
    vendor/ariel/prebuilt/common/etc/init.d/00banner:system/etc/init.d/00banner \
    vendor/ariel/prebuilt/common/bin/sysinit:system/bin/sysinit

ifneq ($(TARGET_BUILD_VARIANT),user)
# userinit support
PRODUCT_COPY_FILES += \
    vendor/ariel/prebuilt/common/etc/init.d/90userinit:system/etc/init.d/90userinit
endif

# Copy all Lineage-specific init rc files
$(foreach f,$(wildcard vendor/ariel/prebuilt/common/etc/init/*.rc),\
	$(eval PRODUCT_COPY_FILES += $(f):system/etc/init/$(notdir $f)))

# Copy over added mimetype supported in libcore.net.MimeUtils
PRODUCT_COPY_FILES += \
    vendor/ariel/prebuilt/common/lib/content-types.properties:system/lib/content-types.properties

# Enable SIP+VoIP on all targets
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.software.sip.voip.xml:system/etc/permissions/android.software.sip.voip.xml

# Enable wireless Xbox 360 controller support
PRODUCT_COPY_FILES += \
    frameworks/base/data/keyboards/Vendor_045e_Product_028e.kl:system/usr/keylayout/Vendor_045e_Product_0719.kl

# This is Lineage!
PRODUCT_COPY_FILES += \
    vendor/ariel/config/permissions/org.lineageos.android.xml:system/etc/permissions/org.lineageos.android.xml \
    vendor/ariel/config/permissions/privapp-permissions-lineage.xml:system/etc/permissions/privapp-permissions-lineage.xml

# Include Lineage audio files
include vendor/ariel/config/lineage_audio.mk

ifneq ($(TARGET_DISABLE_LINEAGE_SDK), true)
# Lineage SDK
include vendor/ariel/config/lineage_sdk_common.mk
endif

# TWRP
ifeq ($(WITH_TWRP),true)
include vendor/ariel/config/twrp.mk
endif

# Bootanimation
PRODUCT_PACKAGES += \
    bootanimation.zip

# Required Lineage packages
PRODUCT_PACKAGES += \
    BluetoothExt \
    LineageParts \
    Development \
    Profiles

# Optional packages
PRODUCT_PACKAGES += \
    libemoji \
    LiveWallpapersPicker \
    PhotoTable \
    Terminal

# Include explicitly to work around GMS issues
PRODUCT_PACKAGES += \
    libprotobuf-cpp-full \
    librsjni

# Custom Lineage packages
PRODUCT_PACKAGES += \
    AudioFX \
    LineageSettingsProvider \
    LineageSetupWizard \
    Eleven \
    ExactCalculator \
    Jelly \
    LockClock \
    Trebuchet \
    Updater \
    WallpaperPicker \
    WeatherProvider

# Exchange support
PRODUCT_PACKAGES += \
    Exchange2

# Berry styles
PRODUCT_PACKAGES += \
    LineageDarkTheme \
    LineageBlackAccent \
    LineageBlueAccent \
    LineageBrownAccent \
    LineageCyanAccent \
    LineageGreenAccent \
    LineageOrangeAccent \
    LineagePinkAccent \
    LineagePurpleAccent \
    LineageRedAccent \
    LineageYellowAccent

# Extra tools in Lineage
PRODUCT_PACKAGES += \
    7z \
    awk \
    bash \
    bzip2 \
    curl \
    fsck.ntfs \
    gdbserver \
    htop \
    lib7z \
    libsepol \
    micro_bench \
    mke2fs \
    mkfs.ntfs \
    mount.ntfs \
    oprofiled \
    pigz \
    powertop \
    sqlite3 \
    strace \
    tune2fs \
    unrar \
    unzip \
    vim \
    wget \
    zip

# Charger
PRODUCT_PACKAGES += \
    charger_res_images

# Custom off-mode charger
ifeq ($(WITH_LINEAGE_CHARGER),true)
PRODUCT_PACKAGES += \
    lineage_charger_res_images \
    font_log.png \
    libhealthd.lineage
endif

# exFAT tools
PRODUCT_PACKAGES += \
    fsck.exfat \
    mkfs.exfat

# Openssh
PRODUCT_PACKAGES += \
    scp \
    sftp \
    ssh \
    sshd \
    sshd_config \
    ssh-keygen \
    start-ssh

# rsync
PRODUCT_PACKAGES += \
    rsync

# Storage manager
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    ro.storage_manager.enabled=true

# Media
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    media.recorder.show_manufacturer_and_model=true

# These packages are excluded from user builds
ifneq ($(TARGET_BUILD_VARIANT),user)
PRODUCT_PACKAGES += \
    procmem \
    procrank

# Conditionally build in su
ifeq ($(WITH_SU),true)
PRODUCT_PACKAGES += \
    su
endif
endif

DEVICE_PACKAGE_OVERLAYS += vendor/ariel/overlay/common

PRODUCT_VERSION_MAJOR = 15
PRODUCT_VERSION_MINOR = 1
PRODUCT_VERSION_MAINTENANCE := 0

ifeq ($(TARGET_VENDOR_SHOW_MAINTENANCE_VERSION),true)
    LINEAGE_VERSION_MAINTENANCE := $(PRODUCT_VERSION_MAINTENANCE)
else
    LINEAGE_VERSION_MAINTENANCE := 0
endif

# Set LINEAGE_BUILDTYPE from the env RELEASE_TYPE, for jenkins compat

ifndef ARIEL_BUILDTYPE
    ifdef RELEASE_TYPE
        # Starting with "LINEAGE_" is optional
        RELEASE_TYPE := $(shell echo $(RELEASE_TYPE) | sed -e 's|^ARIEL_||g')
        ARIEL_BUILDTYPE := $(RELEASE_TYPE)
    endif
endif

# Filter out random types, so it'll reset to UNOFFICIAL
ifeq ($(filter RELEASE NIGHTLY SNAPSHOT EXPERIMENTAL,$(ARIEL_BUILDTYPE)),)
    ARIEL_BUILDTYPE :=
endif

ifdef ARIEL_BUILDTYPE
    ifneq ($(ARIEL_BUILDTYPE), SNAPSHOT)
        ifdef LINEAGE_EXTRAVERSION
            # Force build type to EXPERIMENTAL
            ARIEL_BUILDTYPE := EXPERIMENTAL
            # Remove leading dash from LINEAGE_EXTRAVERSION
            LINEAGE_EXTRAVERSION := $(shell echo $(LINEAGE_EXTRAVERSION) | sed 's/-//')
            # Add leading dash to LINEAGE_EXTRAVERSION
            LINEAGE_EXTRAVERSION := -$(LINEAGE_EXTRAVERSION)
        endif
    else
        ifndef LINEAGE_EXTRAVERSION
            # Force build type to EXPERIMENTAL, SNAPSHOT mandates a tag
            ARIEL_BUILDTYPE := EXPERIMENTAL
        else
            # Remove leading dash from LINEAGE_EXTRAVERSION
            LINEAGE_EXTRAVERSION := $(shell echo $(LINEAGE_EXTRAVERSION) | sed 's/-//')
            # Add leading dash to LINEAGE_EXTRAVERSION
            LINEAGE_EXTRAVERSION := -$(LINEAGE_EXTRAVERSION)
        endif
    endif
else
    # If LINEAGE_BUILDTYPE is not defined, set to UNOFFICIAL
    ARIEL_BUILDTYPE := UNOFFICIAL
    LINEAGE_EXTRAVERSION :=
endif

ifeq ($(ARIEL_BUILDTYPE), UNOFFICIAL)
    ifneq ($(TARGET_UNOFFICIAL_BUILD_ID),)
        LINEAGE_EXTRAVERSION := -$(TARGET_UNOFFICIAL_BUILD_ID)
    endif
endif

ifeq ($(ARIEL_BUILDTYPE), RELEASE)
    ifndef TARGET_VENDOR_RELEASE_BUILD_ID
        ARIEL_VERSION := $(PRODUCT_VERSION_MAJOR).$(PRODUCT_VERSION_MINOR).$(PRODUCT_VERSION_MAINTENANCE)$(PRODUCT_VERSION_DEVICE_SPECIFIC)-$(ARIEL_BUILD)
    else
        ifeq ($(TARGET_BUILD_VARIANT),user)
            ifeq ($(LINEAGE_VERSION_MAINTENANCE),0)
                ARIEL_VERSION := $(PRODUCT_VERSION_MAJOR).$(PRODUCT_VERSION_MINOR)-$(TARGET_VENDOR_RELEASE_BUILD_ID)-$(ARIEL_BUILD)
            else
                ARIEL_VERSION := $(PRODUCT_VERSION_MAJOR).$(PRODUCT_VERSION_MINOR).$(LINEAGE_VERSION_MAINTENANCE)-$(TARGET_VENDOR_RELEASE_BUILD_ID)-$(ARIEL_BUILD)
            endif
        else
            ARIEL_VERSION := $(PRODUCT_VERSION_MAJOR).$(PRODUCT_VERSION_MINOR).$(PRODUCT_VERSION_MAINTENANCE)$(PRODUCT_VERSION_DEVICE_SPECIFIC)-$(ARIEL_BUILD)
        endif
    endif
else
    ifeq ($(LINEAGE_VERSION_MAINTENANCE),0)
        ifeq ($(LINEAGE_VERSION_APPEND_TIME_OF_DAY),true)
            ARIEL_VERSION := $(PRODUCT_VERSION_MAJOR).$(PRODUCT_VERSION_MINOR)-$(shell date -u +%Y%m%d_%H%M%S)-$(LINEAGE_BUILDTYPE)$(LINEAGE_EXTRAVERSION)-$(ARIEL_BUILD)
        else
            ARIEL_VERSION := $(PRODUCT_VERSION_MAJOR).$(PRODUCT_VERSION_MINOR)-$(shell date -u +%Y%m%d)-$(LINEAGE_BUILDTYPE)$(LINEAGE_EXTRAVERSION)-$(ARIEL_BUILD)
        endif
    else
        ifeq ($(LINEAGE_VERSION_APPEND_TIME_OF_DAY),true)
            ARIEL_VERSION := $(PRODUCT_VERSION_MAJOR).$(PRODUCT_VERSION_MINOR).$(LINEAGE_VERSION_MAINTENANCE)-$(shell date -u +%Y%m%d_%H%M%S)-$(LINEAGE_BUILDTYPE)$(LINEAGE_EXTRAVERSION)-$(ARIEL_BUILD)
        else
            ARIEL_VERSION := $(PRODUCT_VERSION_MAJOR).$(PRODUCT_VERSION_MINOR).$(LINEAGE_VERSION_MAINTENANCE)-$(shell date -u +%Y%m%d)-$(LINEAGE_BUILDTYPE)$(LINEAGE_EXTRAVERSION)-$(ARIEL_BUILD)
        endif
    endif
endif

PRODUCT_EXTRA_RECOVERY_KEYS += \
    vendor/lineage/build/target/product/security/lineage

-include vendor/lineage-priv/keys/keys.mk

ARIEL_DISPLAY_VERSION := $(ARIEL_VERSION)

ifneq ($(PRODUCT_DEFAULT_DEV_CERTIFICATE),)
ifneq ($(PRODUCT_DEFAULT_DEV_CERTIFICATE),build/target/product/security/testkey)
    ifneq ($(ARIEL_BUILDTYPE), UNOFFICIAL)
        ifndef TARGET_VENDOR_RELEASE_BUILD_ID
            ifneq ($(LINEAGE_EXTRAVERSION),)
                # Remove leading dash from LINEAGE_EXTRAVERSION
                LINEAGE_EXTRAVERSION := $(shell echo $(LINEAGE_EXTRAVERSION) | sed 's/-//')
                TARGET_VENDOR_RELEASE_BUILD_ID := $(LINEAGE_EXTRAVERSION)
            else
                TARGET_VENDOR_RELEASE_BUILD_ID := $(shell date -u +%Y%m%d)
            endif
        else
            TARGET_VENDOR_RELEASE_BUILD_ID := $(TARGET_VENDOR_RELEASE_BUILD_ID)
        endif
        ifeq ($(LINEAGE_VERSION_MAINTENANCE),0)
            ARIEL_DISPLAY_VERSION := $(PRODUCT_VERSION_MAJOR).$(PRODUCT_VERSION_MINOR)-$(TARGET_VENDOR_RELEASE_BUILD_ID)-$(ARIEL_BUILD)
        else
            ARIEL_DISPLAY_VERSION := $(PRODUCT_VERSION_MAJOR).$(PRODUCT_VERSION_MINOR).$(LINEAGE_VERSION_MAINTENANCE)-$(TARGET_VENDOR_RELEASE_BUILD_ID)-$(ARIEL_BUILD)
        endif
    endif
endif
endif

-include $(WORKSPACE)/build_env/image-auto-bits.mk
-include vendor/ariel/config/partner_gms.mk

$(call prepend-product-if-exists, vendor/extra/product.mk)

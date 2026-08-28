$(call inherit-product-if-exists, vendor/oneplus/oreality/oreality-vendor.mk)

PRODUCT_PACKAGES += \
    ORealityAudio \
    config-com.oplus.oreality.audio.xml \
    oplus.software.audio.audioeffect_support.xml \
    oplus.software.audio.audiox_support.xml \
    privapp-com.oplus.oreality.audio.xml

PRODUCT_SYSTEM_PROPERTIES += \
    ro.oplus.audio.effect.type=audiox_merge

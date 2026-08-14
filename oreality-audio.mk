OREALITY_AUDIO_PATH := vendor/oneplus/oreality

PRODUCT_PACKAGES += \
    libOplusAudioxAidl \
    liboplus_audiox \
    liboppo_audiox \
    liboppo_audiox_sw_effects \
    ORealityAudio \
    config-com.oplus.oreality.audio.xml \
    oplus.software.audio.audioeffect_support.xml \
    oplus.software.audio.audiox_support.xml \
    privapp-com.oplus.oreality.audio.xml

PRODUCT_SYSTEM_PROPERTIES += \
    ro.oplus.audio.effect.type=audiox_merge

PRODUCT_COPY_FILES += \
    $(call find-copy-subdir-files,*,$(OREALITY_AUDIO_PATH)/proprietary/odm/etc/audio/audiox_param,$(TARGET_COPY_OUT_ODM)/etc/audio/audiox_param) \
    $(call find-copy-subdir-files,*,$(OREALITY_AUDIO_PATH)/proprietary/odm/etc/spatializer,$(TARGET_COPY_OUT_ODM)/etc/spatializer)

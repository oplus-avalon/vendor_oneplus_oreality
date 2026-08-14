# OReality Audio

## Device Tree

Add to `device/oneplus/macanc/device.mk`:

```make
$(call inherit-product-if-exists, vendor/oneplus/oreality/oreality-audio.mk)
```

## Common Tree

Add to `device/oneplus/sm8850-common/configs/audio/audio_effects_config.xml`:

```xml
<library name="oppo_audiox_sw_effects" path="libOplusAudioxAidl.so"/>
```

```xml
<effect name="oppo_audiox_sw_effects" library="oppo_audiox_sw_effects" uuid="41f6c0f4-5d8f-11ec-bf63-0242ac130002" type="ec7178ec-e5e1-4432-a3f4-4657e6795210"/>
```

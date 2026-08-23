# OReality Audio

## Setup

1. Clone this repository into:

```text
vendor/oneplus/oreality
```

2. Add the required entries to the audio effects configuration.

**Library**

```xml
<!-- OReality Audio -->
<library name="oppo_audiox_sw_effects" path="libOplusAudioxAidl.so"/>
```

**Effects**

```xml
<!-- OReality Audio -->
<effect name="oppo_audiox_sw_effects" library="oppo_audiox_sw_effects" uuid="41f6c0f4-5d8f-11ec-bf63-0242ac130002" type="ec7178ec-e5e1-4432-a3f4-4657e6795210"/>
```

3. Inherit the OReality makefile from your device's makefile:

```make
$(call inherit-product-if-exists, vendor/oneplus/oreality/oreality-audio.mk)
```

Reference commit:

[OReality Audio effects integration](https://github.com/AxionOS-macan/android_device_oneplus_sm8850-common/commit/ee7033cfff7e69d05e846498e59a47f46f55bce7)

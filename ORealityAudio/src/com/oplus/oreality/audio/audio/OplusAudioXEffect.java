package com.oplus.oreality.audio.audio;

import android.media.audiofx.AudioEffect;
import android.util.Log;

import com.oplus.oreality.audio.data.SoundProfile;

import java.lang.reflect.Method;
import java.util.UUID;

public final class OplusAudioXEffect {
    private static final String TAG = "OplusAudioXEffect";
    private static final int PARAM_AUDIOX_ENABLE = 0x100100b9;
    private static final int PARAM_AUDIOX_PROFILE = 0x100100bb;
    private static final int PARAM_AUDIOX_EQ_GAINS = 0x100100bc;
    private static final int PARAM_AUDIOX_EQ_ENABLE = 0x100100c9;
    private static final int EQ_BAND_COUNT = 10;
    private static final UUID EFFECT_TYPE_NULL =
            UUID.fromString("ec7178ec-e5e1-4432-a3f4-4657e6795210");
    private static final UUID EFFECT_UUID =
            UUID.fromString("41f6c0f4-5d8f-11ec-bf63-0242ac130002");
    private static final Method SET_PARAMETER_METHOD = findSetParameterMethod();
    private static final Method SET_PARAMETER_BYTES_METHOD = findSetParameterBytesMethod();

    private final AudioEffect effect;

    public OplusAudioXEffect(int priority, int audioSession) {
        try {
            effect = AudioEffect.class
                    .getConstructor(UUID.class, UUID.class, int.class, int.class)
                    .newInstance(EFFECT_TYPE_NULL, EFFECT_UUID, priority, audioSession);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to create AudioX effect", e);
        }
    }

    public void setEffectEnabled(boolean enabled) {
        if (enabled) {
            setAudioEffectEnabled(true);
            setParameterInt(PARAM_AUDIOX_ENABLE, 1);
        } else {
            setParameterInt(PARAM_AUDIOX_ENABLE, 0);
            setAudioEffectEnabled(false);
        }
    }

    public void setProfile(SoundProfile profile) {
        setParameterInt(PARAM_AUDIOX_PROFILE, profile.getAudioXValue());
    }

    public void setEqualizerGains(int volumeIndex, int[] gains) {
        if (gains == null || gains.length != EQ_BAND_COUNT) {
            Log.w(TAG, "AudioX EQ gains must contain " + EQ_BAND_COUNT + " bands");
            return;
        }

        boolean enabled = false;
        for (int gain : gains) {
            if (gain != 0) {
                enabled = true;
                break;
            }
        }
        setParameterBytes(PARAM_AUDIOX_EQ_ENABLE, intToBytes(enabled ? 1 : 0));

        byte[] payload = new byte[(EQ_BAND_COUNT + 2) * 4];
        writeInt(volumeIndex, payload, 0);
        writeInt(EQ_BAND_COUNT, payload, 4);
        for (int i = 0; i < gains.length; i++) {
            writeInt(scaleEqualizerGain(gains[i]), payload, 8 + (i * 4));
        }
        setParameterBytes(PARAM_AUDIOX_EQ_GAINS, payload);
    }

    public void release() {
        effect.release();
    }

    public static boolean isRegistered() {
        try {
            AudioEffect.Descriptor[] descriptors = AudioEffect.queryEffects();
            if (descriptors == null) {
                return false;
            }

            for (AudioEffect.Descriptor descriptor : descriptors) {
                if (EFFECT_UUID.equals(descriptor.uuid)) {
                    return true;
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to query audio effects", e);
        }
        return false;
    }

    private void setAudioEffectEnabled(boolean enabled) {
        int result = effect.setEnabled(enabled);
        if (result != AudioEffect.SUCCESS) {
            Log.w(TAG, "AudioX setEnabled(" + enabled + ") returned " + result);
        }
    }

    private void setParameterInt(int param, int value) {
        if (SET_PARAMETER_METHOD == null) {
            Log.w(TAG, "AudioX setParameter(int,int) method is unavailable");
            return;
        }

        try {
            int result = (Integer) SET_PARAMETER_METHOD.invoke(effect, param, value);
            if (result != AudioEffect.SUCCESS) {
                Log.w(TAG, "AudioX setParameter(0x" + Integer.toHexString(param)
                        + ", " + value + ") returned " + result);
            } else {
                Log.d(TAG, "AudioX setParameter(0x" + Integer.toHexString(param)
                        + ", " + value + ") succeeded");
            }
        } catch (ReflectiveOperationException e) {
            Log.w(TAG, "AudioX setParameter(0x" + Integer.toHexString(param)
                    + ", " + value + ") failed", e);
        }
    }

    private void setParameterBytes(int param, byte[] value) {
        if (SET_PARAMETER_BYTES_METHOD == null) {
            Log.w(TAG, "AudioX setParameter(int,byte[]) method is unavailable");
            return;
        }

        try {
            int result = (Integer) SET_PARAMETER_BYTES_METHOD.invoke(effect, param, value);
            if (result != AudioEffect.SUCCESS) {
                Log.w(TAG, "AudioX setParameter(0x" + Integer.toHexString(param)
                        + ", byte[" + value.length + "]) returned " + result);
            } else {
                Log.d(TAG, "AudioX setParameter(0x" + Integer.toHexString(param)
                        + ", byte[" + value.length + "]) succeeded");
            }
        } catch (ReflectiveOperationException e) {
            Log.w(TAG, "AudioX setParameter(0x" + Integer.toHexString(param)
                    + ", byte[" + value.length + "]) failed", e);
        }
    }

    private static int scaleEqualizerGain(int gain) {
        return gain << 22;
    }

    private static byte[] intToBytes(int value) {
        byte[] bytes = new byte[4];
        writeInt(value, bytes, 0);
        return bytes;
    }

    private static void writeInt(int value, byte[] bytes, int offset) {
        bytes[offset] = (byte) (value & 0xff);
        bytes[offset + 1] = (byte) ((value >>> 8) & 0xff);
        bytes[offset + 2] = (byte) ((value >>> 16) & 0xff);
        bytes[offset + 3] = (byte) ((value >>> 24) & 0xff);
    }

    private static Method findSetParameterMethod() {
        try {
            Method method = AudioEffect.class.getDeclaredMethod(
                    "setParameter", int.class, int.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "Unable to find AudioEffect#setParameter(int, int)", e);
            return null;
        }
    }

    private static Method findSetParameterBytesMethod() {
        try {
            Method method = AudioEffect.class.getDeclaredMethod(
                    "setParameter", int.class, byte[].class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "Unable to find AudioEffect#setParameter(int, byte[])", e);
            return null;
        }
    }
}

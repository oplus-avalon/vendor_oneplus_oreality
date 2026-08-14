package com.oplus.oreality.audio.audio;

import android.media.AudioManager;
import android.util.Log;

import com.oplus.oreality.audio.data.SoundProfile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AudioEffectController {
    private static final String TAG = "AudioEffectController";

    private final AudioManager audioManager;
    private final Map<Integer, OplusAudioXEffect> audioXEffects = new HashMap<>();
    private boolean currentEnabled;
    private SoundProfile currentProfile = SoundProfile.SMART;
    private int[] currentEqualizerGains = new int[10];

    public AudioEffectController(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    public void updateActivePlaybackSessions(Set<Integer> sessionIds) {
        Set<Integer> desiredSessions = new HashSet<>();
        if (sessionIds != null) {
            for (Integer sessionId : sessionIds) {
                if (isValidAudioSession(sessionId)) {
                    desiredSessions.add(sessionId);
                }
            }
        }

        Set<Integer> removedSessions = new HashSet<>(audioXEffects.keySet());
        removedSessions.removeAll(desiredSessions);
        for (Integer sessionId : removedSessions) {
            OplusAudioXEffect effect = audioXEffects.remove(sessionId);
            if (effect != null) {
                effect.release();
                Log.i(TAG, "AudioX effect released for inactive session " + sessionId);
            }
        }

        for (Integer sessionId : desiredSessions) {
            ensureAudioXEffect(sessionId);
        }
    }

    public void addAudioSession(int sessionId) {
        if (!isValidAudioSession(sessionId)) {
            return;
        }

        Log.i(TAG, "Adding AudioX playback session " + sessionId);
        ensureAudioXEffect(sessionId);
    }

    public void removeAudioSession(int sessionId) {
        if (!isValidAudioSession(sessionId)) {
            return;
        }

        OplusAudioXEffect effect = audioXEffects.remove(sessionId);
        if (effect != null) {
            effect.release();
            Log.i(TAG, "Released AudioX playback session " + sessionId);
        }
    }

    public void setEnabled(boolean enabled) {
        currentEnabled = enabled;
        try {
            for (OplusAudioXEffect effect : audioXEffects.values()) {
                effect.setEffectEnabled(enabled);
            }
            if (enabled) {
                for (OplusAudioXEffect effect : audioXEffects.values()) {
                    effect.setProfile(currentProfile);
                }
            }
            if (enabled && audioXEffects.isEmpty()) {
                Log.i(TAG, "AudioX enabled; waiting for playback audio session");
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Error setting effects enabled: " + enabled, e);
        }
    }

    public void applyProfile(SoundProfile profile) {
        currentProfile = profile;
        Log.d(TAG, "Applying profile: " + profile);
        for (OplusAudioXEffect effect : audioXEffects.values()) {
            effect.setProfile(profile);
        }
    }

    public void applyEqualizerGains(int[] gains) {
        if (gains == null || gains.length != currentEqualizerGains.length) {
            return;
        }
        currentEqualizerGains = gains.clone();
        Log.d(TAG, "Applying EQ gains");
        for (OplusAudioXEffect effect : audioXEffects.values()) {
            effect.setEqualizerGains(getMusicVolumeIndex(), currentEqualizerGains);
        }
    }

    public void reapplyCurrentState() {
        setEnabled(currentEnabled);
        if (currentEnabled) {
            applyProfile(currentProfile);
            applyEqualizerGains(currentEqualizerGains);
        }
    }

    public void release() {
        for (OplusAudioXEffect effect : audioXEffects.values()) {
            effect.release();
        }
        audioXEffects.clear();
    }

    private int getMusicVolumeIndex() {
        if (audioManager == null) {
            return 0;
        }
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (maxVolume > 100) {
            volume /= 10;
        }
        return volume;
    }

    private OplusAudioXEffect ensureAudioXEffect(int sessionId) {
        if (!OplusAudioXEffect.isRegistered()) {
            Log.w(TAG, "AudioX effect is not registered in audio_effects config");
            return null;
        }

        OplusAudioXEffect existing = audioXEffects.get(sessionId);
        if (existing != null) {
            return existing;
        }

        try {
            OplusAudioXEffect effect = new OplusAudioXEffect(0, sessionId);
            audioXEffects.put(sessionId, effect);
            effect.setEffectEnabled(currentEnabled);
            if (currentEnabled) {
                effect.setProfile(currentProfile);
                effect.setEqualizerGains(getMusicVolumeIndex(), currentEqualizerGains);
            }
            Log.i(TAG, "AudioX effect initialized for session " + sessionId);
            return effect;
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to initialize AudioX effect for session " + sessionId, e);
            return null;
        }
    }

    private static boolean isValidAudioSession(Integer sessionId) {
        return sessionId != null && sessionId > 0;
    }
}

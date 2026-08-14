package com.oplus.oreality.audio;

import android.app.Application;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.oplus.oreality.audio.audio.AudioEffectController;
import com.oplus.oreality.audio.data.SettingsRepository;
import com.oplus.oreality.audio.data.SoundProfile;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ORealityApplication extends Application {
    private static final String TAG = "ORealityApplication";
    private static final int PLAYER_STATE_STARTED = 2;

    private AudioEffectController audioEffectController;
    private AudioManager audioManager;
    private SettingsRepository settingsRepository;
    private AudioManager.AudioPlaybackCallback playbackCallback;
    private SharedPreferences.OnSharedPreferenceChangeListener settingsListener;

    @Override
    public void onCreate() {
        super.onCreate();
        settingsRepository = new SettingsRepository(this);
        audioManager = getSystemService(AudioManager.class);
        audioEffectController = new AudioEffectController(audioManager);
        observePlaybackSessions();
        observeSettings();
        applyCurrentSettings();
    }

    public AudioEffectController getAudioEffectController() {
        return audioEffectController;
    }

    public SettingsRepository getSettingsRepository() {
        return settingsRepository;
    }

    private void observePlaybackSessions() {
        playbackCallback = new AudioManager.AudioPlaybackCallback() {
            @Override
            public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                updatePlaybackSessions(configs);
            }
        };
        audioManager.registerAudioPlaybackCallback(
                playbackCallback, new Handler(Looper.getMainLooper()));
        refreshPlaybackSessions();
    }

    private void observeSettings() {
        settingsListener = (prefs, key) -> applyCurrentSettings();
        settingsRepository.registerListener(settingsListener);
    }

    public void refreshAudioEffects() {
        if (!settingsRepository.isEnabled()) {
            return;
        }
        refreshPlaybackSessions();
        audioEffectController.reapplyCurrentState();
    }

    private void applyCurrentSettings() {
        boolean enabled = settingsRepository.isEnabled();
        if (enabled) {
            refreshPlaybackSessions();
        }
        audioEffectController.setEnabled(enabled);
        if (enabled) {
            SoundProfile profile = settingsRepository.getProfile();
            audioEffectController.applyProfile(profile);
            audioEffectController.applyEqualizerGains(settingsRepository.getEqualizerGains(profile));
        }
    }

    private void refreshPlaybackSessions() {
        updatePlaybackSessions(audioManager.getActivePlaybackConfigurations());
    }

    private void updatePlaybackSessions(List<AudioPlaybackConfiguration> configs) {
        Set<Integer> sessions = new HashSet<>();
        for (AudioPlaybackConfiguration config : configs) {
            if (getPlayerState(config) == PLAYER_STATE_STARTED) {
                int sessionId = getAudioSessionId(config);
                if (sessionId > 0) {
                    sessions.add(sessionId);
                }
            }
        }

        Log.d(TAG, "Active playback sessions: " + sessions);
        audioEffectController.updateActivePlaybackSessions(sessions);
    }

    private static int getAudioSessionId(AudioPlaybackConfiguration config) {
        int sessionId = invokeInt(config, "getSessionId");
        return sessionId > 0 ? sessionId : invokeInt(config, "getAudioSessionId");
    }

    private static int getPlayerState(AudioPlaybackConfiguration config) {
        return invokeInt(config, "getPlayerState");
    }

    private static int invokeInt(Object receiver, String methodName) {
        try {
            Method method = receiver.getClass().getMethod(methodName);
            Object value = method.invoke(receiver);
            return value instanceof Integer ? (Integer) value : 0;
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    @Override
    public void onTerminate() {
        if (playbackCallback != null) {
            audioManager.unregisterAudioPlaybackCallback(playbackCallback);
        }
        if (settingsListener != null) {
            settingsRepository.unregisterListener(settingsListener);
        }
        audioEffectController.release();
        super.onTerminate();
    }
}

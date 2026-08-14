package com.oplus.oreality.audio.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class SettingsRepository {
    private static final String PREFS_NAME = "oreality_audio";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_EQ_GAINS = "eq_gains";
    private static final int EQ_BAND_COUNT = 10;

    private final SharedPreferences prefs;

    public SettingsRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, true);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public SoundProfile getProfile() {
        return SoundProfile.fromName(prefs.getString(KEY_PROFILE, SoundProfile.SMART.name()));
    }

    public void setProfile(SoundProfile profile) {
        prefs.edit().putString(KEY_PROFILE, profile.name()).apply();
    }

    public int[] getEqualizerGains(SoundProfile profile) {
        int[] gains = new int[EQ_BAND_COUNT];
        String value = prefs.getString(equalizerKey(profile), prefs.getString(KEY_EQ_GAINS, ""));
        if (value == null || value.isEmpty()) {
            return gains;
        }

        String[] parts = value.split(",");
        for (int i = 0; i < gains.length && i < parts.length; i++) {
            try {
                gains[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                gains[i] = 0;
            }
        }
        return gains;
    }

    public void setEqualizerGains(SoundProfile profile, int[] gains) {
        if (gains == null || gains.length != EQ_BAND_COUNT) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < gains.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(gains[i]);
        }
        prefs.edit().putString(equalizerKey(profile), builder.toString()).apply();
    }

    public void resetEqualizerGains(SoundProfile profile) {
        prefs.edit().putString(equalizerKey(profile), emptyEqualizerValue()).apply();
    }

    private static String emptyEqualizerValue() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < EQ_BAND_COUNT; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(0);
        }
        return builder.toString();
    }

    private static String equalizerKey(SoundProfile profile) {
        SoundProfile safeProfile = profile == null ? SoundProfile.SMART : profile;
        return KEY_EQ_GAINS + "_" + safeProfile.name();
    }

    public void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }
}

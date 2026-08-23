package com.oplus.oreality.audio;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.oplus.oreality.audio.data.SettingsRepository;
import com.oplus.oreality.audio.data.SoundProfile;

/** Supplies the live OReality state shown by the Settings sound page. */
public final class ORealitySummaryProvider extends ContentProvider {
    private static final String KEY_OREALITY = "oreality";
    private static final String SUMMARY_KEY = "com.android.settings.summary";

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (!KEY_OREALITY.equals(method) || getContext() == null) {
            return null;
        }

        SettingsRepository settings = new SettingsRepository(getContext());
        String summary;
        if (!settings.isEnabled()) {
            summary = getContext().getString(R.string.settings_oreality_off);
        } else {
            SoundProfile profile = settings.getProfile();
            summary = getContext().getString(
                    R.string.settings_oreality_on_with_profile, profile.getTitle());
        }

        Bundle result = new Bundle();
        result.putString(SUMMARY_KEY, summary);
        return result;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) { return null; }

    @Override
    public String getType(Uri uri) { return null; }

    @Override
    public Uri insert(Uri uri, ContentValues values) { return null; }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) { return 0; }
}

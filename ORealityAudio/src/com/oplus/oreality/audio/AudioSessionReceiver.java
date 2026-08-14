package com.oplus.oreality.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.util.Log;

public final class AudioSessionReceiver extends BroadcastReceiver {
    private static final String TAG = "AudioSessionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Context appContext = context.getApplicationContext();
        if (!(appContext instanceof ORealityApplication)) {
            return;
        }

        ORealityApplication app = (ORealityApplication) appContext;
        int sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0);
        String action = intent.getAction();

        if (AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION.equals(action)) {
            Log.d(TAG, "Open audio session " + sessionId);
            app.getAudioEffectController().addAudioSession(sessionId);
            app.refreshAudioEffects();
        } else if (AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION.equals(action)) {
            Log.d(TAG, "Close audio session " + sessionId);
            app.getAudioEffectController().removeAudioSession(sessionId);
        }
    }
}

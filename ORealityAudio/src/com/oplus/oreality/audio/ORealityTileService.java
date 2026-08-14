package com.oplus.oreality.audio;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.oplus.oreality.audio.data.SettingsRepository;

public final class ORealityTileService extends TileService {
    private SettingsRepository settingsRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        settingsRepository = ((ORealityApplication) getApplication()).getSettingsRepository();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        ((ORealityApplication) getApplication()).refreshAudioEffects();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean enabled = !settingsRepository.isEnabled();
        settingsRepository.setEnabled(enabled);
        updateTile(enabled);
    }

    private void updateTile() {
        updateTile(settingsRepository.isEnabled());
    }

    private void updateTile(boolean enabled) {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        tile.setLabel(getString(R.string.qs_tile_oreality_audio));
        tile.setSubtitle(enabled
                ? getString(R.string.qs_tile_oreality_audio_on)
                : getString(R.string.qs_tile_oreality_audio_off));
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}

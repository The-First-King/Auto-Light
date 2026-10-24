package com.mine.autolight;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

// Quick Settings tile: tap opens TileDetailsActivity, where the service and QR mode are controlled.
public class AutoLightTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        Intent intent = new Intent(this, TileDetailsActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PendingIntent pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            startActivityAndCollapse(pending);
        } else {
            startActivityAndCollapse(intent);
        }
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean running = LightService.isRunning;
        tile.setLabel(getString(R.string.app_name));
        tile.setState(running ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int subtitle = ServiceController.isQrModeActive(this) ? R.string.qr_mode_active
                    : running ? R.string.service_running
                    : R.string.service_stopped;
            tile.setSubtitle(getString(subtitle));
        }
        tile.updateTile();
    }
}

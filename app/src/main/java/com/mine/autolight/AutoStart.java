package com.mine.autolight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class AutoStart extends BroadcastReceiver {

    private static final String TAG = "AutoStartReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        final String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }

        // QR-mode timer does not survive a reboot - end the pause now
        if (ServiceController.isQrModeActive(context)) {
            ServiceController.endQrMode(context);
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean(Constants.PREF_ENABLED_KEY, true);

        if (!isEnabled) {
            Log.d(TAG, "Boot detected, but service is disabled by user. Doing nothing.");
            return;
        }

        Log.d(TAG, "Boot completed detected and service is enabled. Starting LightService...");
        Intent serviceIntent = new Intent(context, LightService.class);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
                       Log.e(TAG, "Failed to start service on boot: " + e.getMessage());
        }
    }
}

package com.mine.autolight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Receiver to catch the system BOOT_COMPLETED broadcast.
 * This allows the app to restart the LightService automatically after a device reboot.
 */
public class AutoStart extends BroadcastReceiver {
    private static final String TAG = "AutoStartReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Verify that we are responding to the correct intent
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed detected. Starting LightService...");

            // Create the intent for your service
            Intent serviceIntent = new Intent(context, LightService.class);

            try {
                // Android 8.0 (Oreo) and above requires startForegroundService
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
}

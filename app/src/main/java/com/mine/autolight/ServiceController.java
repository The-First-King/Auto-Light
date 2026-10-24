package com.mine.autolight;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.quicksettings.TileService;

// Start/stop logic shared by the main screen, the Quick Settings tile and the QR-mode timer.
public final class ServiceController {

    private ServiceController() {}

    public static boolean isEnabledByUser(Context context) {
        return prefs(context).getBoolean(Constants.PREF_ENABLED_KEY, true);
    }

    public static boolean isQrModeActive(Context context) {
        return prefs(context).getLong(Constants.PREF_QR_MODE_UNTIL_KEY, 0L) > 0L;
    }

    // Turns the service on or off. Cancels any QR-mode pause first.
    public static void setServiceOn(Context context, boolean on) {
        cancelQrModeTimer(context);
        prefs(context).edit()
                .putBoolean(Constants.PREF_ENABLED_KEY, on)
                .putLong(Constants.PREF_QR_MODE_UNTIL_KEY, 0L)
                .apply();
        if (on) startService(context);
        else stopService(context);
    }

    // Stops the service for QR_MODE_DURATION_MS; the timer then re-enables it.
    public static void startQrMode(Context context) {
        long until = System.currentTimeMillis() + Constants.QR_MODE_DURATION_MS;
        prefs(context).edit()
                .putBoolean(Constants.PREF_ENABLED_KEY, false)
                .putLong(Constants.PREF_QR_MODE_UNTIL_KEY, until)
                .apply();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, until, qrModeIntent(context));
        }
        stopService(context);
    }

    // Ends QR mode, either early or when its timer fires: re-enables and starts the service.
    public static void endQrMode(Context context) {
        cancelQrModeTimer(context);
        prefs(context).edit()
                .putBoolean(Constants.PREF_ENABLED_KEY, true)
                .putLong(Constants.PREF_QR_MODE_UNTIL_KEY, 0L)
                .apply();
        startService(context);
    }

    public static void requestTileRefresh(Context context) {
        TileService.requestListeningState(context,
                new ComponentName(context, AutoLightTileService.class));
    }

    private static void cancelQrModeTimer(Context context) {
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, qrModeIntentRaw(context),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pending == null) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) alarmManager.cancel(pending);
        pending.cancel();
    }

    private static PendingIntent qrModeIntent(Context context) {
        return PendingIntent.getBroadcast(context, 0, qrModeIntentRaw(context),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static Intent qrModeIntentRaw(Context context) {
        return new Intent(context, QrModeReceiver.class);
    }

    private static void startService(Context context) {
        Intent serviceIntent = new Intent(context, LightService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent);
        else context.startService(serviceIntent);
    }

    private static void stopService(Context context) {
        context.stopService(new Intent(context, LightService.class));
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }
}

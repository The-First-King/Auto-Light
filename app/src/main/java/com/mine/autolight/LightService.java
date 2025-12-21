package com.mine.autolight;

import android.app.*;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.*;
import android.os.*;
import android.provider.Settings;

public class LightService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private float mSmoothedLux = -1.0f;
    private static final float ALPHA = 0.2f;

    @Override
    public void onCreate() {
        super.onCreate();
        new MySettings(this);
        setupForeground();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (light != null) sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void setupForeground() {
        String cid = "autolight_chan";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(cid, "Auto-Light", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
        Notification n = new Notification.Builder(this, cid)
                .setContentTitle("Auto-Light Active")
                .setSmallIcon(android.R.drawable.ic_menu_compass).build();
        startForeground(1, n);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Work Mode Logic (Landscape / Portrait / Always)
        int orientation = getResources().getConfiguration().orientation;
        int mode = MySettings.currentMode;

        if (mode == 1 && orientation != Configuration.ORIENTATION_LANDSCAPE) return; // Landscape Only
        if (mode == 2 && orientation != Configuration.ORIENTATION_PORTRAIT) return; // Portrait Only

        float lux = event.values[0];
        mSmoothedLux = (mSmoothedLux == -1.0f) ? lux : (mSmoothedLux * (1.0f - ALPHA)) + (lux * ALPHA);
        
        int target = BrightnessAlgorithm.calculateBrightness(mSmoothedLux);
        try {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, target);
        } catch (Exception ignored) {}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new MySettings(this); // Reload settings if "Apply" was hit
        return START_STICKY;
    }

    @Override public void onDestroy() { 
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onDestroy(); 
    }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onAccuracyChanged(Sensor s, int a) {}
}

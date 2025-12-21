package com.mine.autolight;

import android.app.*;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.*;
import android.os.*;
import android.provider.Settings;

public class LightService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private MySettings mySettings;
    private float mSmoothedLux = -1.0f;
    private static final float ALPHA = 0.2f;

    @Override
    public void onCreate() {
        super.onCreate();
        mySettings = new MySettings(this);
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "reload".equals(intent.getStringExtra("command"))) {
            mySettings.load(); // Forces background process to update mode/points
        }
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int orientation = getResources().getConfiguration().orientation;
        
        // WORK MODE LOGIC
        if (mySettings.mode == Constants.WORK_MODE_LANDSCAPE && orientation != Configuration.ORIENTATION_LANDSCAPE) return;
        if (mySettings.mode == Constants.WORK_MODE_PORTRAIT && orientation != Configuration.ORIENTATION_PORTRAIT) return;

        float lux = event.values[0];
        mSmoothedLux = (mSmoothedLux == -1.0f) ? lux : (mSmoothedLux * (1.0f - ALPHA)) + (lux * ALPHA);
        
        int target = BrightnessAlgorithm.calculateBrightness(mSmoothedLux);
        
        try {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, target);
            
            // Send Data back to UI
            Intent updateIntent = new Intent("COM_MINE_AUTOLIGHT_UPDATE");
            updateIntent.putExtra("lux", (int)lux);
            updateIntent.putExtra("bri", target);
            sendBroadcast(updateIntent);
            
        } catch (Exception ignored) {}
    }

    @Override public void onDestroy() { 
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onDestroy(); 
    }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onAccuracyChanged(Sensor s, int a) {}
}

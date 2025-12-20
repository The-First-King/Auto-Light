package com.mine.autolight;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

public class LightService extends Service implements SensorEventListener {

    private static final String TAG = "AutoLightService";
    private static final String CHANNEL_ID = "AutoLightChannel";
    
    private SensorManager sensorManager;
    private Sensor lightSensor;

    // Smoothing & Logic Constants
    private static final float ALPHA = 0.2f; // Smoothing factor (0.1 to 0.3 is best)
    private float mSmoothedLux = -1.0f;
    private int mLastAppliedBrightness = -1;
    private static final int MIN_CHANGE_THRESHOLD = 5; // Ignored if change is smaller than 5

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 1. Load the user settings into the static Map in MySettings
        new MySettings(this);

        // 2. Start as Foreground Service (Required for Android 8.0+)
        startForegroundServiceCompat();

        // 3. Setup Hardware Sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (lightSensor != null) {
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    private void startForegroundServiceCompat() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Auto-Brightness Service", 
                    NotificationManager.IMPORTANCE_LOW);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("Auto-Light Active")
                .setContentText("Monitoring light to adjust brightness smoothly.")
                .setSmallIcon(android.R.drawable.ic_menu_compass) 
                .build();

        // Start as foreground (ID cannot be 0)
        startForeground(1, notification);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float currentLux = event.values[0];

            // Step 1: Smoothing (Low-pass filter)
            if (mSmoothedLux == -1.0f) {
                mSmoothedLux = currentLux;
            } else {
                mSmoothedLux = (mSmoothedLux * (1.0f - ALPHA)) + (currentLux * ALPHA);
            }

            // Step 2: Calculate target brightness using our Logarithmic Algorithm
            int targetBrightness = BrightnessAlgorithm.calculateBrightness(mSmoothedLux);

            // Step 3: Apply brightness only if change is significant (Battery Optimization)
            if (Math.abs(targetBrightness - mLastAppliedBrightness) >= MIN_CHANGE_THRESHOLD) {
                updateSystemBrightness(targetBrightness);
            }
        }
    }

    private void updateSystemBrightness(int brightness) {
        try {
            // Write to Android System Settings
            Settings.System.putInt(getContentResolver(), 
                    Settings.System.SCREEN_BRIGHTNESS, brightness);
            
            mLastAppliedBrightness = brightness;
            Log.d(TAG, "Brightness updated: " + brightness + " (Lux: " + mSmoothedLux + ")");
        } catch (Exception e) {
            Log.e(TAG, "Write Settings Permission is missing.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Service will restart automatically if killed
    }

    @Override
    public void onDestroy() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

package com.mine.autolight;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.provider.Settings;

public class LightService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private static final float ALPHA = 0.2f; 
    private float mSmoothedLux = -1.0f;
    private int mLastAppliedBrightness = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float currentLux = event.values[0];
        if (mSmoothedLux == -1.0f) mSmoothedLux = currentLux;
        else mSmoothedLux = (mSmoothedLux * (1.0f - ALPHA)) + (currentLux * ALPHA);

        int targetBrightness = BrightnessAlgorithm.calculateBrightness(mSmoothedLux);

        // Only update if change is > 3 to save battery
        if (Math.abs(targetBrightness - mLastAppliedBrightness) > 3) {
            try {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, targetBrightness);
                mLastAppliedBrightness = targetBrightness;
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}

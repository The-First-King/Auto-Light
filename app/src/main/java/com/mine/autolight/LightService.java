package com.mine.autolight;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

public class LightService extends Service implements SensorEventListener {

    private WindowManager windowManager;
    private View dummyView;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private MySettings settings;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int payload = intent.getIntExtra(Constants.SERVICE_INTENT_EXTRA, -1);
            if (payload == Constants.SERVICE_INTENT_PAYLOAD_SET) {
                settings.load();
                refreshOverlayState();
            } else if (payload == 10) { // HIDE_OVERLAY
                removeOverlay();
            } else if (payload == 11) { // SHOW_OVERLAY
                refreshOverlayState();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        settings = new MySettings(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        IntentFilter filter = new IntentFilter(Constants.SERVICE_INTENT_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }

        refreshOverlayState();
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void refreshOverlayState() {
        removeOverlay(); // Clean up first
        if (settings.mode == Constants.WORK_MODE_UNLOCK || settings.mode == Constants.WORK_MODE_LANDSCAPE || settings.mode == Constants.WORK_MODE_PORTRAIT) {
            createOverlay();
        }
    }

    private void createOverlay() {
        if (dummyView != null) return;

        dummyView = new View(this);
        int type = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1, 1, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        try {
            windowManager.addView(dummyView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeOverlay() {
        if (windowManager != null && dummyView != null) {
            try {
                windowManager.removeViewImmediate(dummyView);
            } catch (Exception e) {
                // View might not be attached
            } finally {
                dummyView = null;
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float lux = event.values[0];
        // ... (Your brightness logic remains here)
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        sensorManager.unregisterListener(this);
        removeOverlay();
        super.onDestroy();
    }
}

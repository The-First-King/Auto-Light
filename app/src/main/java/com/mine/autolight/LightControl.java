package com.mine.autolight;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import java.util.ArrayDeque;

public class LightControl implements SensorEventListener {

    private final SensorManager sMgr;
    private final Sensor lightSensor;
    private final MySettings sett;
    private final ContentResolver cResolver;
    private final Context context;

    // Used only to schedule stopListening after "pause" in non-always modes
    private final Handler delayer = new Handler(Looper.getMainLooper());
    private final long pause = 2500;

    private boolean onListen = false;
    private boolean landscape = false;
    private boolean needsImmediateUpdate = false;

    private float lux = 0;
    private int tempBrightness = 0;

    // Window smoothing settings
    private final ArrayDeque<SensorReading> buffer = new ArrayDeque<>();

    private float lastAppliedLux = -1f;
    private float rollingSum = 0f;

    LightControl(Context context) {
        this.context = context;
        sett = new MySettings(context);
        cResolver = context.getContentResolver();
        sMgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sMgr.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT) return;

        float rawLux = event.values[0];
        long now = SystemClock.elapsedRealtime();

        if (lastAppliedLux != -1f && !needsImmediateUpdate) {
            float gap = Math.abs(rawLux - lastAppliedLux);
            float percentChange = (lastAppliedLux == 0f) ? 100f : (gap / lastAppliedLux) * 100f;
            
            // If the ambient sensor data gap is significant, react immediately
            if (gap > sett.quickReactLux && percentChange > sett.quickReactPercent) {
                // Clear old data and apply new brightness instantly
                buffer.clear();
                buffer.addLast(new SensorReading(now, rawLux));
                rollingSum = rawLux;
                applyAndRecord(rawLux);
                return;
            }
        }

        buffer.addLast(new SensorReading(now, rawLux));
        rollingSum += rawLux;

        while (!buffer.isEmpty() && (now - buffer.peekFirst().time) > sett.windowMs) {
            SensorReading old = buffer.removeFirst();
            rollingSum -= old.value;
        }

        if (needsImmediateUpdate || sett.mode == Constants.WORK_MODE_UNLOCK) {
            lux = rawLux;
            setBrightness((int) lux);

            if (sett.mode == Constants.WORK_MODE_UNLOCK) {
                needsImmediateUpdate = false;
                stopListening();
            } else {
                needsImmediateUpdate = false;
            }
            return;
        }

        processSmoothedLux();
    }

    private void processSmoothedLux() {
        if (buffer.isEmpty()) return;

        // First apply: use last sample immediately
        if (lastAppliedLux == -1f) {
            lux = buffer.peekLast().value;
            applyAndRecord(lux);
            return;
        }

        float averageLux = rollingSum / buffer.size();
        float diff = Math.abs(averageLux - lastAppliedLux);

        // Update if diff > (hysteresis% of lastAppliedLux) OR diff > absoluteThreshold
        if (diff > (lastAppliedLux * sett.hysteresisThreshold) || diff > sett.absoluteThreshold) {
            lux = averageLux;
            applyAndRecord(lux);
        }
    }

    private void applyAndRecord(float luxVal) {
        setBrightness((int) luxVal);
        lastAppliedLux = luxVal;
    }

    public void prepareForScreenOn() {
        needsImmediateUpdate = true;
        lastAppliedLux = -1f;

        buffer.clear();
        rollingSum = 0f;
    }

    private void scheduleSuspend() {
        if (sett.mode == Constants.WORK_MODE_ALWAYS) return;
        if (sett.mode == Constants.WORK_MODE_PORTRAIT && !landscape) return;
        if (sett.mode == Constants.WORK_MODE_LANDSCAPE && landscape) return;

        delayer.removeCallbacksAndMessages(null);
        delayer.postDelayed(this::stopListening, pause);
    }

    public void startListening() {
        boolean shouldActivate = false;

        if (sett.mode == Constants.WORK_MODE_ALWAYS) {
            shouldActivate = true;
        } else if (sett.mode == Constants.WORK_MODE_LANDSCAPE) {
            shouldActivate = landscape || needsImmediateUpdate;
        } else if (sett.mode == Constants.WORK_MODE_PORTRAIT) {
            shouldActivate = !landscape || needsImmediateUpdate;
        } else if (sett.mode == Constants.WORK_MODE_UNLOCK) {
            shouldActivate = true;
        }

        if (shouldActivate) {
            delayer.removeCallbacksAndMessages(null);

            if (!onListen && lightSensor != null) {
                if (sett.mode == Constants.WORK_MODE_UNLOCK || needsImmediateUpdate) {
                    lastAppliedLux = -1f;
                    buffer.clear();
                    rollingSum = 0f;
                }
                sMgr.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
                onListen = true;
            }

            scheduleSuspend();
        } else {
            stopListening();
        }
    }

    public void stopListening() {
        if (onListen) {
            sMgr.unregisterListener(this);
            onListen = false;
        }
        delayer.removeCallbacksAndMessages(null);
    }

    private void setBrightness(int luxValue) {
        int brightnessPercent;

        if (luxValue <= sett.l1) brightnessPercent = sett.b1;
        else if (luxValue >= sett.l4) brightnessPercent = sett.b4;
        else {
            float x1, y1, x2, y2;

            if (luxValue <= sett.l2) { x1 = sett.l1; x2 = sett.l2; y1 = sett.b1; y2 = sett.b2; }
            else if (luxValue <= sett.l3) { x1 = sett.l2; x2 = sett.l3; y1 = sett.b2; y2 = sett.b3; }
            else { x1 = sett.l3; x2 = sett.l4; y1 = sett.b3; y2 = sett.b4; }

            double lx = Math.log10((double) luxValue + 1.0);
            double lx1 = Math.log10((double) x1 + 1.0);
            double lx2 = Math.log10((double) x2 + 1.0);

            double t = (lx2 - lx1 == 0) ? 0 : (lx - lx1) / (lx2 - lx1);
            t = Math.max(0.0, Math.min(1.0, t));

            brightnessPercent = (int) Math.round(y1 + (y2 - y1) * t);
        }

        tempBrightness = brightnessPercent;

        int systemMax = 255;
        int resId = context.getResources().getIdentifier("config_screenBrightnessSettingMaximum", "integer", "android");
        if (resId > 0) {
            systemMax = context.getResources().getInteger(resId);
        }

        int finalSystemValue = Math.round((brightnessPercent / 100.0f) * systemMax);

        finalSystemValue = Math.max(1, Math.min(systemMax, finalSystemValue));

        try {
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, finalSystemValue);
        } catch (Exception ignored) { }
    }

    public void reconfigure() {
        stopListening();
        sett.load();
        startListening();
    }

    public void setLandscape(boolean land) {
        this.landscape = land;
    }

    public void onScreenUnlock() {
        try {
            Settings.System.putInt(
                    cResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            );
        } catch (Exception ignored) { }

        needsImmediateUpdate = true;
        startListening();
    }

    public int getLastSensorValue() { return (int) lux; }

    public int getSetBrightness() { return tempBrightness; }

    private static class SensorReading {
        final long time;
        final float value;

        SensorReading(long time, float value) {
            this.time = time;
            this.value = value;
        }
    }
}

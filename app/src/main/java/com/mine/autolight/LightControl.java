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

public class LightControl implements SensorEventListener {

    private final SensorManager sMgr;
    private final Sensor lightSensor;
    private final MySettings sett;
    private final ContentResolver cResolver;

    private final Handler delayer = new Handler(Looper.getMainLooper());
    private final long pause = 2500;

    private boolean onListen = false;
    private boolean landscape = false;
    private boolean needsImmediateUpdate = false;

    private float lux = 0;
    private int tempBrightness = 0;

    // Hysteresis (preserved)
    private static final float HYSTERESIS_THRESHOLD = 0.15f;
    private static final float MIN_ABS_LUX_DELTA = 5f;

    // EMA
    private static final long EMA_TAU_MS = 2500;
    private boolean hasEma = false;
    private float emaLux = 0f;
    private long lastEmaTimeMs = 0L;

    private float lastAppliedLux = -1;

    LightControl(Context context) {
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

        // Immediate update path (screen-on prep) + Unlock mode path
        if (needsImmediateUpdate || sett.mode == Constants.WORK_MODE_UNLOCK) {
            lux = rawLux;
            setBrightness((int) lux);

            // Sync smoothing state so we don't jump when continuing
            hasEma = true;
            emaLux = rawLux;
            lastEmaTimeMs = now;
            lastAppliedLux = rawLux;

            if (sett.mode == Constants.WORK_MODE_UNLOCK) {
                needsImmediateUpdate = false;
                stopListening(); // unlock applies once then stops listening
            } else {
                needsImmediateUpdate = false;
            }
            return;
        }

        // Continuous modes: EMA + hysteresis gate
        float smoothed = updateEma(now, rawLux);
        if (shouldApply(smoothed)) {
            lux = smoothed;
            setBrightness((int) lux);
            lastAppliedLux = smoothed;
        }
    }

    private void resetSmoothing(long nowMs) {
        hasEma = false;
        emaLux = 0f;
        lastEmaTimeMs = nowMs;
        lastAppliedLux = -1;
    }

    private float updateEma(long nowMs, float rawLux) {
        if (!hasEma) {
            hasEma = true;
            emaLux = rawLux;
            lastEmaTimeMs = nowMs;
            return emaLux;
        }

        long dt = nowMs - lastEmaTimeMs;
        if (dt <= 0) return emaLux;

        float alpha = 1f - (float) Math.exp(-(double) dt / (double) EMA_TAU_MS);
        emaLux = emaLux + alpha * (rawLux - emaLux);
        lastEmaTimeMs = nowMs;

        return emaLux;
    }

    private boolean shouldApply(float smoothedLux) {
        if (lastAppliedLux < 0) return true;
        float diff = Math.abs(smoothedLux - lastAppliedLux);
        float threshold = Math.max(lastAppliedLux * HYSTERESIS_THRESHOLD, MIN_ABS_LUX_DELTA);
        return diff > threshold;
    }

    public void prepareForScreenOn() {
        needsImmediateUpdate = true;
        resetSmoothing(SystemClock.elapsedRealtime());
        startListening();
    }

    private void scheduleSuspend() {
        if (sett.mode == Constants.WORK_MODE_ALWAYS) return;
        if (sett.mode == Constants.WORK_MODE_PORTRAIT && !landscape) return;
        if (sett.mode == Constants.WORK_MODE_LANDSCAPE && landscape) return;

        delayer.removeCallbacksAndMessages(null);
        delayer.postDelayed(this::stopListening, pause);
    }

    public void startListening() {
        boolean shouldActivate;

        if (sett.mode == Constants.WORK_MODE_ALWAYS) {
            shouldActivate = true;
        } else if (sett.mode == Constants.WORK_MODE_LANDSCAPE) {
            shouldActivate = landscape || needsImmediateUpdate;
        } else if (sett.mode == Constants.WORK_MODE_PORTRAIT) {
            shouldActivate = !landscape || needsImmediateUpdate;
        } else { // UNLOCK
            shouldActivate = true;
        }

        if (shouldActivate) {
            delayer.removeCallbacksAndMessages(null);

            if (!onListen && lightSensor != null) {
                if (sett.mode == Constants.WORK_MODE_UNLOCK || needsImmediateUpdate) {
                    resetSmoothing(SystemClock.elapsedRealtime());
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
        int brightness;

        if (luxValue <= sett.l1) brightness = sett.b1;
        else if (luxValue >= sett.l4) brightness = sett.b4;
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

            brightness = (int) Math.round(y1 + (y2 - y1) * t);
        }

        tempBrightness = brightness;
        try {
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
        } catch (Exception ignored) { }
    }

    public void reconfigure() {
        stopListening();
        sett.load();
        startListening();
    }

    public void setLandscape(boolean land) {
        landscape = land;
    }

    public void onScreenUnlock() {
        try {
            Settings.System.putInt(cResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        } catch (Exception ignored) { }

        needsImmediateUpdate = true;
        startListening();
       }

    public int getLastSensorValue() {
        return (int) lux;
    }

    public int getSetBrightness() {
        return tempBrightness;
    }
}
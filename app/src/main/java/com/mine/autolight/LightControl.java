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

    private void applyDesiredBrightness(int desired, boolean alwaysMode) {

        if (lastAppliedBrightness < 0) {
            writeSystemBrightness(desired);
            lastAppliedBrightness = desired;
            resetDimDebounceIfNeeded(/*current=*/desired, /*desired=*/desired);
            return;
        }

        final int current = alwaysMode ? getCurrentSystemBrightness() : lastAppliedBrightness;

        if (shouldApplyWithHysteresis(current, desired, alwaysMode)) {
            writeSystemBrightness(desired);
            lastAppliedBrightness = desired;
            resetDimDebounceIfNeeded(current, desired);
        } else {
            resetDimDebounceIfNeeded(current, desired);
        }
    }

    private boolean shouldApplyWithHysteresis(int current, int desired, boolean alwaysMode) {
        if (current < 0) return true;

        final int upTh = sett.upThreshold;
        final int downTh = sett.downThreshold;
        final long dimMs = sett.dimDebounceMs;

        if (desired > current) {
            return desired - current >= upTh;
        } else if (desired < current) {
            if (alwaysMode && dimMs > 0) {
                final long now = SystemClock.elapsedRealtime();
                if (firstDimRequestAt == 0L) {
                    firstDimRequestAt = now;
                    return false;
                }
                if (now - firstDimRequestAt < dimMs) {
                    return false;
                }
            }
            return current - desired >= downTh;
        }

        return false;
    }

    private void resetDimDebounceIfNeeded(int current, int desired) {
        if (desired >= current) {
            firstDimRequestAt = 0L;
        }
    }

    private void writeSystemBrightness(int brightness) {
        try {
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
        } catch (Exception ignored) { }
    }

    private int getCurrentSystemBrightness() {
        try {
            return Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Exception e) {
            return -1;
        }
    }

    private static class SensorReading {
        final long time;
        final float value;

        SensorReading(long time, float value) {
            this.time = time;
            this.value = value;
        }
    }
}

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
    private int lastAppliedBrightness = -1;
    private long firstDimRequestAt = 0L;

    private final ArrayDeque<SensorReading> buffer = new ArrayDeque<>();
    private final long WINDOW_MS = 3000;

    private final float HYSTERESIS_THRESHOLD = 0.15f;

    private float lastAppliedLux = -1f;
    private float rollingSum = 0f;

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

        buffer.addLast(new SensorReading(now, rawLux));
        rollingSum += rawLux;

        while (!buffer.isEmpty() && (now - buffer.peekFirst().time) > WINDOW_MS) {
            SensorReading old = buffer.removeFirst();
            rollingSum -= old.value;
        }

        if (needsImmediateUpdate || sett.mode == Constants.WORK_MODE_UNLOCK) {
            lux = rawLux;
            setBrightnessFromLux((int) lux);

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

        if (lastAppliedLux == -1f) {
            lux = buffer.peekLast().value;
            applyAndRecord(lux);
            return;
        }

        float averageLux = rollingSum / buffer.size();
        float diff = Math.abs(averageLux - lastAppliedLux);

        if (diff > (lastAppliedLux * HYSTERESIS_THRESHOLD) || diff > 5f) {
            lux = averageLux;
            applyAndRecord(lux);
        }
    }

    private void applyAndRecord(float luxVal) {
        setBrightnessFromLux((int) luxVal);
        lastAppliedLux = luxVal;
    }

    public void prepareForScreenOn() {
        needsImmediateUpdate = true;
        lastAppliedLux = -1f;

        buffer.clear();
        rollingSum = 0f;

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

    public int getLastSensorValue() { return (int) lux; }
    public int getSetBrightness() { return lastAppliedBrightness >= 0 ? lastAppliedBrightness : 0; }

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

    private void setBrightnessFromLux(int luxValue) {
        int desired = computeBrightnessFromLux(luxValue);
        boolean isAlwaysMode = (sett.mode == Constants.WORK_MODE_ALWAYS);
        applyDesiredBrightness(desired, isAlwaysMode);
    }

    private int computeBrightnessFromLux(int luxValue) {
        int brightness;

        if (luxValue <= sett.l1) {
            brightness = sett.b1;
        } else if (luxValue >= sett.l4) {
            brightness = sett.b4;
        } else {
            float x1, y1, x2, y2;

            if (luxValue <= sett.l2) {
                x1 = sett.l1; x2 = sett.l2; y1 = sett.b1; y2 = sett.b2;
            } else if (luxValue <= sett.l3) {
                x1 = sett.l2; x2 = sett.l3; y1 = sett.b2; y2 = sett.b3;
            } else {
                x1 = sett.l3; x2 = sett.l4; y1 = sett.b3; y2 = sett.b4;
            }

            double lx = Math.log10((double) luxValue + 1.0);
            double lx1 = Math.log10((double) x1 + 1.0);
            double lx2 = Math.log10((double) x2 + 1.0);

            double t = (lx2 - lx1 == 0) ? 0 : (lx - lx1) / (lx2 - lx1);
            t = Math.max(0.0, Math.min(1.0, t));

            brightness = (int) Math.round(y1 + (y2 - y1) * t);
        }

        if (brightness < 0)   brightness = 0;
        if (brightness > 255) brightness = 255;

        return brightness;
    }

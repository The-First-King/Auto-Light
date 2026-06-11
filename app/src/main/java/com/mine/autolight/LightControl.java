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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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

    private MedianFilter medianFilter; 
    private long quickReactTriggerTime = 0;
    private long quickReactDebounceMs; 
    private boolean isQuickReactPending = false;

    LightControl(Context context) {
        this.context = context;
        sett = new MySettings(context);
        cResolver = context.getContentResolver();
        sMgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sMgr.getDefaultSensor(Sensor.TYPE_LIGHT);
        medianFilter = new MedianFilter(getDynamicWindow(sett.envFilterLevel));
        quickReactDebounceMs = sett.getDebounceMs();
    }

    private long getDynamicWindow(int level) {
        switch (level) {
            case 0: return 500;   // Low
            case 1: return 2000;  // Med
            case 2: return 5000;  // High
            default: return 2000;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT) return;

        float rawLux = event.values[0];
        long now = SystemClock.elapsedRealtime();

        // If light goes dark (raw lux < 2.0), reset filters to allow instant dimming.
        float filteredLux;
        if (rawLux < 2.0f) {
            medianFilter.clear();
            buffer.clear();
            rollingSum = 0f;
            filteredLux = rawLux;
        } else {
            filteredLux = medianFilter.filter(rawLux, now);
        }

        if (lastAppliedLux != -1f && !needsImmediateUpdate) {
            float gap = Math.abs(filteredLux - lastAppliedLux);
            float percentChange = (lastAppliedLux == 0f) ? 100f : (gap / lastAppliedLux) * 100f;
            
			// Debounced Quick React processing
            if (gap > sett.quickReactLux && percentChange > sett.quickReactPercent) {
                if (!isQuickReactPending) {
                    quickReactTriggerTime = now;
                    isQuickReactPending = true;
                } else if (now - quickReactTriggerTime >= quickReactDebounceMs) {
					// If the massive change has sustained, clear smoothing buffer and snap immediately.
                    buffer.clear();
                    buffer.addLast(new SensorReading(now, filteredLux));
                    rollingSum = filteredLux;
                    applyAndRecord(filteredLux);
                    isQuickReactPending = false;
                    return;
                }
            } else {
                isQuickReactPending = false;
            }
        } else {
            isQuickReactPending = false;
        }

		// Standard Window Smoothing using the Filtered Lux
        buffer.addLast(new SensorReading(now, filteredLux));
        rollingSum += filteredLux;

        while (!buffer.isEmpty() && (now - buffer.peekFirst().time) > sett.windowMs) {
            SensorReading old = buffer.removeFirst();
            rollingSum -= old.value;
        }

        if (needsImmediateUpdate || sett.mode == Constants.WORK_MODE_UNLOCK) {
            lux = filteredLux;
            setBrightness((int) lux);
            needsImmediateUpdate = false;
            if (sett.mode == Constants.WORK_MODE_UNLOCK) stopListening();
            return;
        }

        processSmoothedLux();
    }

    private void processSmoothedLux() {
        if (buffer.isEmpty()) return;
        float averageLux = rollingSum / buffer.size();
        float diff = Math.abs(averageLux - lastAppliedLux);

        if (lastAppliedLux == -1f || diff > (lastAppliedLux * sett.hysteresisThreshold) || diff > sett.absoluteThreshold) {
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
        medianFilter.clear();
        isQuickReactPending = false;
    }

    private void scheduleSuspend() {
        if (sett.mode == Constants.WORK_MODE_ALWAYS) return;
        delayer.removeCallbacksAndMessages(null);
        delayer.postDelayed(this::stopListening, pause);
    }

    public void startListening() {
        boolean shouldActivate = (sett.mode == Constants.WORK_MODE_ALWAYS || sett.mode == Constants.WORK_MODE_UNLOCK || (sett.mode == Constants.WORK_MODE_LANDSCAPE && landscape) || (sett.mode == Constants.WORK_MODE_PORTRAIT && !landscape) || needsImmediateUpdate);

        if (shouldActivate) {
            delayer.removeCallbacksAndMessages(null);
            if (!onListen && lightSensor != null) {
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
        int finalSystemValue = Math.max(1, Math.min(255, Math.round((brightnessPercent / 100.0f) * 255)));
        try { Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, finalSystemValue);
		} catch (Exception ignored) { }
    }

    public void reconfigure() {
        stopListening();
        sett.load();
        medianFilter = new MedianFilter(getDynamicWindow(sett.envFilterLevel));
        quickReactDebounceMs = sett.getDebounceMs();
        startListening();
    }

    public void setLandscape(boolean land) { this.landscape = land; }

    public void onScreenUnlock() {
        try { Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
		} catch (Exception ignored) { }
        needsImmediateUpdate = true;
        startListening();
    }

    private static class SensorReading {
        final long time; final float value;
        SensorReading(long time, float value) { this.time = time; this.value = value; }
    }

    private static class MedianFilter {
        private final long windowDurationMs;
        private final LinkedList<SensorReading> window = new LinkedList<>();
        MedianFilter(long windowDurationMs) { this.windowDurationMs = windowDurationMs; }
        float filter(float newValue, long currentTime) {
            window.addLast(new SensorReading(currentTime, newValue));
            while (!window.isEmpty() && (currentTime - window.peekFirst().time) > windowDurationMs) window.removeFirst();
            List<Float> sortedValues = new ArrayList<>(window.size());
            for (SensorReading reading : window) sortedValues.add(reading.value);
            Collections.sort(sortedValues);
            int middle = sortedValues.size() / 2;
            return (sortedValues.size() % 2 == 1) ? sortedValues.get(middle) : (sortedValues.get(middle - 1) + sortedValues.get(middle)) / 2.0f;
        }
        void clear() { window.clear(); }
    }
}

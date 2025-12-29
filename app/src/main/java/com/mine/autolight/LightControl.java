package com.mine.autolight;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import java.util.LinkedList;

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

    private final LinkedList<SensorReading> buffer = new LinkedList<>();
    private final long WINDOW_MS = 3000;
    private final float HYSTERESIS_THRESHOLD = 0.15f;
    private float lastAppliedLux = -1;

    LightControl(Context context) {
        sett = new MySettings(context);
        cResolver = context.getContentResolver();
        sMgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sMgr.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {}

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float rawLux = event.values[0];
            long now = System.currentTimeMillis();

            buffer.addLast(new SensorReading(now, rawLux));
            while (!buffer.isEmpty() && (now - buffer.getFirst().time) > WINDOW_MS) {
                buffer.removeFirst();
            }

            // If we are in "Immediate" mode (Unlock/Rotate/Screen-On)
            if (needsImmediateUpdate || sett.mode == Constants.WORK_MODE_UNLOCK) {
                lux = rawLux;
                setBrightness((int) lux);
                
                // IMPORTANT: If it's a one-shot mode, stop after the first valid reading
                if (sett.mode == Constants.WORK_MODE_UNLOCK) {
                    needsImmediateUpdate = false;
                    stopListening();
                } else if (needsImmediateUpdate) {
                    // This handles the Screen-On snap for continuous modes
                    needsImmediateUpdate = false; 
                }
            } else {
                processSmoothedLux();
            }
        }
    }

    private void processSmoothedLux() {
        if (lastAppliedLux == -1 && !buffer.isEmpty()) {
            lux = buffer.getLast().value;
            applyAndRecord(lux);
            return;
        }

        float sum = 0;
        for (SensorReading r : buffer) sum += r.value;
        float averageLux = sum / buffer.size();

        float diff = Math.abs(averageLux - lastAppliedLux);
        if (diff > (lastAppliedLux * HYSTERESIS_THRESHOLD) || diff > 5) {
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
        lastAppliedLux = -1;
        buffer.clear();
        startListening(); 
    }

    private void scheduleSuspend() {
        // ALWAYS mode never suspends
        if (sett.mode == Constants.WORK_MODE_ALWAYS) return;
        
        // Portrait/Landscape modes stay active as long as orientation matches
        if (sett.mode == Constants.WORK_MODE_PORTRAIT && !landscape) return;
        if (sett.mode == Constants.WORK_MODE_LANDSCAPE && landscape) return;

        // Otherwise (Unlock mode or wrong orientation), stop after the delay
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
            // This enables the "one-shot" behavior for Screen-On and Rotation
            shouldActivate = true; 
        }

        if (shouldActivate) {
            delayer.removeCallbacksAndMessages(null);
            if (!onListen && lightSensor != null) {
                // Clear state for a fresh start
                if (sett.mode == Constants.WORK_MODE_UNLOCK || needsImmediateUpdate) {
                    lastAppliedLux = -1;
                    buffer.clear();
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
        } catch (Exception ignored) {}
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
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        } catch (Exception ignored) {}
        needsImmediateUpdate = true; 
        startListening();
    }

    public int getLastSensorValue() { return (int) lux; }
    public int getSetBrightness() { return tempBrightness; }

    private static class SensorReading {
        long time;
        float value;
        SensorReading(long time, float value) { this.time = time; this.value = value; }
    }
}

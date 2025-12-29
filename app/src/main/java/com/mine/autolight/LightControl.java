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

    private float lux = 0;
    private int tempBrightness = 0;

    // --- Smoothing Variables ---
    private final LinkedList<SensorReading> buffer = new LinkedList<>();
    private final long WINDOW_MS = 2000; // 2 second smoothing window
    private float lastAppliedLux = -1; 
    private final float HYSTERESIS_THRESHOLD = 0.10f; // 10% change required

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

            // 1. Add to buffer
            buffer.addLast(new SensorReading(now, rawLux));
            
            // 2. Prune old data
            while (!buffer.isEmpty() && (now - buffer.getFirst().time) > WINDOW_MS) {
                buffer.removeFirst();
            }

            // 3. Handle Work Modes & Logic
            if (sett.mode == Constants.WORK_MODE_UNLOCK) {
                // For "Unlock" mode, we want immediate action, no smoothing.
                lux = rawLux;
                setBrightness((int) lux);
                stopListening();
            } else {
                // For "Always" or Orientation modes, use Smoothing + Hysteresis
                processSmoothedLux(now);
            }
        }
    }

    private void processSmoothedLux(long now) {
        // If it's the very first reading after starting, apply immediately
        if (lastAppliedLux == -1) {
            lux = buffer.getLast().value;
            applyAndRecord(lux);
            return;
        }

        // Calculate average of the 2-second window
        float sum = 0;
        for (SensorReading r : buffer) sum += r.value;
        float averageLux = sum / buffer.size();

        // Hysteresis: Only update if change is > 10%
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

    private void scheduleSuspend() {
        if (sett.mode == Constants.WORK_MODE_ALWAYS) return;
        if (sett.mode == Constants.WORK_MODE_LANDSCAPE && landscape) return;
        if (sett.mode == Constants.WORK_MODE_PORTRAIT && !landscape) return;

        delayer.removeCallbacksAndMessages(null);
        delayer.postDelayed(this::stopListening, pause);
    }

    public void startListening() {
        boolean shouldActivate = false;

        if (sett.mode == Constants.WORK_MODE_ALWAYS) shouldActivate = true;
        else if (sett.mode == Constants.WORK_MODE_LANDSCAPE && landscape) shouldActivate = true;
        else if (sett.mode == Constants.WORK_MODE_PORTRAIT && !landscape) shouldActivate = true;
        else if (sett.mode == Constants.WORK_MODE_UNLOCK) shouldActivate = true;

        if (shouldActivate) {
            delayer.removeCallbacksAndMessages(null);
            
            if (!onListen && lightSensor != null) {
                // Reset smoothing state on every fresh start
                buffer.clear();
                lastAppliedLux = -1;
                
                sMgr.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
                onListen = true;
            }
            scheduleSuspend();
        }
    }

    public void stopListening() {
        if (onListen) {
            sMgr.unregisterListener(this);
            onListen = false;
        }
        delayer.removeCallbacksAndMessages(null);
        buffer.clear(); // Clear buffer to stay memory efficient
    }

    private void setBrightness(int luxValue) {
        int brightness;
        // --- YOUR ORIGINAL LOGIC PRESERVED EXACTLY ---
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

    // ... Remaining helper methods (reconfigure, setLandscape, etc.) stay the same ...
    
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
        
        onListen = false; 
        startListening();
    }

    public int getLastSensorValue() { return (int) lux; }
    public int getSetBrightness() { return tempBrightness; }

    // Inner class for buffer tracking
    private static class SensorReading {
        long time;
        float value;
        SensorReading(long time, float value) { this.time = time; this.value = value; }
    }
}

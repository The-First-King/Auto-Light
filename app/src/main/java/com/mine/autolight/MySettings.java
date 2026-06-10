package com.mine.autolight;

import android.content.Context;
import android.content.SharedPreferences;

public class MySettings {

    private final Context context;
    private SharedPreferences sharedPref;

    public int l1, l2, l3, l4, b1, b2, b3, b4;
    public int mode;
    
    public float hysteresisThreshold;
    public int absoluteThreshold;
    public int windowMs;
    public int quickReactLux;
    public int quickReactPercent;
    
    // Environment Filtering Level (0=Low, 1=Medium, 2=High)
    public int envFilterLevel; 

    MySettings(Context context) {
        this.context = context;
        load();
    }

    public void load() {
        sharedPref = context.getSharedPreferences(Constants.SETTINGS_PREFS_NAME, Context.MODE_PRIVATE);

        l1 = sharedPref.getInt("l1", 1);
        l2 = sharedPref.getInt("l2", 1000);
        l3 = sharedPref.getInt("l3", 10000);
        l4 = sharedPref.getInt("l4", 100000);

        b1 = sharedPref.getInt("b1", 1);
        b2 = sharedPref.getInt("b2", 15);
        b3 = sharedPref.getInt("b3", 30);
        b4 = sharedPref.getInt("b4", 60);

        mode = sharedPref.getInt("mode", Constants.WORK_MODE_UNLOCK);
        
        hysteresisThreshold = sharedPref.getFloat("hysteresisThreshold", 0.15f);
        absoluteThreshold = sharedPref.getInt("absoluteThreshold", 5);
        windowMs = sharedPref.getInt("windowMs", 3000);
        quickReactLux = sharedPref.getInt("quickReactLux", 50);
        quickReactPercent = sharedPref.getInt("quickReactPercent", 50);
        envFilterLevel = sharedPref.getInt("envFilterLevel", 1); 
    }

    public void save() {
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putInt("l1", l1);
        editor.putInt("l2", l2);
        editor.putInt("l3", l3);
        editor.putInt("l4", l4);

        editor.putInt("b1", b1);
        editor.putInt("b2", b2);
        editor.putInt("b3", b3);
        editor.putInt("b4", b4);

        editor.putInt("mode", mode);
        
        editor.putFloat("hysteresisThreshold", hysteresisThreshold);
        editor.putInt("absoluteThreshold", absoluteThreshold);
        editor.putInt("windowMs", windowMs);
        editor.putInt("quickReactLux", quickReactLux);
        editor.putInt("quickReactPercent", quickReactPercent);
        editor.putInt("envFilterLevel", envFilterLevel);

        editor.apply();
    }

    // Helpers for LightControl
    public long getMedianWindowMs() {
        switch (envFilterLevel) {
            case 0: return 1000; // Low
            case 2: return 3000; // High
            case 1: 
            default: return 2000; // Medium
        }
    }

    public long getDebounceMs() {
        switch (envFilterLevel) {
            case 0: return 600;  // Low
            case 2: return 2000; // High
            case 1: 
            default: return 1200; // Medium
        }
    }
}

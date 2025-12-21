package com.mine.autolight;

import android.content.Context;
import android.content.SharedPreferences;

public class MySettings {
    private final SharedPreferences prefs;
    public int l1, l2, l3, l4, b1, b2, b3, b4, mode;

    public MySettings(Context context) {
        prefs = context.getSharedPreferences("autolight_prefs", Context.MODE_PRIVATE);
        load();
    }

    public void load() {
        l1 = prefs.getInt("l1", 10);
        l2 = prefs.getInt("l2", 100);
        l3 = prefs.getInt("l3", 500);
        l4 = prefs.getInt("l4", 1000);
        b1 = prefs.getInt("b1", 30);
        b2 = prefs.getInt("b2", 80);
        b3 = prefs.getInt("b3", 150);
        b4 = prefs.getInt("b4", 255);
        mode = prefs.getInt("mode", Constants.WORK_MODE_ALWAYS);
        
        // Sync the static map for the algorithm
        BrightnessAlgorithm.setPoints(l1, b1, l2, b2, l3, b3, l4, b4);
    }

    public void save() {
        prefs.edit()
            .putInt("l1", l1).putInt("l2", l2).putInt("l3", l3).putInt("l4", l4)
            .putInt("b1", b1).putInt("b2", b2).putInt("b3", b3).putInt("b4", b4)
            .putInt("mode", mode)
            .apply();
        BrightnessAlgorithm.setPoints(l1, b1, l2, b2, l3, b3, l4, b4);
    }
}

package com.mine.autolight;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;

public class MySettings {
    private final Context context;
    private SharedPreferences sharedPref;
    
    // These must be public for the algorithm to see them
    public int l1, l2, l3, l4, b1, b2, b3, b4;
    public int mode;

    // This is what the BrightnessAlgorithm uses to "see" your points
    public static HashMap<Integer, Integer> points = new HashMap<>();

    public MySettings(Context context) {
        this.context = context;
        load();
    }

    public void load() {
        // Use MODE_PRIVATE. Note: MODE_MULTI_PROCESS is deprecated in modern Android, 
        // but it's okay for older devices.
        sharedPref = context.getSharedPreferences("mine.autolight", Context.MODE_PRIVATE);
        
        l1 = sharedPref.getInt("l1", 10);
        l2 = sharedPref.getInt("l2", 100);
        l3 = sharedPref.getInt("l3", 1000);
        l4 = sharedPref.getInt("l4", 10000);
        
        b1 = sharedPref.getInt("b1", 10); // Changed from 1 to 10 for better visibility
        b2 = sharedPref.getInt("b2", 80);
        b3 = sharedPref.getInt("b3", 160);
        b4 = sharedPref.getInt("b4", 255);
        
        mode = sharedPref.getInt("mode", 0); // Default mode

        // CRITICAL: Fill the hashmap so the algorithm can use it
        updatePointsMap();
    }

    private void updatePointsMap() {
        points.clear();
        points.put(l1, b1);
        points.put(l2, b2);
        points.put(l3, b3);
        points.put(l4, b4);
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
        editor.apply(); // apply() is faster and safer than commit()
        
        // Refresh the map after saving
        updatePointsMap();
    }
}

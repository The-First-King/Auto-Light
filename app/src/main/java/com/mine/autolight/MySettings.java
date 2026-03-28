package com.mine.autolight;

import android.content.Context;
import android.content.SharedPreferences;

public class MySettings {

    private final Context context;
    private SharedPreferences sharedPref;
    public int l1, l2, l3, l4, b1, b2, b3, b4;
    public int mode;
    public int  upThreshold;
    public int  downThreshold;
    public long dimDebounceMs;

    MySettings(Context context) {
        this.context = context.getApplicationContext();
        load();
    }

    public void load() {
        sharedPref = context.getSharedPreferences(Constants.SETTINGS_PREFS_NAME, Context.MODE_PRIVATE);

        l1 = sharedPref.getInt("l1", 1);
        l2 = sharedPref.getInt("l2", 100);
        l3 = sharedPref.getInt("l3", 1000);
        l4 = sharedPref.getInt("l4", 10000);

        b1 = sharedPref.getInt("b1", 1);
        b2 = sharedPref.getInt("b2", 10);
        b3 = sharedPref.getInt("b3", 30);
        b4 = sharedPref.getInt("b4", 90);

        mode = sharedPref.getInt("mode", Constants.WORK_MODE_UNLOCK);

        upThreshold = clampInt(
                sharedPref.getInt("pref_up_threshold", Constants.DEFAULT_BRIGHTNESS_UP_THRESHOLD),
                Constants.MIN_BRIGHTNESS_THRESHOLD,
                Constants.MAX_BRIGHTNESS_THRESHOLD
        );

        downThreshold = clampInt(
                sharedPref.getInt("pref_down_threshold", Constants.DEFAULT_BRIGHTNESS_DOWN_THRESHOLD),
                Constants.MIN_BRIGHTNESS_THRESHOLD,
                Constants.MAX_BRIGHTNESS_THRESHOLD
        );

        dimDebounceMs = clampLong(
                sharedPref.getLong("pref_dim_debounce_ms", Constants.DEFAULT_DIM_DEBOUNCE_MS),
                Constants.MIN_DIM_DEBOUNCE_MS,
                Constants.MAX_DIM_DEBOUNCE_MS
        );
    }

    public void save() {

        upThreshold    = clampInt(upThreshold, Constants.MIN_BRIGHTNESS_THRESHOLD, Constants.MAX_BRIGHTNESS_THRESHOLD);
        downThreshold  = clampInt(downThreshold, Constants.MIN_BRIGHTNESS_THRESHOLD, Constants.MAX_BRIGHTNESS_THRESHOLD);
        dimDebounceMs  = clampLong(dimDebounceMs, Constants.MIN_DIM_DEBOUNCE_MS, Constants.MAX_DIM_DEBOUNCE_MS);

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
        editor.putInt("pref_up_threshold", upThreshold);
        editor.putInt("pref_down_threshold", downThreshold);
        editor.putLong("pref_dim_debounce_ms", dimDebounceMs);

        editor.apply();
    }

    public static int getUpThreshold(Context c) {
        SharedPreferences p = c.getSharedPreferences(Constants.SETTINGS_PREFS_NAME, Context.MODE_PRIVATE);
        int v = p.getInt("pref_up_threshold", Constants.DEFAULT_BRIGHTNESS_UP_THRESHOLD);
        return clampInt(v, Constants.MIN_BRIGHTNESS_THRESHOLD, Constants.MAX_BRIGHTNESS_THRESHOLD);
    }

    public static void setUpThreshold(Context c, int value) {
        int v = clampInt(value, Constants.MIN_BRIGHTNESS_THRESHOLD, Constants.MAX_BRIGHTNESS_THRESHOLD);
        c.getSharedPreferences(Constants.SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt("pref_up_threshold", v)
                .apply();
    }

    public static int getDownThreshold(Context c) {
        SharedPreferences p = c.getSharedPreferences(Constants.SETTINGS_PREFS_NAME, Context.MODE_PRIVATE);
        int v = p.getInt("pref_down_threshold", Constants.DEFAULT_BRIGHTNESS_DOWN_THRESHOLD);
        return clampInt(v, Constants.MIN_BRIGHTNESS_THRESHOLD, Constants.MAX_BRIGHTNESS_THRESHOLD);
    }

    public static void setDownThreshold(Context c, int value) {
        int v = clampInt(value, Constants.MIN_BRIGHTNESS_THRESHOLD, Constants.MAX_BRIGHTNESS_THRESHOLD);
        c.getSharedPreferences(Constants.SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt("pref_down_threshold", v)
                .apply();
    }

    public static long getDimDebounceMs(Context c) {
        SharedPreferences p = c.getSharedPreferences(Constants.SETTINGS_PREFS_NAME, Context.MODE_PRIVATE);
        long v = p.getLong("pref_dim_debounce_ms", Constants.DEFAULT_DIM_DEBOUNCE_MS);
        return clampLong(v, Constants.MIN_DIM_DEBOUNCE_MS, Constants.MAX_DIM_DEBOUNCE_MS);
    }

    public static void setDimDebounceMs(Context c, long value) {
        long v = clampLong(value, Constants.MIN_DIM_DEBOUNCE_MS, Constants.MAX_DIM_DEBOUNCE_MS);
        c.getSharedPreferences(Constants.SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong("pref_dim_debounce_ms", v)
                .apply();
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static long clampLong(long v, long min, long max) {
        return Math.max(min, Math.min(max, v));
    }
}

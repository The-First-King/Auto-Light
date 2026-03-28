package com.mine.autolight;

public final class Constants {

    private Constants() {}

    public static final int WORK_MODE_ALWAYS = 1;
    public static final int WORK_MODE_PORTRAIT = 2;
    public static final int WORK_MODE_UNLOCK = 3;
    public static final int WORK_MODE_LANDSCAPE = 4;
    public static final int SERVICE_INTENT_PAYLOAD_PING = 0;
    public static final int SERVICE_INTENT_PAYLOAD_SET  = 1;
    public static final String PREFS_NAME = "AutoLightPrefs";
    public static final String PREF_ENABLED_KEY = "service_enabled_by_user";
    public static final String SERVICE_INTENT_ACTION = "com.mine.autolight.ACTION_LIGHT_COMMAND";
    public static final String SERVICE_INTENT_EXTRA = "com.mine.autolight.EXTRA_COMMAND";
    public static final String SETTINGS_PREFS_NAME = "mine.autolight";
	
    // Defaults for the brightness hysteresis to help avoid visible flicker
	public static final int  DEFAULT_BRIGHTNESS_UP_THRESHOLD = 4;
	public static final int  DEFAULT_BRIGHTNESS_DOWN_THRESHOLD = 6;
	public static final long DEFAULT_DIM_DEBOUNCE_MS = 750L;
	
	// Validation bounds for user's brightness hysteresis configuration
    public static final int  MIN_BRIGHTNESS_THRESHOLD = 0;
    public static final int  MAX_BRIGHTNESS_THRESHOLD = 48;
    public static final long MIN_DIM_DEBOUNCE_MS = 0L;
    public static final long MAX_DIM_DEBOUNCE_MS = 5000L;
}

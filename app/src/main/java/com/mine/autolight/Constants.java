package com.mine.autolight;

public final class Constants {

    private Constants() {}

    public static final int WORK_MODE_ALWAYS = 1;
    public static final int WORK_MODE_PORTRAIT = 2;
    public static final int WORK_MODE_UNLOCK = 3;
    public static final int WORK_MODE_LANDSCAPE = 4;

    // Best practice: fully-qualified action & extra names (avoid collisions)
    public static final String SERVICE_INTENT_ACTION = "com.mine.autolight.ACTION_LIGHT_COMMAND";
    public static final String SERVICE_INTENT_EXTRA = "com.mine.autolight.EXTRA_COMMAND";

    public static final int SERVICE_INTENT_PAYLOAD_PING = 0;
    public static final int SERVICE_INTENT_PAYLOAD_SET  = 1;

    // Keep the user-enabled preference centralized
    public static final String PREFS_NAME = "AutoLightPrefs";
    public static final String PREF_ENABLED_KEY = "service_enabled_by_user";

    // Settings storage file for curve/mode
    public static final String SETTINGS_PREFS_NAME = "mine.autolight";
}

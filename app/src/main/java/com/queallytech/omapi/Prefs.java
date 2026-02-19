package com.queallytech.omapi;

public final class Prefs {
    private Prefs() {
    }

    public static final String PREF_FILE = "omapi_settings";

    public static final String ACTION_RECORD_RECENT_CALL = "com.queallytech.omapi.action.RECORD_RECENT_CALL";
    public static final String EXTRA_CALLER_PACKAGE = "extra_caller_package";
    public static final String EXTRA_ARAM_HASH = "extra_aram_hash";
    public static final int MAX_RECENT_CALLS = 10;

    public static final String KEY_VERBOSE_LOG = "verbose_log";
    public static final String KEY_TARGET_APP = "target_app";
    public static final String KEY_TARGET_HASH = "target_hash";
    public static final String KEY_RECENT_CALLS = "recent_calls";
}

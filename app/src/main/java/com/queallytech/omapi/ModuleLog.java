package com.queallytech.omapi;

import de.robv.android.xposed.XposedBridge;

public final class ModuleLog {

    private static final String LOG_TAG = "OMAPI-Bypass";

    private ModuleLog() {
    }

    public static void info(String message) {
        XposedBridge.log(LOG_TAG + " [I] " + message);
    }

    public static void error(String message, Throwable t) {
        XposedBridge.log(LOG_TAG + " [E] " + message);
        XposedBridge.log(t);
    }
}

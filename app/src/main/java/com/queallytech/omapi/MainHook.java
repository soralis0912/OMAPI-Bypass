package com.queallytech.omapi;

import java.util.LinkedHashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String LOG_TAG = "OMAPI-Bypass";
    private static final String TARGET_PACKAGE = "com.android.se";
    private static final String TARGET_CLASS = "com.android.se.security.AccessControlEnforcer";
    private static final String TARGET_METHOD_SET_UP_CHANNEL_ACCESS = "setUpChannelAccess";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        try {
            Class<?> clazz = XposedHelpers.findClass(TARGET_CLASS, lpparam.classLoader);
            XposedBridge.hookAllMethods(clazz, TARGET_METHOD_SET_UP_CHANNEL_ACCESS, buildBypassHook());
            logInfo("Hook registered: setUpChannelAccess");
        } catch (Throwable t) {
            logError("Failed to hook AccessControlEnforcer", t);
        }
    }

    private static XC_MethodHook buildBypassHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    // Reset per-call flags first to avoid leaking bypass state to the next caller.
                    applyDefaultAccessFlags(param.thisObject);

                    XSharedPreferences preferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, Prefs.PREF_FILE);
                    preferences.reload();

                    String rawApps = safeTrim(preferences.getString(Prefs.KEY_TARGET_APP, ""));
                    Set<String> targetApps = parseList(rawApps);
                    if (targetApps.isEmpty()) {
                        return;
                    }

                    String callerPackage = resolveCallerPackage(param.args);
                    if (isEmpty(callerPackage) || !targetApps.contains(callerPackage)) {
                        return;
                    }

                    applyBypassAccessFlags(param.thisObject);

                    if (preferences.getBoolean(Prefs.KEY_VERBOSE_LOG, false)) {
                        XposedBridge.log(LOG_TAG + " [I] Bypass applied: CALLER=" + callerPackage);
                    }
                } catch (Throwable t) {
                    logError("beforeHookedMethod failed", t);
                }
            }
        };
    }

    private static void applyDefaultAccessFlags(Object accessControlEnforcer) {
        XposedHelpers.setBooleanField(accessControlEnforcer, "mUseArf", true);
        XposedHelpers.setBooleanField(accessControlEnforcer, "mUseAra", true);
        XposedHelpers.setBooleanField(accessControlEnforcer, "mFullAccess", false);
    }

    private static void applyBypassAccessFlags(Object accessControlEnforcer) {
        XposedHelpers.setBooleanField(accessControlEnforcer, "mUseArf", false);
        XposedHelpers.setBooleanField(accessControlEnforcer, "mUseAra", false);
        XposedHelpers.setBooleanField(accessControlEnforcer, "mFullAccess", true);
    }

    private static String resolveCallerPackage(Object[] args) {
        if (args == null || args.length < 2) {
            return "";
        }
        Object value = args[1];
        if (!(value instanceof String)) {
            return "";
        }
        String packageName = safeTrim((String) value);
        return looksLikePackageName(packageName) ? packageName : "";
    }

    private static Set<String> parseList(String raw) {
        Set<String> items = new LinkedHashSet<>();
        if (raw == null || raw.isEmpty()) {
            return items;
        }

        String[] lines = raw.split("\\n");
        for (String line : lines) {
            String value = safeTrim(line);
            if (!isEmpty(value)) {
                items.add(value);
            }
        }
        return items;
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private static boolean looksLikePackageName(String value) {
        if (isEmpty(value)) {
            return false;
        }
        if (!value.contains(".")) {
            return false;
        }
        return value.matches("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$");
    }

    private static void logInfo(String message) {
        XposedBridge.log(LOG_TAG + " [I] " + message);
    }

    private static void logError(String message, Throwable t) {
        XposedBridge.log(LOG_TAG + " [E] " + message);
        XposedBridge.log(t);
    }
}

package com.queallytech.omapi;

import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

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
            ModuleLog.info("Hook registered: setUpChannelAccess");
        } catch (Throwable t) {
            ModuleLog.error("Failed to hook AccessControlEnforcer", t);
        }
    }

    private static XC_MethodHook buildBypassHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    // Reset per-call flags first to avoid leaking bypass state to the next caller.
                    AccessControlUtils.applyDefaultAccessFlags(param.thisObject);

                    XSharedPreferences preferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, Prefs.PREF_FILE);
                    preferences.reload();

                    String rawApps = Utils.safeTrim(preferences.getString(Prefs.KEY_TARGET_APP, ""));
                    Set<String> targetApps = Utils.parseNonEmptyLines(rawApps);
                    if (targetApps.isEmpty()) {
                        return;
                    }

                    String callerPackage = AccessControlUtils.resolveCallerPackage(param.args);
                    if (Utils.isEmpty(callerPackage) || !targetApps.contains(callerPackage)) {
                        return;
                    }

                    AccessControlUtils.applyBypassAccessFlags(param.thisObject);

                    if (preferences.getBoolean(Prefs.KEY_VERBOSE_LOG, false)) {
                        ModuleLog.info("Bypass applied: CALLER=" + callerPackage);
                    }
                } catch (Throwable t) {
                    ModuleLog.error("beforeHookedMethod failed", t);
                }
            }
        };
    }
}

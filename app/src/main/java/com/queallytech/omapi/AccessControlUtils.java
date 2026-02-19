package com.queallytech.omapi;

import de.robv.android.xposed.XposedHelpers;

public final class AccessControlUtils {

    private AccessControlUtils() {
    }

    public static void applyDefaultAccessFlags(Object accessControlEnforcer) {
        XposedHelpers.setBooleanField(accessControlEnforcer, "mUseArf", true);
        XposedHelpers.setBooleanField(accessControlEnforcer, "mUseAra", true);
        XposedHelpers.setBooleanField(accessControlEnforcer, "mFullAccess", false);
    }

    public static void applyBypassAccessFlags(Object accessControlEnforcer) {
        XposedHelpers.setBooleanField(accessControlEnforcer, "mUseArf", false);
        XposedHelpers.setBooleanField(accessControlEnforcer, "mUseAra", false);
        XposedHelpers.setBooleanField(accessControlEnforcer, "mFullAccess", true);
    }

    public static String resolveCallerPackage(Object[] args) {
        if (args == null || args.length < 2) {
            return "";
        }
        Object value = args[1];
        if (!(value instanceof String)) {
            return "";
        }
        String packageName = Utils.safeTrim((String) value);
        return looksLikePackageName(packageName) ? packageName : "";
    }

    private static boolean looksLikePackageName(String value) {
        if (Utils.isEmpty(value)) {
            return false;
        }
        if (!value.contains(".")) {
            return false;
        }
        return value.matches("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$");
    }
}

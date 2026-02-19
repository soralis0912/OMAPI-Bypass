package org.soralis.omapi.policyhook;

import java.util.regex.Pattern;

import de.robv.android.xposed.XposedHelpers;

public final class AccessControlUtils {

    private static final Pattern PACKAGE_NAME_PATTERN =
            Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$");

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
        return PACKAGE_NAME_PATTERN.matcher(value).matches();
    }
}

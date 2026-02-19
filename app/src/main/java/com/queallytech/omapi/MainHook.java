package com.queallytech.omapi;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Locale;
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
    private static final String TARGET_METHOD = "readSecurityProfile";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(TARGET_CLASS, lpparam.classLoader, TARGET_METHOD, buildBypassHook());
            logInfoIfEnabled("Hook registered");
        } catch (Throwable t) {
            logError("Failed to hook AccessControlEnforcer", t);
        }
    }

    private static XC_MethodHook buildBypassHook() {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                XSharedPreferences preferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, Prefs.PREF_FILE);
                preferences.makeWorldReadable();
                preferences.reload();

                Set<String> targetApps = parseList(preferences.getString(Prefs.KEY_TARGET_APP, ""), false);
                Set<String> targetHashes = parseList(preferences.getString(Prefs.KEY_TARGET_HASH, ""), true);
                String callerPackage = resolveCallerPackage(param.thisObject);
                String araMHash = resolveAraMHash(param.thisObject);

                if (targetApps.isEmpty() && targetHashes.isEmpty()) {
                    return;
                }

                if (!matchesFilter(callerPackage, araMHash, targetApps, targetHashes)) {
                    return;
                }

                XposedHelpers.setBooleanField(param.thisObject, "mUseArf", false);
                XposedHelpers.setBooleanField(param.thisObject, "mUseAra", false);
                XposedHelpers.setBooleanField(param.thisObject, "mFullAccess", true);

                logInfoIfEnabled("Bypass applied: CALLER=" + callerPackage + " ARAM=" + araMHash);
            }
        };
    }

    private static boolean matchesFilter(String callerPackage, String araMHash, Set<String> targetApps, Set<String> targetHashes) {
        boolean appMatched = !isEmpty(callerPackage) && targetApps.contains(callerPackage);
        boolean hashMatched = !isEmpty(araMHash) && targetHashes.contains(araMHash);
        return appMatched || hashMatched;
    }

    private static Set<String> parseList(String raw, boolean normalizeHash) {
        Set<String> items = new LinkedHashSet<>();
        if (raw == null || raw.isEmpty()) {
            return items;
        }

        String[] lines = raw.split("\\n");
        for (String line : lines) {
            String value = safeTrim(line);
            if (normalizeHash) {
                value = normalizeHash(value);
            }
            if (!isEmpty(value)) {
                items.add(value);
            }
        }
        return items;
    }

    private static String resolveCallerPackage(Object accessControlEnforcer) {
        for (String fieldName : new String[]{
                "mPackageName",
                "mCallingPackage",
                "mCallerPackageName",
                "mClientPackageName"
        }) {
            Object value = readField(accessControlEnforcer, fieldName);
            if (value instanceof String) {
                String packageName = safeTrim((String) value);
                if (!isEmpty(packageName)) {
                    return packageName;
                }
            }
        }
        return "";
    }

    private static String resolveAraMHash(Object accessControlEnforcer) {
        for (String fieldName : new String[]{
                "mAraMHash",
                "mAramHash",
                "mCertificateHash",
                "mCertHash",
                "mCallerCertHash"
        }) {
            Object value = readField(accessControlEnforcer, fieldName);
            String hash = normalizeHashObject(value);
            if (!isEmpty(hash)) {
                return hash;
            }
        }
        return "";
    }

    private static Object readField(Object instance, String name) {
        if (instance == null) {
            return null;
        }
        Class<?> clazz = instance.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(instance);
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static String normalizeHashObject(Object value) {
        if (value instanceof byte[]) {
            return bytesToHex((byte[]) value);
        }
        if (value instanceof String) {
            return normalizeHash((String) value);
        }
        return "";
    }

    private static String bytesToHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        final char[] hex = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            out[i * 2] = hex[value >>> 4];
            out[i * 2 + 1] = hex[value & 0x0F];
        }
        return new String(out);
    }

    private static String normalizeHash(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^0-9a-f]", "");
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private static void logInfoIfEnabled(String message) {
        XSharedPreferences preferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, Prefs.PREF_FILE);
        preferences.reload();
        if (preferences.getBoolean(Prefs.KEY_VERBOSE_LOG, false)) {
            XposedBridge.log(LOG_TAG + " [I] " + message);
        }
    }

    private static void logError(String message, Throwable t) {
        XposedBridge.log(LOG_TAG + " [E] " + message);
        XposedBridge.log(t);
    }
}

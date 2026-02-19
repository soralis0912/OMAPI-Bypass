package org.soralis.omapi.policyhook;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

public final class PrefAccess {

    private PrefAccess() {
    }

    public static SharedPreferences openModulePrefs(Context context) {
        try {
            return context.getSharedPreferences(Prefs.PREF_FILE, Context.MODE_WORLD_READABLE);
        } catch (Throwable ignored) {
            return context.getSharedPreferences(Prefs.PREF_FILE, Context.MODE_PRIVATE);
        }
    }

    public static void ensureWorldReadable(Context context) {
        try {
            File prefsFile = new File(context.getApplicationInfo().dataDir
                    + "/shared_prefs/" + Prefs.PREF_FILE + ".xml");
            File dir = prefsFile.getParentFile();
            if (dir != null && dir.exists()) {
                dir.setReadable(true, false);
                dir.setExecutable(true, false);
            }
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false);
            }
        } catch (Throwable ignored) {
        }
    }
}

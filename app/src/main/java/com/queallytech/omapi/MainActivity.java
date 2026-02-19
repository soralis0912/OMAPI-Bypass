package com.queallytech.omapi;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private EditText targetApp;
    private EditText targetHash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSettingsPrefs();

        final Switch bypassEnabled = findViewById(R.id.switch_bypass_enabled);
        final Switch disableArf = findViewById(R.id.switch_disable_arf);
        final Switch disableAra = findViewById(R.id.switch_disable_ara);
        final Switch fullAccess = findViewById(R.id.switch_full_access);
        final Switch verboseLog = findViewById(R.id.switch_verbose_log);
        targetApp = findViewById(R.id.edit_target_app);
        targetHash = findViewById(R.id.edit_target_hash);
        final TextView summary = findViewById(R.id.text_summary);

        bypassEnabled.setChecked(prefs.getBoolean(Prefs.KEY_BYPASS_ENABLED, true));
        disableArf.setChecked(prefs.getBoolean(Prefs.KEY_DISABLE_ARF, true));
        disableAra.setChecked(prefs.getBoolean(Prefs.KEY_DISABLE_ARA, true));
        fullAccess.setChecked(prefs.getBoolean(Prefs.KEY_ENABLE_FULL_ACCESS, true));
        verboseLog.setChecked(prefs.getBoolean(Prefs.KEY_VERBOSE_LOG, false));
        targetApp.setText(prefs.getString(Prefs.KEY_TARGET_APP, ""));
        targetHash.setText(prefs.getString(Prefs.KEY_TARGET_HASH, ""));

        updateSummary(summary, bypassEnabled.isChecked(), disableArf.isChecked(), disableAra.isChecked(), fullAccess.isChecked());

        bypassEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(Prefs.KEY_BYPASS_ENABLED, isChecked).apply();
            makePrefsWorldReadable();
            updateSummary(summary, isChecked, disableArf.isChecked(), disableAra.isChecked(), fullAccess.isChecked());
        });

        disableArf.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(Prefs.KEY_DISABLE_ARF, isChecked).apply();
            makePrefsWorldReadable();
            updateSummary(summary, bypassEnabled.isChecked(), isChecked, disableAra.isChecked(), fullAccess.isChecked());
        });

        disableAra.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(Prefs.KEY_DISABLE_ARA, isChecked).apply();
            makePrefsWorldReadable();
            updateSummary(summary, bypassEnabled.isChecked(), disableArf.isChecked(), isChecked, fullAccess.isChecked());
        });

        fullAccess.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(Prefs.KEY_ENABLE_FULL_ACCESS, isChecked).apply();
            makePrefsWorldReadable();
            updateSummary(summary, bypassEnabled.isChecked(), disableArf.isChecked(), disableAra.isChecked(), isChecked);
        });

        verboseLog.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(Prefs.KEY_VERBOSE_LOG, isChecked).apply();
            makePrefsWorldReadable();
        });

        targetApp.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                prefs.edit().putString(Prefs.KEY_TARGET_APP, targetApp.getText().toString().trim()).apply();
                makePrefsWorldReadable();
            }
        });

        targetHash.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                prefs.edit().putString(Prefs.KEY_TARGET_HASH, targetHash.getText().toString().trim()).apply();
                makePrefsWorldReadable();
            }
        });

        makePrefsWorldReadable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefs.edit()
                .putString(Prefs.KEY_TARGET_APP, targetApp.getText().toString().trim())
                .putString(Prefs.KEY_TARGET_HASH, targetHash.getText().toString().trim())
                .apply();
        makePrefsWorldReadable();
    }

    private SharedPreferences getSettingsPrefs() {
        Context context = createDeviceProtectedStorageContext();
        context.moveSharedPreferencesFrom(this, Prefs.PREF_FILE);
        return context.getSharedPreferences(Prefs.PREF_FILE, MODE_PRIVATE);
    }

    private void makePrefsWorldReadable() {
        Context context = createDeviceProtectedStorageContext();
        File xml = new File(context.getDataDir(), "shared_prefs/" + Prefs.PREF_FILE + ".xml");
        // Best-effort for XSharedPreferences compatibility.
        if (xml.exists()) {
            //noinspection ResultOfMethodCallIgnored
            xml.setReadable(true, false);
        }
    }

    private void updateSummary(TextView summary, boolean bypassEnabled, boolean disableArf,
                               boolean disableAra, boolean fullAccess) {
        if (!bypassEnabled) {
            summary.setText(R.string.summary_disabled);
            return;
        }

        if (disableArf && disableAra && fullAccess) {
            summary.setText(R.string.summary_full);
            return;
        }

        summary.setText(R.string.summary_custom);
    }
}

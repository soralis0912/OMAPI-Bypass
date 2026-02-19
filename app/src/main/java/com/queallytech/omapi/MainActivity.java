package com.queallytech.omapi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private EditText targetAppInput;
    private EditText targetHashInput;
    private TextView targetAppsListText;
    private TextView targetHashesListText;
    private final List<String> targetApps = new ArrayList<>();
    private final List<String> targetHashes = new ArrayList<>();

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
        final Button pickTargetApp = findViewById(R.id.button_pick_target_app);
        final Button addTargetApp = findViewById(R.id.button_add_target_app);
        final Button removeTargetApp = findViewById(R.id.button_remove_target_app);
        final Button addTargetHash = findViewById(R.id.button_add_target_hash);
        final Button removeTargetHash = findViewById(R.id.button_remove_target_hash);
        targetAppInput = findViewById(R.id.edit_target_app);
        targetHashInput = findViewById(R.id.edit_target_hash);
        targetAppsListText = findViewById(R.id.text_target_apps_list);
        targetHashesListText = findViewById(R.id.text_target_hashes_list);
        final TextView summary = findViewById(R.id.text_summary);

        bypassEnabled.setChecked(prefs.getBoolean(Prefs.KEY_BYPASS_ENABLED, true));
        disableArf.setChecked(prefs.getBoolean(Prefs.KEY_DISABLE_ARF, true));
        disableAra.setChecked(prefs.getBoolean(Prefs.KEY_DISABLE_ARA, true));
        fullAccess.setChecked(prefs.getBoolean(Prefs.KEY_ENABLE_FULL_ACCESS, true));
        verboseLog.setChecked(prefs.getBoolean(Prefs.KEY_VERBOSE_LOG, false));
        targetApps.clear();
        targetApps.addAll(parseListPreference(prefs.getString(Prefs.KEY_TARGET_APP, "")));
        targetHashes.clear();
        targetHashes.addAll(parseListPreference(prefs.getString(Prefs.KEY_TARGET_HASH, "")));
        refreshListViews();

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

        pickTargetApp.setOnClickListener(v -> showAppPicker());
        addTargetApp.setOnClickListener(v -> addTargetAppFromInput());
        removeTargetApp.setOnClickListener(v -> showRemoveDialog(targetApps, true));
        addTargetHash.setOnClickListener(v -> addTargetHashFromInput());
        removeTargetHash.setOnClickListener(v -> showRemoveDialog(targetHashes, false));

        makePrefsWorldReadable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        persistTargetLists();
    }

    private SharedPreferences getSettingsPrefs() {
        return getSharedPreferences(Prefs.PREF_FILE, MODE_PRIVATE);
    }

    private void makePrefsWorldReadable() {
        File xml = new File(getApplicationInfo().dataDir, "shared_prefs/" + Prefs.PREF_FILE + ".xml");
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

    private void addTargetAppFromInput() {
        String packageId = targetAppInput.getText().toString().trim();
        if (packageId.isEmpty()) {
            return;
        }
        if (targetApps.contains(packageId)) {
            Toast.makeText(this, R.string.item_already_exists, Toast.LENGTH_SHORT).show();
            return;
        }
        targetApps.add(packageId);
        targetAppInput.setText("");
        refreshListViews();
        persistTargetLists();
    }

    private void addTargetHashFromInput() {
        String hash = normalizeHash(targetHashInput.getText().toString());
        if (hash.isEmpty()) {
            return;
        }
        if (targetHashes.contains(hash)) {
            Toast.makeText(this, R.string.item_already_exists, Toast.LENGTH_SHORT).show();
            return;
        }
        targetHashes.add(hash);
        targetHashInput.setText("");
        refreshListViews();
        persistTargetLists();
    }

    private void showRemoveDialog(List<String> items, boolean isAppList) {
        if (items.isEmpty()) {
            Toast.makeText(this, R.string.list_is_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        String title = getString(isAppList ? R.string.remove_app_title : R.string.remove_hash_title);
        CharSequence[] labels = items.toArray(new CharSequence[0]);
        boolean[] checked = new boolean[items.size()];

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_remove, (dialog, which) -> {
                    for (int i = checked.length - 1; i >= 0; i--) {
                        if (checked[i]) {
                            items.remove(i);
                        }
                    }
                    refreshListViews();
                    persistTargetLists();
                })
                .show();
    }

    private void showAppPicker() {
        List<AppItem> apps = loadLaunchableApps();
        if (apps.isEmpty()) {
            Toast.makeText(this, R.string.picker_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] labels = new CharSequence[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            AppItem item = apps.get(i);
            labels[i] = item.label + "\n" + item.packageName;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.picker_title)
                .setItems(labels, (dialog, which) -> {
                    AppItem selected = apps.get(which);
                    if (!targetApps.contains(selected.packageName)) {
                        targetApps.add(selected.packageName);
                        refreshListViews();
                        persistTargetLists();
                    } else {
                        Toast.makeText(this, R.string.item_already_exists, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private List<AppItem> loadLaunchableApps() {
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolved = pm.queryIntentActivities(launcherIntent, 0);
        List<AppItem> items = new ArrayList<>();
        for (ResolveInfo info : resolved) {
            if (info.activityInfo == null || info.activityInfo.packageName == null) {
                continue;
            }
            CharSequence labelCs = info.loadLabel(pm);
            String label = labelCs == null ? info.activityInfo.packageName : labelCs.toString();
            items.add(new AppItem(label, info.activityInfo.packageName));
        }

        Collections.sort(items, Comparator.comparing(a -> a.label.toLowerCase(Locale.ROOT)));
        return items;
    }

    private List<String> parseListPreference(String raw) {
        Set<String> unique = new LinkedHashSet<>();
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        String[] lines = raw.split("\\n");
        for (String line : lines) {
            String value = line.trim();
            if (!value.isEmpty()) {
                unique.add(value);
            }
        }
        return new ArrayList<>(unique);
    }

    private void refreshListViews() {
        targetAppsListText.setText(formatList(targetApps));
        targetHashesListText.setText(formatList(targetHashes));
    }

    private String formatList(List<String> items) {
        if (items.isEmpty()) {
            return getString(R.string.list_empty_placeholder);
        }
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            builder.append("- ").append(item).append('\n');
        }
        return builder.toString().trim();
    }

    private String normalizeHash(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^0-9a-f]", "");
    }

    private void persistTargetLists() {
        prefs.edit()
                .putString(Prefs.KEY_TARGET_APP, joinLines(targetApps))
                .putString(Prefs.KEY_TARGET_HASH, joinLines(targetHashes))
                .apply();
        makePrefsWorldReadable();
    }

    private String joinLines(List<String> values) {
        if (values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static final class AppItem {
        final String label;
        final String packageName;

        AppItem(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }
}

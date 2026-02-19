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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SettingsActivity extends Activity {

    private SharedPreferences prefs;
    private EditText targetAppInput;
    private TextView targetAppsListText;
    private final List<String> targetApps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PrefAccess.openModulePrefs(this);

        Button pickTargetApp = findViewById(R.id.button_pick_target_app);
        Button addTargetApp = findViewById(R.id.button_add_target_app);
        Button removeTargetApp = findViewById(R.id.button_remove_target_app);
        Button restartSeService = findViewById(R.id.button_restart_se_service);
        Switch verboseLog = findViewById(R.id.switch_verbose_log);

        targetAppInput = findViewById(R.id.edit_target_app);
        targetAppsListText = findViewById(R.id.text_target_apps_list);

        ensurePrefsCreated();

        targetApps.clear();
        targetApps.addAll(parseListPreference(prefs.getString(Prefs.KEY_TARGET_APP, "")));
        verboseLog.setChecked(prefs.getBoolean(Prefs.KEY_VERBOSE_LOG, false));
        refreshListViews();

        verboseLog.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(Prefs.KEY_VERBOSE_LOG, isChecked).commit();
            PrefAccess.ensureWorldReadable(this);
        });

        pickTargetApp.setOnClickListener(v -> showAppPicker());
        addTargetApp.setOnClickListener(v -> addTargetAppFromInput());
        removeTargetApp.setOnClickListener(v -> showRemoveDialog());
        restartSeService.setOnClickListener(v -> restartSeService());

        PrefAccess.ensureWorldReadable(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        persistTargetApps();
    }

    private void ensurePrefsCreated() {
        SharedPreferences.Editor editor = prefs.edit();
        if (!prefs.contains(Prefs.KEY_VERBOSE_LOG)) {
            editor.putBoolean(Prefs.KEY_VERBOSE_LOG, false);
        }
        if (!prefs.contains(Prefs.KEY_TARGET_APP)) {
            editor.putString(Prefs.KEY_TARGET_APP, "");
        }
        editor.commit();
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
        persistTargetApps();
    }

    private void showRemoveDialog() {
        if (targetApps.isEmpty()) {
            Toast.makeText(this, R.string.list_is_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] labels = targetApps.toArray(new CharSequence[0]);
        boolean[] checked = new boolean[targetApps.size()];

        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_app_title)
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_remove, (dialog, which) -> {
                    for (int i = checked.length - 1; i >= 0; i--) {
                        if (checked[i]) {
                            targetApps.remove(i);
                        }
                    }
                    refreshListViews();
                    persistTargetApps();
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
                        persistTargetApps();
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
        Set<String> unique = Utils.parseNonEmptyLines(raw);
        return new ArrayList<>(unique);
    }

    private void refreshListViews() {
        targetAppsListText.setText(formatList(targetApps));
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

    private void persistTargetApps() {
        prefs.edit().putString(Prefs.KEY_TARGET_APP, joinLines(targetApps)).commit();
        PrefAccess.ensureWorldReadable(this);
    }

    private void restartSeService() {
        new Thread(() -> {
            boolean success = false;
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "killall com.android.se"});
                int exitCode = process.waitFor();
                success = exitCode == 0;
            } catch (Throwable ignored) {
                success = false;
            }

            final boolean result = success;
            runOnUiThread(() -> Toast.makeText(
                    this,
                    result ? R.string.restart_service_done : R.string.restart_service_failed,
                    Toast.LENGTH_SHORT
            ).show());
        }).start();
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

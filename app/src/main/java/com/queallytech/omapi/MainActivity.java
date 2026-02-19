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
    private TextView recentCallsText;
    private final List<String> targetApps = new ArrayList<>();
    private final List<String> targetHashes = new ArrayList<>();
    private final List<RecentCall> recentCalls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSettingsPrefs();

        final Button pickTargetApp = findViewById(R.id.button_pick_target_app);
        final Button addTargetApp = findViewById(R.id.button_add_target_app);
        final Button removeTargetApp = findViewById(R.id.button_remove_target_app);
        final Button addTargetHash = findViewById(R.id.button_add_target_hash);
        final Button removeTargetHash = findViewById(R.id.button_remove_target_hash);
        final Button addFromRecent = findViewById(R.id.button_add_from_recent);
        final Switch verboseLog = findViewById(R.id.switch_verbose_log);
        targetAppInput = findViewById(R.id.edit_target_app);
        targetHashInput = findViewById(R.id.edit_target_hash);
        targetAppsListText = findViewById(R.id.text_target_apps_list);
        targetHashesListText = findViewById(R.id.text_target_hashes_list);
        recentCallsText = findViewById(R.id.text_recent_calls);
        targetApps.clear();
        targetApps.addAll(parseListPreference(prefs.getString(Prefs.KEY_TARGET_APP, "")));
        targetHashes.clear();
        targetHashes.addAll(parseListPreference(prefs.getString(Prefs.KEY_TARGET_HASH, "")));
        reloadRecentCalls();
        verboseLog.setChecked(prefs.getBoolean(Prefs.KEY_VERBOSE_LOG, false));
        refreshListViews();

        verboseLog.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(Prefs.KEY_VERBOSE_LOG, isChecked).apply();
            makePrefsWorldReadable();
        });

        pickTargetApp.setOnClickListener(v -> showAppPicker());
        addTargetApp.setOnClickListener(v -> addTargetAppFromInput());
        removeTargetApp.setOnClickListener(v -> showRemoveDialog(targetApps, true));
        addTargetHash.setOnClickListener(v -> addTargetHashFromInput());
        removeTargetHash.setOnClickListener(v -> showRemoveDialog(targetHashes, false));
        addFromRecent.setOnClickListener(v -> showRecentCallPicker());

        makePrefsWorldReadable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        persistTargetLists();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadRecentCalls();
        refreshListViews();
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
        recentCallsText.setText(formatRecentCalls(recentCalls));
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

    private void showRecentCallPicker() {
        if (recentCalls.isEmpty()) {
            Toast.makeText(this, R.string.list_is_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] items = new CharSequence[recentCalls.size()];
        for (int i = 0; i < recentCalls.size(); i++) {
            RecentCall call = recentCalls.get(i);
            items[i] = formatRecentCallLine(call);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.recent_picker_title)
                .setItems(items, (dialog, which) -> {
                    RecentCall call = recentCalls.get(which);
                    boolean changed = false;
                    if (!call.callerPackage.isEmpty() && !targetApps.contains(call.callerPackage)) {
                        targetApps.add(call.callerPackage);
                        changed = true;
                    }
                    if (!call.araMHash.isEmpty() && !targetHashes.contains(call.araMHash)) {
                        targetHashes.add(call.araMHash);
                        changed = true;
                    }
                    if (changed) {
                        persistTargetLists();
                        refreshListViews();
                        Toast.makeText(this, R.string.added_from_recent, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.item_already_exists, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void reloadRecentCalls() {
        recentCalls.clear();
        String raw = prefs.getString(Prefs.KEY_RECENT_CALLS, "");
        if (raw == null || raw.isEmpty()) {
            return;
        }
        String[] lines = raw.split("\\n");
        for (String line : lines) {
            String value = line.trim();
            if (value.isEmpty()) {
                continue;
            }
            String[] parts = value.split("\\|", 3);
            if (parts.length < 2) {
                continue;
            }
            String caller = parts[0].trim();
            String hash = parts[1].trim();
            if (caller.isEmpty() && hash.isEmpty()) {
                continue;
            }
            recentCalls.add(new RecentCall(caller, hash));
            if (recentCalls.size() >= Prefs.MAX_RECENT_CALLS) {
                break;
            }
        }
    }

    private String formatRecentCalls(List<RecentCall> calls) {
        if (calls.isEmpty()) {
            return getString(R.string.list_empty_placeholder);
        }
        StringBuilder builder = new StringBuilder();
        for (RecentCall call : calls) {
            builder.append("- ").append(formatRecentCallLine(call)).append('\n');
        }
        return builder.toString().trim();
    }

    private String formatRecentCallLine(RecentCall call) {
        String caller = call.callerPackage.isEmpty() ? "?" : call.callerPackage;
        String hash = call.araMHash.isEmpty() ? "-" : call.araMHash;
        return caller + " | " + hash;
    }

    private static final class AppItem {
        final String label;
        final String packageName;

        AppItem(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }

    private static final class RecentCall {
        final String callerPackage;
        final String araMHash;

        RecentCall(String callerPackage, String araMHash) {
            this.callerPackage = callerPackage;
            this.araMHash = araMHash;
        }
    }
}

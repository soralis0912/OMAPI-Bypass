package com.queallytech.omapi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RecentCallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Prefs.ACTION_RECORD_RECENT_CALL.equals(intent.getAction())) {
            return;
        }

        String caller = trim(intent.getStringExtra(Prefs.EXTRA_CALLER_PACKAGE));
        String hash = trim(intent.getStringExtra(Prefs.EXTRA_ARAM_HASH));
        if (caller.isEmpty() && hash.isEmpty()) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(Prefs.PREF_FILE, Context.MODE_PRIVATE);
        List<String> existing = parseLines(prefs.getString(Prefs.KEY_RECENT_CALLS, ""));
        String newEntry = caller + "|" + hash + "|" + System.currentTimeMillis();

        Set<String> dedup = new LinkedHashSet<>();
        dedup.add(caller + "|" + hash);

        List<String> result = new ArrayList<>();
        result.add(newEntry);

        for (String line : existing) {
            String[] parts = line.split("\\|", 3);
            if (parts.length < 2) {
                continue;
            }
            String key = trim(parts[0]) + "|" + trim(parts[1]);
            if (dedup.contains(key)) {
                continue;
            }
            dedup.add(key);
            result.add(line);
            if (result.size() >= Prefs.MAX_RECENT_CALLS) {
                break;
            }
        }

        prefs.edit().putString(Prefs.KEY_RECENT_CALLS, joinLines(result)).apply();
    }

    private static List<String> parseLines(String raw) {
        List<String> lines = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return lines;
        }
        String[] split = raw.split("\\n");
        for (String line : split) {
            String value = trim(line);
            if (!value.isEmpty()) {
                lines.add(value);
            }
        }
        return lines;
    }

    private static String joinLines(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}

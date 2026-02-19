package org.soralis.omapi.policyhook;

import java.util.LinkedHashSet;
import java.util.Set;

public final class Utils {

    private Utils() {
    }

    public static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static Set<String> parseNonEmptyLines(String raw) {
        Set<String> items = new LinkedHashSet<>();
        if (isEmpty(raw)) {
            return items;
        }

        String[] lines = raw.split("\\n");
        for (String line : lines) {
            String value = safeTrim(line);
            if (!isEmpty(value)) {
                items.add(value);
            }
        }
        return items;
    }
}

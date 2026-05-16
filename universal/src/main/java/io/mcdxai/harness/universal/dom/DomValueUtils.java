package io.mcdxai.harness.universal.dom;

import java.util.ArrayList;
import java.util.List;

public final class DomValueUtils {
    private DomValueUtils() {
    }

    public static boolean asBoolean(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean booleanValue) return booleanValue;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public static String asString(Object value, String fallback) {
        if (value == null) return fallback;
        String str = String.valueOf(value);
        return str == null ? fallback : str;
    }

    public static List<String> asStringList(Object value) {
        if (value == null) return List.of();
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object item : list) {
                if (item == null) continue;
                String str = String.valueOf(item).trim();
                if (!str.isEmpty()) out.add(str);
            }
            return out;
        }
        String single = String.valueOf(value).trim();
        if (single.isEmpty()) return List.of();
        return List.of(single);
    }

    public static boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) return booleanValue;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public static double toDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0D;
        }
    }
}

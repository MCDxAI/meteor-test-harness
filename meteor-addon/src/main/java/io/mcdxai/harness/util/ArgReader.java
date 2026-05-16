package io.mcdxai.harness.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ArgReader {
    private final Map<String, Object> args;

    public ArgReader(Map<String, Object> args) {
        this.args = args == null ? Collections.emptyMap() : args;
    }

    public String string(String key) {
        Object value = args.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public String string(String key, String fallback) {
        String value = string(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public boolean bool(String key, boolean fallback) {
        Object value = args.get(key);
        if (value == null) return fallback;
        if (value instanceof Boolean booleanValue) return booleanValue;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public int intValue(String key, int fallback) {
        Object value = args.get(key);
        if (value == null) return fallback;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public double doubleValue(String key, double fallback) {
        Object value = args.get(key);
        if (value == null) return fallback;
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> object(String key) {
        Object value = args.get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public List<Object> list(String key) {
        Object value = args.get(key);
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        return Collections.emptyList();
    }

    public Object raw(String key) {
        return args.get(key);
    }
}
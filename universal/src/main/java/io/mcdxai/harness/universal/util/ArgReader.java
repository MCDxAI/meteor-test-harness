package io.mcdxai.harness.universal.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ArgReader {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Object>> LIST_TYPE = new TypeReference<>() {};

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
        // Some MCP clients (notably code-mode wrappers) serialize nested object args
        // as a JSON string instead of a JSON object. Falling back silently to an empty
        // map caused filter inputs to be ignored, returning unfiltered results.
        if (value instanceof String str && !str.isBlank()) {
            try {
                return JSON.readValue(str, MAP_TYPE);
            } catch (Exception ignored) {
            }
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public List<Object> list(String key) {
        Object value = args.get(key);
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return JSON.readValue(str, LIST_TYPE);
            } catch (Exception ignored) {
            }
        }
        return Collections.emptyList();
    }

    public Object raw(String key) {
        return args.get(key);
    }
}

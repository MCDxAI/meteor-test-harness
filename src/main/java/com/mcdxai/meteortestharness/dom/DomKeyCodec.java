package com.mcdxai.meteortestharness.dom;

import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DomKeyCodec {
    public static final int VERSION = 3;

    private static final Map<String, String> KEY_ALIASES = Map.ofEntries(
        Map.entry("ESC", "ESCAPE"),
        Map.entry("RETURN", "ENTER"),
        Map.entry("SPACEBAR", "SPACE"),
        Map.entry("DEL", "DELETE"),
        Map.entry("INS", "INSERT"),
        Map.entry("PGUP", "PAGE_UP"),
        Map.entry("PGDN", "PAGE_DOWN"),
        Map.entry("ARROW_UP", "UP"),
        Map.entry("ARROW_DOWN", "DOWN"),
        Map.entry("ARROW_LEFT", "LEFT"),
        Map.entry("ARROW_RIGHT", "RIGHT"),
        Map.entry("SHIFT", "LEFT_SHIFT"),
        Map.entry("LSHIFT", "LEFT_SHIFT"),
        Map.entry("RSHIFT", "RIGHT_SHIFT"),
        Map.entry("CTRL", "LEFT_CONTROL"),
        Map.entry("CONTROL", "LEFT_CONTROL"),
        Map.entry("LCTRL", "LEFT_CONTROL"),
        Map.entry("RCTRL", "RIGHT_CONTROL"),
        Map.entry("ALT", "LEFT_ALT"),
        Map.entry("LALT", "LEFT_ALT"),
        Map.entry("RALT", "RIGHT_ALT"),
        Map.entry("OPTION", "LEFT_ALT"),
        Map.entry("WIN", "LEFT_SUPER"),
        Map.entry("WINDOWS", "LEFT_SUPER"),
        Map.entry("CMD", "LEFT_SUPER"),
        Map.entry("COMMAND", "LEFT_SUPER")
    );

    private DomKeyCodec() {
    }

    public static Integer parseKeyCode(String keyName) {
        if (keyName == null || keyName.isBlank()) {
            return null;
        }

        String normalized = keyName.trim()
            .toUpperCase(Locale.ROOT)
            .replace(' ', '_')
            .replace('-', '_');

        normalized = KEY_ALIASES.getOrDefault(normalized, normalized);
        if (normalized.length() == 1) {
            char c = normalized.charAt(0);
            if (c >= 'A' && c <= 'Z') return GLFW.GLFW_KEY_A + (c - 'A');
            if (c >= '0' && c <= '9') return GLFW.GLFW_KEY_0 + (c - '0');
        }

        if (normalized.startsWith("F")) {
            try {
                int fn = Integer.parseInt(normalized.substring(1));
                if (fn >= 1 && fn <= 25) {
                    return GLFW.GLFW_KEY_F1 + (fn - 1);
                }
            } catch (NumberFormatException ignored) {
                // Fall through.
            }
        }

        Integer named = parseNamedKey(normalized);
        if (named != null) return named;

        Integer glfwKey = parseGlfwKey(normalized);
        if (glfwKey != null) return glfwKey;

        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer parseNamedKey(String normalized) {
        return switch (normalized) {
            case "ENTER" -> GLFW.GLFW_KEY_ENTER;
            case "ESCAPE" -> GLFW.GLFW_KEY_ESCAPE;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "SPACE" -> GLFW.GLFW_KEY_SPACE;
            case "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE;
            case "DELETE" -> GLFW.GLFW_KEY_DELETE;
            case "UP" -> GLFW.GLFW_KEY_UP;
            case "DOWN" -> GLFW.GLFW_KEY_DOWN;
            case "LEFT" -> GLFW.GLFW_KEY_LEFT;
            case "RIGHT" -> GLFW.GLFW_KEY_RIGHT;
            case "HOME" -> GLFW.GLFW_KEY_HOME;
            case "END" -> GLFW.GLFW_KEY_END;
            case "PAGE_UP" -> GLFW.GLFW_KEY_PAGE_UP;
            case "PAGE_DOWN" -> GLFW.GLFW_KEY_PAGE_DOWN;
            case "INSERT" -> GLFW.GLFW_KEY_INSERT;
            case "LEFT_SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RIGHT_SHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LEFT_CONTROL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RIGHT_CONTROL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LEFT_ALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "RIGHT_ALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
            case "LEFT_SUPER" -> GLFW.GLFW_KEY_LEFT_SUPER;
            case "RIGHT_SUPER" -> GLFW.GLFW_KEY_RIGHT_SUPER;
            case "MENU" -> GLFW.GLFW_KEY_MENU;
            default -> null;
        };
    }

    private static Integer parseGlfwKey(String normalized) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(normalized);

        if (normalized.startsWith("GLFW_KEY_")) {
            candidates.add(normalized.substring("GLFW_KEY_".length()));
        }
        if (normalized.startsWith("KEY_")) {
            candidates.add(normalized.substring("KEY_".length()));
        }
        if (normalized.startsWith("NUMPAD_")) {
            candidates.add("KP_" + normalized.substring("NUMPAD_".length()));
        }
        if (normalized.startsWith("NUMPAD") && normalized.length() > "NUMPAD".length()) {
            candidates.add("KP_" + normalized.substring("NUMPAD".length()));
        }
        if (normalized.startsWith("KP") && normalized.length() > 2 && normalized.charAt(2) != '_') {
            candidates.add("KP_" + normalized.substring(2));
        }

        for (String candidate : candidates) {
            Integer key = readGlfwKeyConstant(candidate);
            if (key != null) return key;
        }
        return null;
    }

    private static Integer readGlfwKeyConstant(String token) {
        String constantName = token.startsWith("GLFW_KEY_") ? token : "GLFW_KEY_" + token;
        try {
            Field field = GLFW.class.getField(constantName);
            if (field.getType() != int.class) return null;
            return field.getInt(null);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return null;
        }
    }
}

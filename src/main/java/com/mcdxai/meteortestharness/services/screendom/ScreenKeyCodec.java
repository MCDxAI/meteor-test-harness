package com.mcdxai.meteortestharness.services.screendom;

import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class ScreenKeyCodec {
    private ScreenKeyCodec() {
    }

    public static Integer parseKeyCode(String keyName) {
        if (keyName == null || keyName.isBlank()) {
            return null;
        }

        String normalized = keyName.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
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

        return switch (normalized) {
            case "ENTER", "RETURN" -> GLFW.GLFW_KEY_ENTER;
            case "ESC", "ESCAPE" -> GLFW.GLFW_KEY_ESCAPE;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "SPACE", "SPACEBAR" -> GLFW.GLFW_KEY_SPACE;
            case "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE;
            case "DELETE", "DEL" -> GLFW.GLFW_KEY_DELETE;
            case "UP", "ARROW_UP" -> GLFW.GLFW_KEY_UP;
            case "DOWN", "ARROW_DOWN" -> GLFW.GLFW_KEY_DOWN;
            case "LEFT", "ARROW_LEFT" -> GLFW.GLFW_KEY_LEFT;
            case "RIGHT", "ARROW_RIGHT" -> GLFW.GLFW_KEY_RIGHT;
            case "HOME" -> GLFW.GLFW_KEY_HOME;
            case "END" -> GLFW.GLFW_KEY_END;
            case "PAGE_UP", "PGUP" -> GLFW.GLFW_KEY_PAGE_UP;
            case "PAGE_DOWN", "PGDN" -> GLFW.GLFW_KEY_PAGE_DOWN;
            case "INSERT", "INS" -> GLFW.GLFW_KEY_INSERT;
            default -> {
                try {
                    yield Integer.parseInt(normalized);
                } catch (NumberFormatException ignored) {
                    yield null;
                }
            }
        };
    }
}

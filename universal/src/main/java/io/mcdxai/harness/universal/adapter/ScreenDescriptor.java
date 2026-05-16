package io.mcdxai.harness.universal.adapter;

import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.Map;

/** Per-screen LLM-facing hints (keyboard shortcuts, navigation tips, role labels). */
public interface ScreenDescriptor {
    boolean matches(Screen screen);

    List<String> hints();

    default Map<String, String> keyboardShortcuts() {
        return Map.of();
    }

    /** Optional alias for the screen role (e.g. "item_editor"). */
    default String screenRole() {
        return null;
    }
}

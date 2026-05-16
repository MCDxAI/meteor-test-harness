package io.mcdxai.harness.universal.adapter;

import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.Map;

/** A pluggable engine that knows how to introspect a particular family of screens. */
public interface ScreenEngine {
    /** Engine name used in element prefixes and DOM payload (e.g. "vanilla", "owo", "hybrid"). */
    String engineName();

    /** Higher priority wins when multiple engines could handle a screen. */
    int priority();

    /** Whether this engine claims the given screen. */
    boolean canHandle(Screen screen);

    /** Build the DOM element tree (root list) for this screen. */
    List<Map<String, Object>> buildDom(Screen screen, DomBuildContext ctx);

    /** Default screen title extraction. */
    default String getScreenTitle(Screen screen) {
        try {
            return screen.getTitle() == null ? "" : screen.getTitle().getString();
        } catch (Exception e) {
            return "";
        }
    }
}

package io.mcdxai.harness.universal.adapter;

import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.Map;

/**
 * Metadata + interaction adapter for a specific widget/component class.
 * Implementations override only what they need; defaults are no-ops.
 *
 * Higher specificity wins: an adapter declared for a concrete subclass takes
 * precedence over one declared for an abstract base. The registry checks
 * adapters in registration order, walking up the class hierarchy if needed.
 */
public interface WidgetAdapter<W> {
    /** The widget/component class this adapter handles (most specific match wins). */
    Class<W> widgetType();

    /** Extract metadata into the target map (label, text, value, role, etc.). */
    default void extractMetadata(W widget, Map<String, Object> target) {
    }

    /** What actions can this widget currently perform? Used for the "actions" array. */
    default List<String> supportedActions(W widget) {
        return List.of();
    }

    /** Optional: handle a click directly. Return true if handled (overrides default screen-coord click). */
    default boolean handleClick(Screen screen, W widget, double x, double y, int button, boolean doubled) {
        return false;
    }

    /** Optional: set text on this widget. Return true if handled. */
    default boolean handleSetText(Screen screen, W widget, String text) {
        return false;
    }

    /** Optional: set a typed value on this widget (boolean/number/etc.). Return true if handled. */
    default boolean handleSetValue(Screen screen, W widget, Object value) {
        return false;
    }
}

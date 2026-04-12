package com.mcdxai.meteortestharness.services.screendom;

import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ScreenDomActionHints {
    private ScreenDomActionHints() {
    }

    public static void addVanillaHints(Map<String, Object> mapped, Element element, EntryListWidget<?> owningList, double x, double y, double w, double h) {
        List<String> actions = new ArrayList<>();
        boolean clickable = canClickVanillaElement(element, owningList, w, h);

        if (clickable) {
            actions.add("click");
        }
        if (element instanceof TextFieldWidget) {
            actions.add("set_text");
            actions.add("type_text");
        }
        if (element instanceof ScrollableWidget || element instanceof EntryListWidget<?>) {
            actions.add("scroll");
        }
        if (element instanceof SliderWidget) {
            actions.add("drag");
        }

        mapped.put("clickable", clickable);
        mapped.put("actionable", !actions.isEmpty());
        mapped.put("actions", actions);
    }

    private static boolean canClickVanillaElement(Element element, EntryListWidget<?> owningList, double w, double h) {
        boolean hasCoordinates = !Double.isNaN(w) && !Double.isNaN(h) && w > 0 && h > 0;
        if (!hasCoordinates) {
            return false;
        }

        if (element instanceof ClickableWidget widget) {
            return widget.visible && widget.active;
        }

        return element instanceof AlwaysSelectedEntryListWidget.Entry<?> && owningList != null;
    }

    public static void addMeteorHints(Map<String, Object> mapped, WWidget widget, double w, double h, boolean moduleWidget) {
        List<String> actions = new ArrayList<>();
        boolean hasCoordinates = !Double.isNaN(w) && !Double.isNaN(h) && w > 0 && h > 0;

        boolean clickable = widget.visible
            && hasCoordinates
            && (widget instanceof WPressable || widget instanceof WTextBox || widget instanceof WCheckbox || widget instanceof WSlider);

        if (clickable) {
            actions.add("click");
        }
        if (widget instanceof WTextBox) {
            actions.add("set_text");
            actions.add("type_text");
        }
        if (widget instanceof WCheckbox || widget instanceof WSlider) {
            actions.add("set_value");
        }
        if (widget instanceof WSlider) {
            actions.add("drag");
        }
        if (moduleWidget && clickable) {
            actions.add("click_secondary");
            actions.add("open_module_settings");
        }

        mapped.put("clickable", clickable);
        mapped.put("actionable", !actions.isEmpty());
        mapped.put("actions", actions);
    }
}

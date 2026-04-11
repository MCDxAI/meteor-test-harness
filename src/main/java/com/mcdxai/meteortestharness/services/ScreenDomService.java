package com.mcdxai.meteortestharness.services;

import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.input.MouseInput;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class ScreenDomService {
    private static final Field WIDGET_SCREEN_ROOT_FIELD = resolveWidgetScreenRootField();

    private final Map<String, ElementRef> refs = new HashMap<>();
    private int idCounter = 0;

    public synchronized Map<String, Object> snapshot() {
        refs.clear();
        idCounter = 0;

        Screen screen = mc.currentScreen;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hasScreen", screen != null);

        if (screen == null) {
            payload.put("screen", null);
            payload.put("elements", List.of());
            return payload;
        }

        payload.put("screen", Map.of(
            "class", screen.getClass().getName(),
            "title", screen.getTitle() == null ? "" : screen.getTitle().getString(),
            "meteorWidgetScreen", screen instanceof WidgetScreen
        ));

        List<Map<String, Object>> elements;
        if (screen instanceof WidgetScreen widgetScreen) {
            elements = buildMeteorDom(widgetScreen);
            payload.put("engine", "meteor");
        } else {
            elements = buildVanillaDom(screen);
            payload.put("engine", "vanilla");
        }

        payload.put("elements", elements);
        return payload;
    }

    public synchronized boolean click(String id) {
        ElementRef ref = resolveRef(id);
        if (ref == null || mc.currentScreen == null) return false;

        if (ref.backing instanceof Element element && ref.clickableCoordinates()) {
            try {
                Click click = new Click(ref.centerX(), ref.centerY(), new MouseInput(0, 0));
                element.mouseClicked(click, false);
                return true;
            } catch (Exception ignored) {}
        }

        if (ref.clickableCoordinates()) {
            double clickX = ref.centerX();
            double clickY = ref.centerY();
            return invokeScreenClick(mc.currentScreen, clickX, clickY, 0);
        }

        if (ref.backing instanceof WPressable pressable && pressable.action != null) {
            pressable.action.run();
            return true;
        }

        return false;
    }

    public synchronized boolean setText(String id, String text) {
        ElementRef ref = resolveRef(id);
        if (ref == null) return false;

        if (ref.backing instanceof TextFieldWidget textFieldWidget) {
            textFieldWidget.setText(text);
            return true;
        }

        if (ref.backing instanceof WTextBox textBox) {
            textBox.set(text);
            if (textBox.action != null) textBox.action.run();
            return true;
        }

        return false;
    }

    public synchronized boolean setValue(String id, Object value) {
        ElementRef ref = resolveRef(id);
        if (ref == null) return false;

        if (ref.backing instanceof WCheckbox checkBox) {
            checkBox.checked = toBoolean(value);
            if (checkBox.action != null) checkBox.action.run();
            return true;
        }

        if (ref.backing instanceof WSlider slider) {
            slider.set(toDouble(value));
            if (slider.action != null) slider.action.run();
            return true;
        }

        return false;
    }

    public boolean navigateBack() {
        if (mc.currentScreen == null) return false;
        mc.currentScreen.close();
        return true;
    }

    private List<Map<String, Object>> buildVanillaDom(Screen screen) {
        List<Map<String, Object>> elements = new ArrayList<>();

        List<? extends Element> children = screen.children();
        for (Element child : children) {
            elements.add(mapVanillaElement(child));
        }

        return elements;
    }

    private Map<String, Object> mapVanillaElement(Element element) {
        String id = nextId("v");

        ElementRef ref = new ElementRef(id, element);
        refs.put(id, ref);

        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("id", id);
        mapped.put("engine", "vanilla");
        mapped.put("type", element.getClass().getSimpleName());

        if (element instanceof ClickableWidget widget) {
            mapped.put("label", widget.getMessage() == null ? "" : widget.getMessage().getString());
            mapped.put("visible", widget.visible);
            mapped.put("active", widget.active);
            mapped.put("x", widget.getX());
            mapped.put("y", widget.getY());
            mapped.put("width", widget.getWidth());
            mapped.put("height", widget.getHeight());

            ref.x = widget.getX();
            ref.y = widget.getY();
            ref.width = widget.getWidth();
            ref.height = widget.getHeight();
        }

        if (element instanceof Widget widget) {
            mapped.putIfAbsent("x", widget.getX());
            mapped.putIfAbsent("y", widget.getY());
            mapped.putIfAbsent("width", widget.getWidth());
            mapped.putIfAbsent("height", widget.getHeight());

            if (Double.isNaN(ref.x)) {
                ref.x = widget.getX();
                ref.y = widget.getY();
                ref.width = widget.getWidth();
                ref.height = widget.getHeight();
            }
        }

        if (element instanceof TextFieldWidget textFieldWidget) {
            mapped.put("text", textFieldWidget.getText());
            mapped.put("editable", textFieldWidget.isActive());
        }

        if (element instanceof ParentElement parentElement) {
            List<Map<String, Object>> children = new ArrayList<>();
            for (Element child : parentElement.children()) {
                if (child == element) continue;
                children.add(mapVanillaElement(child));
            }
            mapped.put("children", children);
        }

        return mapped;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildMeteorDom(WidgetScreen screen) {
        List<Map<String, Object>> elements = new ArrayList<>();
        if (WIDGET_SCREEN_ROOT_FIELD == null) {
            return elements;
        }

        try {
            WContainer root = (WContainer) WIDGET_SCREEN_ROOT_FIELD.get(screen);
            if (root == null) return elements;

            for (Cell<?> cell : root.cells) {
                elements.add(mapMeteorWidget(cell.widget()));
            }
        } catch (Exception ignored) {
            // Return whatever we have if reflection fails.
        }

        return elements;
    }

    private static Field resolveWidgetScreenRootField() {
        try {
            Field field = WidgetScreen.class.getDeclaredField("root");
            field.setAccessible(true);
            return field;
        } catch (Exception ignored) {
            return null;
        }
    }

    private ElementRef resolveRef(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        ElementRef ref = refs.get(id);
        if (ref != null) {
            return ref;
        }

        snapshot();
        return refs.get(id);
    }

    private Map<String, Object> mapMeteorWidget(WWidget widget) {
        String id = nextId("m");

        ElementRef ref = new ElementRef(id, widget);
        refs.put(id, ref);

        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("id", id);
        mapped.put("engine", "meteor");
        mapped.put("type", widget.getClass().getSimpleName());
        mapped.put("visible", widget.visible);
        mapped.put("focused", widget.isFocused());
        mapped.put("x", widget.x);
        mapped.put("y", widget.y);
        mapped.put("width", widget.width);
        mapped.put("height", widget.height);

        ref.x = widget.x;
        ref.y = widget.y;
        ref.width = widget.width;
        ref.height = widget.height;

        if (widget instanceof WButton button) {
            mapped.put("label", button.getText());
        }
        if (widget instanceof WLabel label) {
            mapped.put("label", label.get());
        }
        if (widget instanceof WTextBox textBox) {
            mapped.put("text", textBox.get());
        }
        if (widget instanceof WCheckbox checkBox) {
            mapped.put("checked", checkBox.checked);
        }
        if (widget instanceof WSlider slider) {
            mapped.put("value", slider.get());
        }

        if (widget instanceof WContainer container) {
            List<Map<String, Object>> children = new ArrayList<>();
            for (Cell<?> cell : container.cells) {
                children.add(mapMeteorWidget(cell.widget()));
            }
            mapped.put("children", children);
        }

        return mapped;
    }

    private String nextId(String prefix) {
        idCounter++;
        return prefix + "-" + idCounter;
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) return booleanValue;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0D;
        }
    }

    private boolean invokeScreenClick(Screen screen, double x, double y, int button) {
        try {
            Click click = new Click(x, y, new MouseInput(button, 0));
            screen.mouseClicked(click, false);
            screen.mouseReleased(click);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static final class ElementRef {
        private final String id;
        private final Object backing;

        private double x = Double.NaN;
        private double y = Double.NaN;
        private double width = Double.NaN;
        private double height = Double.NaN;

        private ElementRef(String id, Object backing) {
            this.id = id;
            this.backing = backing;
        }

        private boolean clickableCoordinates() {
            return !Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(width) && !Double.isNaN(height) && width > 0 && height > 0;
        }

        private double centerX() {
            return x + width / 2D;
        }

        private double centerY() {
            return y + height / 2D;
        }
    }
}

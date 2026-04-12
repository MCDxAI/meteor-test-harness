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
import com.mcdxai.meteortestharness.services.screendom.ScreenDomActionHints;
import com.mcdxai.meteortestharness.services.screendom.ScreenKeyCodec;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

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
    private final NameMappingService nameMappingService;
    private int idCounter = 0;

    public ScreenDomService(NameMappingService nameMappingService) {
        this.nameMappingService = nameMappingService;
    }

    public synchronized Map<String, Object> snapshot() {
        refs.clear();
        idCounter = 0;

        Screen screen = mc.currentScreen;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hasScreen", screen != null);
        payload.put("mappingRuntimeNamespace", nameMappingService.getRuntimeNamespace());
        payload.put("mappingPreferredNamespace", nameMappingService.getPreferredNamespace());
        payload.put("mappingMode", nameMappingService.getMappingMode());
        payload.put("mappingNamespaces", nameMappingService.getNamespaces());
        payload.put("mappingRuntimeNamedAvailable", nameMappingService.hasRuntimeNamedMappings());
        payload.put("mappingBundledNamedAvailable", nameMappingService.hasBundledNamedMappings());
        payload.put("mappingBundledNamedClassCount", nameMappingService.getBundledNamedClassCount());

        if (screen == null) {
            payload.put("screen", null);
            payload.put("elements", List.of());
            return payload;
        }

        String screenClass = screen.getClass().getName();
        String mappedScreenClass = nameMappingService.mapClassName(screenClass);
        Map<String, Object> screenInfo = new LinkedHashMap<>();
        screenInfo.put("class", screenClass);
        screenInfo.put("classMapped", mappedScreenClass);
        screenInfo.put("type", nameMappingService.simpleName(screenClass));
        screenInfo.put("typeMapped", nameMappingService.simpleName(mappedScreenClass));
        screenInfo.put("title", screen.getTitle() == null ? "" : screen.getTitle().getString());
        screenInfo.put("meteorWidgetScreen", screen instanceof WidgetScreen);
        payload.put("screen", screenInfo);

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

    public synchronized Map<String, Object> clickDetailed(String id, int button, boolean doubled) {
        Map<String, Object> result = interaction("click", id);
        result.put("button", button);
        result.put("doubleClick", doubled);

        ElementRef ref = resolveRef(id);
        Screen screen = mc.currentScreen;
        if (ref == null || screen == null) {
            result.put("reason", ref == null ? "element_not_found" : "no_active_screen");
            return finishInteraction(result, false, "none", screen);
        }

        result.put("screenBefore", classMetadata(screen.getClass()));

        // Refresh coordinates for Widget elements (may have moved due to scrolling).
        refreshCoordinates(ref);

        Click click = null;
        if (ref.clickableCoordinates()) {
            click = new Click(ref.centerX(), ref.centerY(), new MouseInput(button, 0));
            result.put("x", click.x());
            result.put("y", click.y());
        }
        result.put("coordinatesAvailable", click != null);

        boolean handled = false;

        // Path 1: Screen-level click through vanilla dispatch chain.
        if (click != null) {
            handled = invokeScreenClick(screen, click, doubled);
            if (mc.currentScreen != screen) {
                result.put("reason", "screen_changed");
                return finishInteraction(result, true, "screen", screen);
            }
        }
        result.put("screenHandled", handled);

        // For list entries, require an actual selection change.
        if (ref.backing instanceof Element element && button == 0 && isEntryElement(screen, element)) {
            result.put("entryTarget", true);
            result.put("selectedBefore", isEntrySelected(screen, element));

            if (isEntrySelected(screen, element)) {
                result.put("selectedAfter", true);
                result.put("reason", "entry_selected_via_screen_dispatch");
                return finishInteraction(result, true, "screen", screen);
            }

            if (click != null && trySelectEntryAtCoordinates(screen, click, doubled)) {
                result.put("selectedAfter", true);
                result.put("reason", "entry_selected_via_hover_fallback");
                return finishInteraction(result, true, "entry_hover_fallback", screen);
            }

            if (trySelectEntryFallback(screen, element, click, doubled)) {
                result.put("selectedAfter", true);
                result.put("reason", "entry_selected_via_owner_fallback");
                return finishInteraction(result, true, "entry_owner_fallback", screen);
            }

            result.put("selectedAfter", false);
            handled = false;
        }

        // Path 2: Direct element click.
        if (!handled && ref.backing instanceof Element element && click != null) {
            handled = invokeElementClick(element, click, doubled);
            if (mc.currentScreen != screen) {
                result.put("reason", "screen_changed");
                return finishInteraction(result, true, "element", screen);
            }
        }
        result.put("elementHandled", handled);

        // Path 3: Meteor WPressable fallback.
        if (!handled && ref.backing instanceof WPressable pressable && pressable.action != null) {
            pressable.action.run();
            handled = true;
            result.put("reason", "meteor_pressable_action");
            return finishInteraction(result, true, "meteor_action", screen);
        }

        result.put("reason", handled ? "handled" : "not_handled");
        return finishInteraction(result, handled, handled ? "element" : "none", screen);
    }

    public synchronized Map<String, Object> setTextDetailed(String id, String text, boolean submit, boolean typeCharacters, boolean clearFirst) {
        Map<String, Object> result = interaction("set_text", id);
        result.put("submit", submit);
        result.put("typeCharacters", typeCharacters);
        result.put("clearFirst", clearFirst);

        ElementRef ref = resolveRef(id);
        Screen screen = mc.currentScreen;
        if (ref == null) {
            result.put("reason", "element_not_found");
            return finishInteraction(result, false, "none", screen);
        }

        if (ref.backing instanceof TextFieldWidget textFieldWidget) {
            if (typeCharacters && screen != null) {
                refreshCoordinates(ref);
                if (ref.clickableCoordinates()) {
                    invokeScreenClick(screen, new Click(ref.centerX(), ref.centerY(), new MouseInput(0, 0)), false);
                } else {
                    screen.setFocused(textFieldWidget);
                    textFieldWidget.setFocused(true);
                }

                if (clearFirst) {
                    textFieldWidget.setText("");
                }

                boolean typed = typeChars(screen, text);
                if (!typed) {
                    String current = clearFirst ? "" : textFieldWidget.getText();
                    textFieldWidget.setText(current + text);
                }

                if (submit) {
                    pressKey(screen, GLFW.GLFW_KEY_ENTER, 0, 1, true);
                }

                result.put("reason", typed ? "typed_chars" : "typed_fallback_set_text");
                return finishInteraction(result, true, "typed", screen);
            }

            textFieldWidget.setText(text);
            if (submit && screen != null) {
                pressKey(screen, GLFW.GLFW_KEY_ENTER, 0, 1, true);
            }
            result.put("reason", "set_text_direct");
            return finishInteraction(result, true, "direct", screen);
        }

        if (ref.backing instanceof WTextBox textBox) {
            textBox.set(text);
            if (submit && textBox.action != null) {
                textBox.action.run();
            }
            result.put("reason", "set_text_direct");
            return finishInteraction(result, true, "direct", screen);
        }

        if (typeCharacters && screen != null) {
            refreshCoordinates(ref);
            if (ref.clickableCoordinates()) {
                invokeScreenClick(screen, new Click(ref.centerX(), ref.centerY(), new MouseInput(0, 0)), false);
            }

            boolean typed = typeChars(screen, text);
            if (submit) {
                pressKey(screen, GLFW.GLFW_KEY_ENTER, 0, 1, true);
            }

            result.put("reason", typed ? "typed_chars" : "char_typed_not_handled");
            return finishInteraction(result, typed, "typed", screen);
        }

        result.put("reason", "element_does_not_accept_text");
        return finishInteraction(result, false, "none", screen);
    }

    public synchronized Map<String, Object> typeTextDetailed(String id, String text, boolean clearFirst, boolean submit) {
        return setTextDetailed(id, text, submit, true, clearFirst);
    }

    public synchronized Map<String, Object> setValueDetailed(String id, Object value) {
        Map<String, Object> result = interaction("set_value", id);
        Screen screen = mc.currentScreen;

        ElementRef ref = resolveRef(id);
        if (ref == null) {
            result.put("reason", "element_not_found");
            return finishInteraction(result, false, "none", screen);
        }

        if (ref.backing instanceof WCheckbox checkBox) {
            boolean parsed = toBoolean(value);
            checkBox.checked = parsed;
            if (checkBox.action != null) {
                checkBox.action.run();
            }
            result.put("appliedValue", parsed);
            result.put("reason", "set_checkbox");
            return finishInteraction(result, true, "direct", screen);
        }

        if (ref.backing instanceof WSlider slider) {
            double parsed = toDouble(value);
            slider.set(parsed);
            if (slider.action != null) {
                slider.action.run();
            }
            result.put("appliedValue", parsed);
            result.put("reason", "set_slider");
            return finishInteraction(result, true, "direct", screen);
        }

        result.put("reason", "element_does_not_support_set_value");
        return finishInteraction(result, false, "none", screen);
    }

    public synchronized Map<String, Object> pressKeyDetailed(String keyName, int modifiers, int repeat, boolean release) {
        Map<String, Object> result = interaction("press_key", null);
        result.put("key", keyName);
        result.put("modifiers", modifiers);
        result.put("repeat", repeat);
        result.put("release", release);

        Screen screen = mc.currentScreen;
        if (screen == null) {
            result.put("reason", "no_active_screen");
            return finishInteraction(result, false, "none", null);
        }

        Integer keyCode = ScreenKeyCodec.parseKeyCode(keyName);
        if (keyCode == null) {
            result.put("reason", "unknown_key");
            return finishInteraction(result, false, "none", screen);
        }

        boolean handled = pressKey(screen, keyCode, modifiers, Math.max(1, Math.min(32, repeat)), release);
        result.put("keyCode", keyCode);
        result.put("reason", handled ? "key_handled" : "key_not_handled");
        return finishInteraction(result, handled, "screen", screen);
    }

    public synchronized Map<String, Object> scrollDetailed(String id, double verticalAmount, double horizontalAmount) {
        Map<String, Object> result = interaction("scroll", id);
        result.put("verticalAmount", verticalAmount);
        result.put("horizontalAmount", horizontalAmount);

        Screen screen = mc.currentScreen;
        if (screen == null) {
            result.put("reason", "no_active_screen");
            return finishInteraction(result, false, "none", null);
        }

        ElementRef ref = id == null || id.isBlank() ? null : resolveRef(id);
        if (id != null && !id.isBlank() && ref == null) {
            result.put("reason", "element_not_found");
            return finishInteraction(result, false, "none", screen);
        }

        double x;
        double y;
        if (ref != null) {
            refreshCoordinates(ref);
            if (ref.clickableCoordinates()) {
                x = ref.centerX();
                y = ref.centerY();
            } else {
                x = screen.width / 2D;
                y = screen.height / 2D;
            }
        } else {
            x = screen.width / 2D;
            y = screen.height / 2D;
        }

        boolean handled;
        try {
            handled = screen.mouseScrolled(x, y, horizontalAmount, verticalAmount);
        } catch (Exception ignored) {
            handled = false;
        }

        result.put("x", x);
        result.put("y", y);
        result.put("reason", handled ? "scroll_handled" : "scroll_not_handled");
        return finishInteraction(result, handled, "screen", screen);
    }

    public synchronized Map<String, Object> dragDetailed(String id, double offsetX, double offsetY, int steps, int button) {
        Map<String, Object> result = interaction("drag", id);
        result.put("offsetX", offsetX);
        result.put("offsetY", offsetY);
        result.put("steps", steps);
        result.put("button", button);

        ElementRef ref = resolveRef(id);
        Screen screen = mc.currentScreen;
        if (ref == null || screen == null) {
            result.put("reason", ref == null ? "element_not_found" : "no_active_screen");
            return finishInteraction(result, false, "none", screen);
        }

        refreshCoordinates(ref);
        if (!ref.clickableCoordinates()) {
            result.put("reason", "missing_click_coordinates");
            return finishInteraction(result, false, "none", screen);
        }

        int clampedSteps = Math.max(1, Math.min(64, steps));
        double startX = ref.centerX();
        double startY = ref.centerY();
        Click startClick = new Click(startX, startY, new MouseInput(button, 0));

        boolean pressed;
        try {
            pressed = screen.mouseClicked(startClick, false);
        } catch (Exception ignored) {
            pressed = false;
        }

        boolean dragged = false;
        double previousX = startX;
        double previousY = startY;

        for (int i = 1; i <= clampedSteps; i++) {
            double ratio = (double) i / clampedSteps;
            double nextX = startX + offsetX * ratio;
            double nextY = startY + offsetY * ratio;
            double dx = nextX - previousX;
            double dy = nextY - previousY;
            Click dragClick = new Click(nextX, nextY, new MouseInput(button, 0));

            try {
                dragged = screen.mouseDragged(dragClick, dx, dy) || dragged;
            } catch (Exception ignored) {
                // Keep trying to release.
            }

            previousX = nextX;
            previousY = nextY;
        }

        boolean released;
        try {
            released = screen.mouseReleased(new Click(previousX, previousY, new MouseInput(button, 0)));
        } catch (Exception ignored) {
            released = false;
        }

        boolean success = pressed || dragged || released;
        result.put("startX", startX);
        result.put("startY", startY);
        result.put("endX", previousX);
        result.put("endY", previousY);
        result.put("pressed", pressed);
        result.put("dragged", dragged);
        result.put("released", released);
        result.put("reason", success ? "drag_handled" : "drag_not_handled");
        return finishInteraction(result, success, "screen", screen);
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
            elements.add(mapVanillaElement(child, null));
        }

        return elements;
    }

    private Map<String, Object> mapVanillaElement(Element element, EntryListWidget<?> owningList) {
        String id = nextId("v");

        ElementRef ref = new ElementRef(id, element);
        refs.put(id, ref);

        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("id", id);
        mapped.put("engine", "vanilla");
        putClassMetadata(mapped, element.getClass());

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

        if (owningList != null && element instanceof Widget && !(element instanceof ClickableWidget)) {
            mapped.put("selected", isSelectedInList(owningList, element));
        }

        // Extract label from list entries via narration.
        if (element instanceof AlwaysSelectedEntryListWidget.Entry<?> entry) {
            Text narration = entry.getNarration();
            if (narration != null) {
                mapped.putIfAbsent("label", narration.getString());
            }
        }

        if (element instanceof TextFieldWidget textFieldWidget) {
            mapped.put("text", textFieldWidget.getText());
            mapped.put("editable", textFieldWidget.isActive());
        }

        ScreenDomActionHints.addVanillaHints(mapped, element, owningList, ref.x, ref.y, ref.width, ref.height);

        EntryListWidget<?> nextOwningList = element instanceof EntryListWidget<?> entryListWidget ? entryListWidget : owningList;
        if (element instanceof ParentElement parentElement) {
            List<Map<String, Object>> children = new ArrayList<>();
            for (Element child : parentElement.children()) {
                if (child == element) continue;
                children.add(mapVanillaElement(child, nextOwningList));
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
        putClassMetadata(mapped, widget.getClass());
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

        ScreenDomActionHints.addMeteorHints(mapped, widget, ref.width, ref.height);

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

    private Map<String, Object> classMetadata(Class<?> type) {
        String className = type.getName();
        String mappedClassName = nameMappingService.mapClassName(className);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("class", className);
        metadata.put("classMapped", mappedClassName);
        metadata.put("type", nameMappingService.simpleName(className));
        metadata.put("typeMapped", nameMappingService.simpleName(mappedClassName));
        return metadata;
    }

    private void putClassMetadata(Map<String, Object> target, Class<?> type) {
        target.putAll(classMetadata(type));
    }

    private Map<String, Object> interaction(String action, String elementId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", action);
        result.put("elementId", elementId);
        result.put("success", false);
        return result;
    }

    private Map<String, Object> finishInteraction(Map<String, Object> result, boolean success, String path, Screen screenBefore) {
        result.put("success", success);
        result.put("path", path);

        Screen screenAfter = mc.currentScreen;
        result.put("screenChanged", screenBefore != screenAfter);
        result.put("screenAfter", screenAfter == null ? null : classMetadata(screenAfter.getClass()));
        return result;
    }

    private void refreshCoordinates(ElementRef ref) {
        if (ref.backing instanceof Widget widget) {
            ref.x = widget.getX();
            ref.y = widget.getY();
            ref.width = widget.getWidth();
            ref.height = widget.getHeight();
        }
    }

    private boolean typeChars(Screen screen, String text) {
        boolean handled = false;
        for (int codepoint : text.codePoints().toArray()) {
            try {
                handled = screen.charTyped(new CharInput(codepoint, 0)) || handled;
            } catch (Exception ignored) {
                // Continue typing remaining characters.
            }
        }
        return handled;
    }

    private boolean pressKey(Screen screen, int keyCode, int modifiers, int repeat, boolean release) {
        boolean handled = false;
        KeyInput input = new KeyInput(keyCode, 0, modifiers);

        for (int i = 0; i < repeat; i++) {
            try {
                handled = screen.keyPressed(input) || handled;
            } catch (Exception ignored) {
                // Continue attempts.
            }
        }

        if (release) {
            try {
                handled = screen.keyReleased(input) || handled;
            } catch (Exception ignored) {
                // Ignore release failure.
            }
        }

        return handled;
    }

    private boolean invokeScreenClick(Screen screen, Click click, boolean doubled) {
        try {
            boolean pressed = screen.mouseClicked(click, doubled);
            boolean released = screen.mouseReleased(click);
            return pressed || released;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean invokeElementClick(Element element, Click click, boolean doubled) {
        try {
            boolean pressed = element.mouseClicked(click, doubled);
            boolean released = pressed && element.mouseReleased(click);
            return pressed || released;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isEntryElement(Screen screen, Element element) {
        return element instanceof AlwaysSelectedEntryListWidget.Entry<?>;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean isSelectedInList(EntryListWidget<?> entryList, Element element) {
        EntryListWidget rawList = (EntryListWidget) entryList;
        return rawList.getSelectedOrNull() == element;
    }

    private boolean isEntrySelected(Screen screen, Element entry) {
        EntryListWidget<?> owner = findOwningEntryList(screen, entry);
        return owner != null && isSelectedInList(owner, entry);
    }

    private boolean trySelectEntryAtCoordinates(Screen screen, Click click, boolean doubled) {
        List<EntryListWidget<?>> entryLists = new ArrayList<>();
        for (Element child : screen.children()) {
            collectEntryLists(child, entryLists);
        }

        for (EntryListWidget<?> entryList : entryLists) {
            Element hovered = entryList.hoveredElement(click.x(), click.y()).orElse(null);
            if (hovered instanceof AlwaysSelectedEntryListWidget.Entry<?> && selectEntry(screen, entryList, hovered, click, doubled)) {
                return true;
            }
        }

        return false;
    }

    private boolean trySelectEntryFallback(Screen screen, Element entry, Click click, boolean doubled) {
        EntryListWidget<?> owner = findOwningEntryList(screen, entry);
        if (owner == null || !owner.children().contains(entry)) {
            return false;
        }

        return selectEntry(screen, owner, entry, click, doubled);
    }

    private void collectEntryLists(Element element, List<EntryListWidget<?>> entryLists) {
        if (element instanceof EntryListWidget<?> entryList) {
            entryLists.add(entryList);
        }

        if (element instanceof ParentElement parent) {
            for (Element child : parent.children()) {
                if (child == element) continue;
                collectEntryLists(child, entryLists);
            }
        }
    }

    private EntryListWidget<?> findOwningEntryList(Screen screen, Element entry) {
        for (Element child : screen.children()) {
            EntryListWidget<?> found = findOwningEntryList(child, entry);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private EntryListWidget<?> findOwningEntryList(Element element, Element entry) {
        if (element instanceof EntryListWidget<?> entryList && entryList.children().contains(entry)) {
            return entryList;
        }

        if (element instanceof ParentElement parent) {
            for (Element child : parent.children()) {
                if (child == element) continue;
                EntryListWidget<?> found = findOwningEntryList(child, entry);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private boolean selectEntry(Screen screen, EntryListWidget<?> entryList, Element entry, Click click, boolean doubled) {
        try {
            if (!entryList.children().contains(entry)) {
                return false;
            }

            screen.setFocused(entryList);
            entryList.setFocused(entry);

            if (click != null) {
                entry.mouseClicked(click, doubled);
                entry.mouseReleased(click);
            }

            return isSelectedInList(entryList, entry);
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

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
import meteordevelopment.meteorclient.systems.modules.Module;
import com.mcdxai.meteortestharness.mixin.KeyboardInvoker;
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
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class ScreenDomService {
    private static final Field WIDGET_SCREEN_ROOT_FIELD = resolveWidgetScreenRootField();
    private static final int SNAPSHOT_CACHE_LIMIT = 8;
    private static final int DEFAULT_QUERY_LIMIT = 32;
    private static final int MAX_QUERY_LIMIT = 512;

    private final Map<String, ElementRef> refs = new HashMap<>();
    private final LinkedHashMap<String, DomSnapshot> snapshotCache = new LinkedHashMap<>();
    private final NameMappingService nameMappingService;
    private int idCounter = 0;
    private long snapshotCounter = 0;
    private String latestSnapshotId;

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
        payload.put("mappingBundledSource", nameMappingService.getBundledMappingsSource());
        payload.put("mappingBundledError", nameMappingService.getBundledMappingsError());

        if (screen == null) {
            payload.put("screen", null);
            payload.put("elements", List.of());
            finalizeAndCacheSnapshot(payload, List.of(), null);
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
            if (screenClass.endsWith(".ModulesScreen")) {
                payload.put("screenHints", List.of(
                    "Module rows expose moduleName/moduleTitle/category/active metadata.",
                    "Use click_dom_element with button=0 to toggle modules and button=1 to open module settings."
                ));
            }
        } else {
            elements = buildVanillaDom(screen);
            payload.put("engine", "vanilla");
        }

        payload.put("elements", elements);
        finalizeAndCacheSnapshot(payload, elements, screenInfo);
        return payload;
    }

    public synchronized Map<String, Object> snapshotSummary(boolean refresh) {
        DomSnapshot snapshot = refresh ? refreshSnapshotForQuery() : resolveSnapshotForRead(null, true);
        if (snapshot == null) {
            return Map.of("hasSnapshot", false, "reason", "no_snapshot");
        }

        int actionable = 0;
        int clickable = 0;
        int moduleEntries = 0;

        for (Map<String, Object> element : snapshot.elementsById.values()) {
            if (asBoolean(element.get("actionable"), false)) actionable++;
            if (asBoolean(element.get("clickable"), false)) clickable++;
            if ("module_entry".equals(String.valueOf(element.get("role")))) moduleEntries++;
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("hasSnapshot", true);
        summary.put("snapshotId", snapshot.id);
        summary.put("snapshotCreatedAtMs", snapshot.createdAtMs);
        summary.put("screenSignature", snapshot.screenSignature);
        summary.put("hasScreen", asBoolean(snapshot.payload.get("hasScreen"), false));
        summary.put("engine", snapshot.payload.get("engine"));
        summary.put("elementCount", snapshot.elementCount);
        summary.put("actionableCount", actionable);
        summary.put("clickableCount", clickable);
        summary.put("moduleEntryCount", moduleEntries);
        summary.put("screen", snapshot.payload.get("screen"));
        return summary;
    }

    public synchronized Map<String, Object> findElements(
        String snapshotId,
        Map<String, Object> filters,
        int limit,
        List<Object> fieldsRaw,
        boolean includeChildren
    ) {
        DomSnapshot snapshot = resolveSnapshotForRead(snapshotId, true);
        if (snapshot == null) {
            return Map.of("success", false, "reason", "snapshot_not_found", "snapshotId", snapshotId);
        }

        int clampedLimit = clampLimit(limit);
        List<String> fields = normalizeFields(fieldsRaw);

        List<Map<String, Object>> allMatches = findMatchingElements(snapshot, filters);
        int returned = Math.min(allMatches.size(), clampedLimit);

        List<Map<String, Object>> projected = new ArrayList<>(returned);
        for (int i = 0; i < returned; i++) {
            projected.add(projectElement(allMatches.get(i), fields, includeChildren, includeChildren ? -1 : 0));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("snapshotId", snapshot.id);
        response.put("screenSignature", snapshot.screenSignature);
        response.put("totalMatches", allMatches.size());
        response.put("returned", projected.size());
        response.put("limit", clampedLimit);
        response.put("elements", projected);
        return response;
    }

    public synchronized Map<String, Object> getElement(
        String snapshotId,
        String elementId,
        List<Object> fieldsRaw,
        boolean includeChildren
    ) {
        DomSnapshot snapshot = resolveSnapshotForRead(snapshotId, true);
        if (snapshot == null) {
            return Map.of("success", false, "reason", "snapshot_not_found", "snapshotId", snapshotId);
        }

        if (elementId == null || elementId.isBlank()) {
            return Map.of("success", false, "reason", "element_id_required", "snapshotId", snapshot.id);
        }

        Map<String, Object> element = snapshot.elementsById.get(elementId);
        if (element == null) {
            return Map.of("success", false, "reason", "element_not_found", "snapshotId", snapshot.id, "elementId", elementId);
        }

        List<String> fields = normalizeFields(fieldsRaw);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("snapshotId", snapshot.id);
        response.put("screenSignature", snapshot.screenSignature);
        response.put("element", projectElement(element, fields, includeChildren, includeChildren ? -1 : 0));
        return response;
    }

    public synchronized Map<String, Object> getSubtree(
        String snapshotId,
        String elementId,
        int depth,
        List<Object> fieldsRaw
    ) {
        DomSnapshot snapshot = resolveSnapshotForRead(snapshotId, true);
        if (snapshot == null) {
            return Map.of("success", false, "reason", "snapshot_not_found", "snapshotId", snapshotId);
        }

        if (elementId == null || elementId.isBlank()) {
            return Map.of("success", false, "reason", "element_id_required", "snapshotId", snapshot.id);
        }

        Map<String, Object> element = snapshot.elementsById.get(elementId);
        if (element == null) {
            return Map.of("success", false, "reason", "element_not_found", "snapshotId", snapshot.id, "elementId", elementId);
        }

        int clampedDepth = Math.max(0, Math.min(32, depth));
        List<String> fields = normalizeFields(fieldsRaw);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("snapshotId", snapshot.id);
        response.put("screenSignature", snapshot.screenSignature);
        response.put("depth", clampedDepth);
        response.put("element", projectElement(element, fields, true, clampedDepth));
        return response;
    }

    public synchronized Map<String, Object> clickByQueryDetailed(
        Map<String, Object> filters,
        int index,
        int button,
        boolean doubled
    ) {
        DomSnapshot snapshot = refreshSnapshotForQuery();
        if (snapshot == null) {
            return Map.of("success", false, "reason", "no_snapshot");
        }

        List<Map<String, Object>> matches = findMatchingElements(snapshot, filters);
        int selectedIndex = Math.max(0, index);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("snapshotId", snapshot.id);
        response.put("screenSignature", snapshot.screenSignature);
        response.put("matchCount", matches.size());
        response.put("selectedIndex", selectedIndex);

        if (matches.isEmpty()) {
            response.put("reason", "query_no_match");
            response.put("success", false);
            return response;
        }

        if (selectedIndex >= matches.size()) {
            response.put("reason", "query_index_out_of_range");
            response.put("success", false);
            return response;
        }

        Map<String, Object> selected = matches.get(selectedIndex);
        String elementId = String.valueOf(selected.get("id"));
        response.put("selectedElement", projectElement(selected, List.of(
            "id", "label", "role", "moduleName", "moduleTitle", "moduleCategory", "actions", "x", "y", "width", "height"
        ), false, 0));

        Map<String, Object> interaction = clickDetailed(elementId, button, doubled);
        response.put("interaction", interaction);
        response.put("success", asBoolean(interaction.get("success"), false));
        if (!response.containsKey("reason")) {
            response.put("reason", interaction.getOrDefault("reason", "click_result"));
        }
        return response;
    }

    public synchronized Map<String, Object> setTextByQueryDetailed(
        Map<String, Object> filters,
        int index,
        String text,
        boolean submit,
        boolean typeCharacters,
        boolean clearFirst
    ) {
        DomSnapshot snapshot = refreshSnapshotForQuery();
        if (snapshot == null) {
            return Map.of("success", false, "reason", "no_snapshot");
        }

        List<Map<String, Object>> matches = findMatchingElements(snapshot, filters);
        int selectedIndex = Math.max(0, index);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("snapshotId", snapshot.id);
        response.put("screenSignature", snapshot.screenSignature);
        response.put("matchCount", matches.size());
        response.put("selectedIndex", selectedIndex);

        if (matches.isEmpty()) {
            response.put("reason", "query_no_match");
            response.put("success", false);
            return response;
        }

        if (selectedIndex >= matches.size()) {
            response.put("reason", "query_index_out_of_range");
            response.put("success", false);
            return response;
        }

        Map<String, Object> selected = matches.get(selectedIndex);
        String elementId = String.valueOf(selected.get("id"));
        response.put("selectedElement", projectElement(selected, List.of(
            "id", "label", "role", "moduleName", "moduleTitle", "actions", "x", "y", "width", "height", "text"
        ), false, 0));

        Map<String, Object> interaction = setTextDetailed(elementId, text == null ? "" : text, submit, typeCharacters, clearFirst);
        response.put("interaction", interaction);
        response.put("success", asBoolean(interaction.get("success"), false));
        if (!response.containsKey("reason")) {
            response.put("reason", interaction.getOrDefault("reason", "set_text_result"));
        }
        return response;
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
            double targetX = ref.centerX();
            double targetY = ref.centerY();
            click = buildScreenClick(screen, ref, button, targetX, targetY);
            result.put("targetX", targetX);
            result.put("targetY", targetY);
            result.put("x", click.x());
            result.put("y", click.y());
        }
        result.put("coordinatesAvailable", click != null);

        boolean handled = false;

        // Path 1: Screen-level click through vanilla dispatch chain.
        if (click != null) {
            handled = invokeScreenClick(screen, click, doubled);
            if (mc.currentScreen != screen) {
                if (button == 1 && isMeteorModuleEntry(ref)) {
                    return finishMeteorModuleTransition(result, ref, screen, "screen");
                }

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
                    invokeScreenClick(screen, buildScreenClick(screen, ref, 0, ref.centerX(), ref.centerY()), false);
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
                invokeScreenClick(screen, buildScreenClick(screen, ref, 0, ref.centerX(), ref.centerY()), false);
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

        Integer keyCode = ScreenKeyCodec.parseKeyCode(keyName);
        if (keyCode == null) {
            result.put("reason", "unknown_key");
            return finishInteraction(result, false, "none", mc.currentScreen);
        }

        int clampedRepeat = Math.max(1, Math.min(32, repeat));
        Screen screen = mc.currentScreen;
        Screen screenBefore = screen;

        boolean handled = dispatchKeyboardKey(keyCode, modifiers, clampedRepeat, release, result);
        String path = screen == null ? "global" : "screen";

        if (mc.currentScreen != screenBefore) {
            result.put("screenTransitionDetected", true);
        }

        result.put("keyCode", keyCode);
        result.put("reason", handled ? "key_handled" : "key_not_handled");
        return finishInteraction(result, handled, path, screen);
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
        double startTargetX = ref.centerX();
        double startTargetY = ref.centerY();
        double coordinateScale = coordinateScaleForScreenDispatch(screen, ref.backing);
        double dispatchOffsetX = offsetX / coordinateScale;
        double dispatchOffsetY = offsetY / coordinateScale;

        double startX = startTargetX / coordinateScale;
        double startY = startTargetY / coordinateScale;
        Click startClick = new Click(startX, startY, new MouseInput(button, 0));

        try {
            screen.mouseMoved(startX, startY);
        } catch (Exception ignored) {
            // Best effort.
        }

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
            double nextX = startX + dispatchOffsetX * ratio;
            double nextY = startY + dispatchOffsetY * ratio;
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
        result.put("startTargetX", startTargetX);
        result.put("startTargetY", startTargetY);
        result.put("dispatchOffsetX", dispatchOffsetX);
        result.put("dispatchOffsetY", dispatchOffsetY);
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

    private void finalizeAndCacheSnapshot(Map<String, Object> payload, List<Map<String, Object>> rootElements, Map<String, Object> screenInfo) {
        long now = System.currentTimeMillis();
        String snapshotId = "s-" + (++snapshotCounter);
        int elementCount = countElements(rootElements);
        String screenSignature = buildScreenSignature(payload, screenInfo, elementCount);

        payload.put("snapshotId", snapshotId);
        payload.put("snapshotCreatedAtMs", now);
        payload.put("screenSignature", screenSignature);
        payload.put("elementCount", elementCount);

        Map<String, Map<String, Object>> elementIndex = new LinkedHashMap<>();
        indexElements(rootElements, elementIndex);

        DomSnapshot snapshot = new DomSnapshot(snapshotId, now, screenSignature, elementCount, payload, rootElements, elementIndex);
        snapshotCache.put(snapshotId, snapshot);
        latestSnapshotId = snapshotId;

        while (snapshotCache.size() > SNAPSHOT_CACHE_LIMIT) {
            String oldestId = snapshotCache.keySet().iterator().next();
            snapshotCache.remove(oldestId);
            if (oldestId.equals(latestSnapshotId)) {
                latestSnapshotId = null;
            }
        }
    }

    private DomSnapshot refreshSnapshotForQuery() {
        snapshot();
        return latestSnapshotId == null ? null : snapshotCache.get(latestSnapshotId);
    }

    private DomSnapshot resolveSnapshotForRead(String snapshotId, boolean refreshIfMissing) {
        if (snapshotId != null && !snapshotId.isBlank()) {
            DomSnapshot snapshot = snapshotCache.get(snapshotId);
            if (snapshot != null) return snapshot;
            return null;
        }

        if (latestSnapshotId != null) {
            DomSnapshot snapshot = snapshotCache.get(latestSnapshotId);
            if (snapshot != null) return snapshot;
        }

        return refreshIfMissing ? refreshSnapshotForQuery() : null;
    }

    private String buildScreenSignature(Map<String, Object> payload, Map<String, Object> screenInfo, int elementCount) {
        if (screenInfo == null) {
            return "none|" + elementCount;
        }

        String className = asString(screenInfo.get("class"), "none");
        String title = asString(screenInfo.get("title"), "");
        String engine = asString(payload.get("engine"), "unknown");
        return className + "|" + title + "|" + engine + "|" + elementCount;
    }

    private int countElements(List<Map<String, Object>> elements) {
        int total = 0;
        for (Map<String, Object> element : elements) {
            total++;
            total += countElements(childMaps(element));
        }
        return total;
    }

    private void indexElements(List<Map<String, Object>> elements, Map<String, Map<String, Object>> index) {
        for (Map<String, Object> element : elements) {
            Object id = element.get("id");
            if (id != null) {
                index.put(String.valueOf(id), element);
            }
            indexElements(childMaps(element), index);
        }
    }

    private List<Map<String, Object>> findMatchingElements(DomSnapshot snapshot, Map<String, Object> filters) {
        if (snapshot == null) return List.of();

        List<Map<String, Object>> matches = new ArrayList<>();
        for (Map<String, Object> element : snapshot.elementsById.values()) {
            if (matchesFilters(element, filters)) {
                matches.add(element);
            }
        }
        return matches;
    }

    private boolean matchesFilters(Map<String, Object> element, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) return true;

        boolean exact = asBoolean(filters.get("exact"), false);
        boolean regex = asBoolean(filters.get("regex"), false);
        boolean caseSensitive = asBoolean(filters.get("case_sensitive"), false);
        if (!filters.containsKey("case_sensitive")) {
            caseSensitive = asBoolean(filters.get("caseSensitive"), false);
        }

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = normalizeFilterKey(entry.getKey());
            Object expected = entry.getValue();

            if (key.equals("exact") || key.equals("regex") || key.equals("case_sensitive") || key.equals("casesensitive")) {
                continue;
            }

            if (key.equals("text") || key.equals("query") || key.equals("q")) {
                if (!matchesGlobalText(element, expected, exact, regex, caseSensitive)) return false;
                continue;
            }

            if (key.equals("action") || key.equals("actions_any")) {
                if (!matchesActionsAny(element, expected, exact, regex, caseSensitive)) return false;
                continue;
            }

            if (key.equals("actions_all")) {
                if (!matchesActionsAll(element, expected, exact, regex, caseSensitive)) return false;
                continue;
            }

            String elementKey = resolveElementKey(key);
            Object actual = element.get(elementKey);
            if (!matchesExpected(actual, expected, exact, regex, caseSensitive)) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesGlobalText(
        Map<String, Object> element,
        Object expected,
        boolean exact,
        boolean regex,
        boolean caseSensitive
    ) {
        List<String> needles = asStringList(expected);
        if (needles.isEmpty()) return false;

        List<String> haystacks = List.of(
            asString(element.get("id"), ""),
            asString(element.get("label"), ""),
            asString(element.get("text"), ""),
            asString(element.get("role"), ""),
            asString(element.get("moduleName"), ""),
            asString(element.get("moduleTitle"), ""),
            asString(element.get("moduleCategory"), ""),
            asString(element.get("type"), ""),
            asString(element.get("typeMapped"), ""),
            asString(element.get("class"), ""),
            asString(element.get("classMapped"), "")
        );

        for (String needle : needles) {
            if (needle.isBlank()) continue;
            for (String haystack : haystacks) {
                if (haystack.isBlank()) continue;
                if (matchesString(haystack, needle, exact, regex, caseSensitive)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean matchesActionsAny(
        Map<String, Object> element,
        Object expected,
        boolean exact,
        boolean regex,
        boolean caseSensitive
    ) {
        List<String> expectedActions = asStringList(expected);
        if (expectedActions.isEmpty()) return false;

        List<String> actions = asStringList(element.get("actions"));
        if (actions.isEmpty()) return false;

        for (String expectedAction : expectedActions) {
            for (String action : actions) {
                if (matchesString(action, expectedAction, exact, regex, caseSensitive)) return true;
            }
        }
        return false;
    }

    private boolean matchesActionsAll(
        Map<String, Object> element,
        Object expected,
        boolean exact,
        boolean regex,
        boolean caseSensitive
    ) {
        List<String> expectedActions = asStringList(expected);
        if (expectedActions.isEmpty()) return false;

        List<String> actions = asStringList(element.get("actions"));
        if (actions.isEmpty()) return false;

        for (String expectedAction : expectedActions) {
            boolean found = false;
            for (String action : actions) {
                if (matchesString(action, expectedAction, exact, regex, caseSensitive)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        return true;
    }

    private boolean matchesExpected(
        Object actual,
        Object expected,
        boolean exact,
        boolean regex,
        boolean caseSensitive
    ) {
        if (expected instanceof List<?> expectedList) {
            if (expectedList.isEmpty()) return false;
            for (Object item : expectedList) {
                if (matchesExpected(actual, item, exact, regex, caseSensitive)) return true;
            }
            return false;
        }

        if (actual instanceof List<?> actualList) {
            for (Object item : actualList) {
                if (matchesExpected(item, expected, exact, regex, caseSensitive)) return true;
            }
            return false;
        }

        if (expected instanceof Boolean expectedBoolean) {
            return asBoolean(actual, false) == expectedBoolean;
        }

        if (expected instanceof Number expectedNumber) {
            if (!(actual instanceof Number actualNumber)) return false;
            return Double.compare(actualNumber.doubleValue(), expectedNumber.doubleValue()) == 0;
        }

        String expectedString = asString(expected, "");
        if (expectedString.isBlank()) {
            return asString(actual, "").isBlank();
        }

        String actualString = asString(actual, "");
        return matchesString(actualString, expectedString, exact, regex, caseSensitive);
    }

    private boolean matchesString(String actual, String expected, boolean exact, boolean regex, boolean caseSensitive) {
        if (actual == null || expected == null) return false;

        if (regex) {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            try {
                return Pattern.compile(expected, flags).matcher(actual).find();
            } catch (PatternSyntaxException ignored) {
                return false;
            }
        }

        if (exact) {
            return caseSensitive ? actual.equals(expected) : actual.equalsIgnoreCase(expected);
        }

        if (!caseSensitive) {
            return actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
        }

        return actual.contains(expected);
    }

    private String normalizeFilterKey(String key) {
        if (key == null) return "";
        return key.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private String resolveElementKey(String normalizedFilterKey) {
        return switch (normalizedFilterKey) {
            case "module_name", "modulename" -> "moduleName";
            case "module_title", "moduletitle" -> "moduleTitle";
            case "module_category", "modulecategory" -> "moduleCategory";
            case "module_active", "moduleactive" -> "moduleActive";
            case "class_mapped", "classmapped" -> "classMapped";
            case "type_mapped", "typemapped" -> "typeMapped";
            case "left_click_action", "leftclickaction" -> "leftClickAction";
            case "right_click_action", "rightclickaction" -> "rightClickAction";
            case "module_ui_label", "moduleuilabel" -> "moduleUiLabel";
            default -> normalizedFilterKey;
        };
    }

    private List<String> normalizeFields(List<Object> fieldsRaw) {
        if (fieldsRaw == null || fieldsRaw.isEmpty()) return List.of();

        Set<String> fields = new LinkedHashSet<>();
        for (Object raw : fieldsRaw) {
            if (raw == null) continue;
            String field = String.valueOf(raw).trim();
            if (field.isEmpty()) continue;
            fields.add(resolveElementKey(normalizeFilterKey(field)));
        }
        fields.add("id");
        return new ArrayList<>(fields);
    }

    private Map<String, Object> projectElement(
        Map<String, Object> element,
        List<String> fields,
        boolean includeChildren,
        int depth
    ) {
        if (element == null) return Map.of();
        return projectElementRecursive(element, fields, includeChildren, depth);
    }

    private Map<String, Object> projectElementRecursive(
        Map<String, Object> element,
        List<String> fields,
        boolean includeChildren,
        int depth
    ) {
        Map<String, Object> projected = new LinkedHashMap<>();

        if (fields == null || fields.isEmpty()) {
            for (Map.Entry<String, Object> entry : element.entrySet()) {
                if ("children".equals(entry.getKey())) continue;
                projected.put(entry.getKey(), entry.getValue());
            }
        } else {
            for (String field : fields) {
                if ("children".equals(field)) continue;
                if (element.containsKey(field)) {
                    projected.put(field, element.get(field));
                }
            }
            projected.putIfAbsent("id", element.get("id"));
        }

        List<Map<String, Object>> children = childMaps(element);
        if (includeChildren && depth != 0 && !children.isEmpty()) {
            int nextDepth = depth < 0 ? -1 : depth - 1;
            List<Map<String, Object>> projectedChildren = new ArrayList<>(children.size());
            for (Map<String, Object> child : children) {
                projectedChildren.add(projectElementRecursive(child, fields, true, nextDepth));
            }
            projected.put("children", projectedChildren);
        } else if (!includeChildren) {
            projected.put("childCount", children.size());
        }

        return projected;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> childMaps(Map<String, Object> element) {
        Object children = element.get("children");
        if (!(children instanceof List<?> rawChildren) || rawChildren.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> mappedChildren = new ArrayList<>();
        for (Object raw : rawChildren) {
            if (raw instanceof Map<?, ?> child) {
                mappedChildren.add((Map<String, Object>) child);
            }
        }
        return mappedChildren;
    }

    private int clampLimit(int limit) {
        int requested = limit <= 0 ? DEFAULT_QUERY_LIMIT : limit;
        return Math.max(1, Math.min(MAX_QUERY_LIMIT, requested));
    }

    private boolean asBoolean(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean booleanValue) return booleanValue;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String asString(Object value, String fallback) {
        if (value == null) return fallback;
        String str = String.valueOf(value);
        return str == null ? fallback : str;
    }

    private List<String> asStringList(Object value) {
        if (value == null) return List.of();
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object item : list) {
                if (item == null) continue;
                String str = String.valueOf(item).trim();
                if (!str.isEmpty()) out.add(str);
            }
            return out;
        }
        String single = String.valueOf(value).trim();
        if (single.isEmpty()) return List.of();
        return List.of(single);
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

        MeteorModuleMetadata moduleMetadata = extractMeteorModuleMetadata(widget);
        if (moduleMetadata != null) {
            mapped.put("role", "module_entry");
            mapped.put("moduleName", moduleMetadata.name);
            mapped.put("moduleTitle", moduleMetadata.title);
            mapped.put("moduleCategory", moduleMetadata.category);
            mapped.put("moduleActive", moduleMetadata.active);
            mapped.put("moduleUiLabel", moduleMetadata.uiLabel);
            mapped.put("leftClickAction", "toggle_module");
            mapped.put("rightClickAction", "open_module_settings");
            mapped.put("label", moduleMetadata.uiLabel);

            ref.role = "module_entry";
            ref.moduleName = moduleMetadata.name;
            ref.moduleTitle = moduleMetadata.title;
        }

        ScreenDomActionHints.addMeteorHints(mapped, widget, ref.width, ref.height, moduleMetadata != null);

        if (widget instanceof WContainer container) {
            List<Map<String, Object>> children = new ArrayList<>();
            for (Cell<?> cell : container.cells) {
                children.add(mapMeteorWidget(cell.widget()));
            }
            mapped.put("children", children);
        }

        return mapped;
    }

    private MeteorModuleMetadata extractMeteorModuleMetadata(WWidget widget) {
        if (!(widget instanceof WPressable)) {
            return null;
        }

        Object moduleObject = findFieldValueByType(widget, Module.class.getName());
        if (!(moduleObject instanceof Module module)) {
            return null;
        }

        String uiLabel = null;
        Object titleFieldValue = findFieldValue(widget, "title");
        if (titleFieldValue instanceof String titleText && !titleText.isBlank()) {
            uiLabel = titleText;
        }

        if (uiLabel == null || uiLabel.isBlank()) {
            uiLabel = module.title != null && !module.title.isBlank() ? module.title : module.name;
        }

        String category = module.category == null ? "" : module.category.name;
        return new MeteorModuleMetadata(module.name, module.title, category, uiLabel, module.isActive());
    }

    private Object findFieldValueByType(Object target, String fieldTypeName) {
        if (target == null || fieldTypeName == null || fieldTypeName.isBlank()) {
            return null;
        }

        Class<?> type = target.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.getType().getName().equals(fieldTypeName)) continue;

                try {
                    field.setAccessible(true);
                    return field.get(target);
                } catch (Exception ignored) {
                    // Keep scanning.
                }
            }
            type = type.getSuperclass();
        }

        return null;
    }

    private Object findFieldValue(Object target, String fieldName) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }

        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
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
        } else if (ref.backing instanceof WWidget widget) {
            ref.x = widget.x;
            ref.y = widget.y;
            ref.width = widget.width;
            ref.height = widget.height;
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

    private boolean dispatchKeyboardKey(int keyCode, int modifiers, int repeat, boolean release, Map<String, Object> result) {
        if (mc.keyboard == null || mc.getWindow() == null) {
            result.put("dispatchError", "keyboard_unavailable");
            return false;
        }

        if (!(mc.keyboard instanceof KeyboardInvoker invoker)) {
            result.put("dispatchError", "keyboard_invoker_unavailable");
            return false;
        }

        long windowHandle = mc.getWindow().getHandle();
        int scancode = GLFW.glfwGetKeyScancode(keyCode);
        if (scancode < 0) {
            scancode = 0;
        }

        KeyInput input = new KeyInput(keyCode, scancode, modifiers);
        result.put("scancode", scancode);
        result.put("matchedBindings", countMatchingBindings(input));
        result.put("dispatchMode", "keyboard_onKey");

        int dispatchedEvents = 0;
        try {
            invoker.meteorHarness$invokeOnKey(windowHandle, GLFW.GLFW_PRESS, input);
            dispatchedEvents++;

            for (int i = 1; i < repeat; i++) {
                invoker.meteorHarness$invokeOnKey(windowHandle, GLFW.GLFW_REPEAT, input);
                dispatchedEvents++;
            }

            if (release) {
                invoker.meteorHarness$invokeOnKey(windowHandle, GLFW.GLFW_RELEASE, input);
                dispatchedEvents++;
            }

            result.put("dispatchedEvents", dispatchedEvents);
            return true;
        } catch (Throwable throwable) {
            result.put("dispatchError", throwable.getClass().getSimpleName());
            result.put("dispatchMessage", throwable.getMessage());
            result.put("dispatchedEvents", dispatchedEvents);
            return false;
        }
    }

    private int countMatchingBindings(KeyInput input) {
        if (mc.options == null || mc.options.allKeys == null) {
            return 0;
        }

        int count = 0;
        for (KeyBinding binding : mc.options.allKeys) {
            if (binding != null && binding.matchesKey(input)) {
                count++;
            }
        }
        return count;
    }

    private boolean invokeScreenClick(Screen screen, Click click, boolean doubled) {
        try {
            screen.mouseMoved(click.x(), click.y());
        } catch (Exception ignored) {
            // Best effort hover sync.
        }

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

    private Click buildScreenClick(Screen screen, ElementRef ref, int button, double targetX, double targetY) {
        double scale = coordinateScaleForScreenDispatch(screen, ref.backing);
        return new Click(targetX / scale, targetY / scale, new MouseInput(button, 0));
    }

    private double coordinateScaleForScreenDispatch(Screen screen, Object backing) {
        if (!(screen instanceof WidgetScreen) || !(backing instanceof WWidget)) {
            return 1D;
        }

        double scale = mc.getWindow() == null ? 1D : mc.getWindow().getScaleFactor();
        return scale > 0D ? scale : 1D;
    }

    private boolean isMeteorModuleEntry(ElementRef ref) {
        return "module_entry".equals(ref.role) && ref.moduleName != null && !ref.moduleName.isBlank();
    }

    private Map<String, Object> finishMeteorModuleTransition(Map<String, Object> result, ElementRef ref, Screen screenBefore, String path) {
        Screen screenAfter = mc.currentScreen;
        String expectedTitle = ref.moduleTitle == null || ref.moduleTitle.isBlank() ? ref.moduleName : ref.moduleTitle;
        String actualTitle = screenAfter == null || screenAfter.getTitle() == null ? "" : screenAfter.getTitle().getString();
        boolean isModuleScreen = screenAfter != null && screenAfter.getClass().getName().endsWith(".ModuleScreen");
        boolean titleMatch = isModuleScreen
            && !expectedTitle.isBlank()
            && actualTitle.toLowerCase(Locale.ROOT).contains(expectedTitle.toLowerCase(Locale.ROOT));
        boolean moduleMatch = isModuleScreen && titleMatch;

        result.put("expectedModuleName", ref.moduleName);
        result.put("expectedModuleTitle", ref.moduleTitle);
        result.put("actualScreenTitle", actualTitle);
        result.put("moduleScreen", isModuleScreen);
        result.put("moduleMatch", moduleMatch);
        result.put("reason", moduleMatch ? "screen_changed" : "module_screen_mismatch");
        return finishInteraction(result, moduleMatch, path, screenBefore);
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

    private static final class DomSnapshot {
        private final String id;
        private final long createdAtMs;
        private final String screenSignature;
        private final int elementCount;
        private final Map<String, Object> payload;
        private final List<Map<String, Object>> rootElements;
        private final Map<String, Map<String, Object>> elementsById;

        private DomSnapshot(
            String id,
            long createdAtMs,
            String screenSignature,
            int elementCount,
            Map<String, Object> payload,
            List<Map<String, Object>> rootElements,
            Map<String, Map<String, Object>> elementsById
        ) {
            this.id = id;
            this.createdAtMs = createdAtMs;
            this.screenSignature = screenSignature;
            this.elementCount = elementCount;
            this.payload = payload;
            this.rootElements = rootElements;
            this.elementsById = elementsById;
        }
    }

    private static final class ElementRef {
        private final String id;
        private final Object backing;

        private double x = Double.NaN;
        private double y = Double.NaN;
        private double width = Double.NaN;
        private double height = Double.NaN;

        private String role;
        private String moduleName;
        private String moduleTitle;

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

    private static final class MeteorModuleMetadata {
        private final String name;
        private final String title;
        private final String category;
        private final String uiLabel;
        private final boolean active;

        private MeteorModuleMetadata(String name, String title, String category, String uiLabel, boolean active) {
            this.name = name;
            this.title = title;
            this.category = category;
            this.uiLabel = uiLabel;
            this.active = active;
        }
    }
}

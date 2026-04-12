package com.mcdxai.meteortestharness.dom;

import com.mcdxai.meteortestharness.mixin.KeyboardInvoker;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.mcdxai.meteortestharness.dom.DomValueUtils.asBoolean;
import static com.mcdxai.meteortestharness.dom.DomValueUtils.toBoolean;
import static com.mcdxai.meteortestharness.dom.DomValueUtils.toDouble;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class DomInteractor {
    private final DomSnapshotBuilder snapshotBuilder;
    private final DomQueryEngine queryEngine;
    private final DomEntryListHelper entryListHelper;
    private final DomMetadataHelper metadataHelper;

    public DomInteractor(
        DomSnapshotBuilder snapshotBuilder,
        DomQueryEngine queryEngine,
        DomEntryListHelper entryListHelper,
        DomMetadataHelper metadataHelper
    ) {
        this.snapshotBuilder = snapshotBuilder;
        this.queryEngine = queryEngine;
        this.entryListHelper = entryListHelper;
        this.metadataHelper = metadataHelper;
    }

    public Map<String, Object> clickByQueryDetailed(
        Map<String, Object> filters,
        int index,
        int button,
        boolean doubled
    ) {
        DomSnapshot snapshot = snapshotBuilder.refreshSnapshotForQuery();
        if (snapshot == null) {
            return Map.of("success", false, "reason", "no_snapshot");
        }

        List<Map<String, Object>> matches = queryEngine.findMatchingElements(snapshot, filters);
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
        response.put("selectedElement", queryEngine.projectElement(selected, List.of(
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

    public Map<String, Object> setTextByQueryDetailed(
        Map<String, Object> filters,
        int index,
        String text,
        boolean submit,
        boolean typeCharacters,
        boolean clearFirst
    ) {
        DomSnapshot snapshot = snapshotBuilder.refreshSnapshotForQuery();
        if (snapshot == null) {
            return Map.of("success", false, "reason", "no_snapshot");
        }

        List<Map<String, Object>> matches = queryEngine.findMatchingElements(snapshot, filters);
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
        response.put("selectedElement", queryEngine.projectElement(selected, List.of(
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

    public Map<String, Object> clickDetailed(String id, int button, boolean doubled) {
        Map<String, Object> result = interaction("click", id);
        result.put("button", button);
        result.put("doubleClick", doubled);

        ElementRef ref = snapshotBuilder.resolveRef(id);
        Screen screen = mc.currentScreen;
        if (ref == null || screen == null) {
            result.put("reason", ref == null ? "element_not_found" : "no_active_screen");
            return finishInteraction(result, false, "none", screen);
        }

        result.put("screenBefore", metadataHelper.classMetadata(screen.getClass()));

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

        if (ref.backing instanceof Element element && button == 0 && entryListHelper.isEntryElement(screen, element)) {
            result.put("entryTarget", true);
            result.put("selectedBefore", entryListHelper.isEntrySelected(screen, element));

            if (entryListHelper.isEntrySelected(screen, element)) {
                result.put("selectedAfter", true);
                result.put("reason", "entry_selected_via_screen_dispatch");
                return finishInteraction(result, true, "screen", screen);
            }

            if (click != null && entryListHelper.trySelectEntryAtCoordinates(screen, click, doubled)) {
                result.put("selectedAfter", true);
                result.put("reason", "entry_selected_via_hover_fallback");
                return finishInteraction(result, true, "entry_hover_fallback", screen);
            }

            if (entryListHelper.trySelectEntryFallback(screen, element, click, doubled)) {
                result.put("selectedAfter", true);
                result.put("reason", "entry_selected_via_owner_fallback");
                return finishInteraction(result, true, "entry_owner_fallback", screen);
            }

            result.put("selectedAfter", false);
            handled = false;
        }

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

    public Map<String, Object> setTextDetailed(String id, String text, boolean submit, boolean typeCharacters, boolean clearFirst) {
        Map<String, Object> result = interaction("set_text", id);
        result.put("submit", submit);
        result.put("typeCharacters", typeCharacters);
        result.put("clearFirst", clearFirst);

        ElementRef ref = snapshotBuilder.resolveRef(id);
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

    public Map<String, Object> typeTextDetailed(String id, String text, boolean clearFirst, boolean submit) {
        return setTextDetailed(id, text, submit, true, clearFirst);
    }

    public Map<String, Object> setValueDetailed(String id, Object value) {
        Map<String, Object> result = interaction("set_value", id);
        Screen screen = mc.currentScreen;

        ElementRef ref = snapshotBuilder.resolveRef(id);
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

    public Map<String, Object> pressKeyDetailed(String keyName, int modifiers, int repeat, boolean release) {
        Map<String, Object> result = interaction("press_key", null);
        result.put("key", keyName);
        result.put("modifiers", modifiers);
        result.put("repeat", repeat);
        result.put("release", release);

        Integer keyCode = DomKeyCodec.parseKeyCode(keyName);
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

    public Map<String, Object> scrollDetailed(String id, double verticalAmount, double horizontalAmount) {
        Map<String, Object> result = interaction("scroll", id);
        result.put("verticalAmount", verticalAmount);
        result.put("horizontalAmount", horizontalAmount);

        Screen screen = mc.currentScreen;
        if (screen == null) {
            result.put("reason", "no_active_screen");
            return finishInteraction(result, false, "none", null);
        }

        ElementRef ref = id == null || id.isBlank() ? null : snapshotBuilder.resolveRef(id);
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

    public Map<String, Object> dragDetailed(String id, double offsetX, double offsetY, int steps, int button) {
        Map<String, Object> result = interaction("drag", id);
        result.put("offsetX", offsetX);
        result.put("offsetY", offsetY);
        result.put("steps", steps);
        result.put("button", button);

        ElementRef ref = snapshotBuilder.resolveRef(id);
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
        result.put("screenAfter", screenAfter == null ? null : metadataHelper.classMetadata(screenAfter.getClass()));
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
}

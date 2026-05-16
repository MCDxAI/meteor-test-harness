package io.mcdxai.harness.universal.dom;

import io.mcdxai.harness.universal.adapter.AdapterRegistry;
import io.mcdxai.harness.universal.adapter.WidgetAdapter;
import io.mcdxai.harness.universal.adapter.vanilla.VanillaEntryListHelper;
import io.mcdxai.harness.universal.mixin.KeyboardInvoker;
import io.mcdxai.harness.universal.util.MainThreadInvoker;
import io.wispforest.owo.ui.core.UIComponent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import static io.mcdxai.harness.universal.dom.DomValueUtils.asBoolean;

/**
 * Adapter-driven screen interactor.
 *
 * Public *Detailed methods follow a 3-phase pattern:
 *   1. dispatch the action on the render thread
 *   2. settle for ~150ms (3 ticks @ 20tps) on the calling thread so Minecraft processes
 *      keybind events, screen transitions, and async layout passes
 *   3. capture post-action screen state on the render thread
 *
 * Critical: these methods MUST be called from a non-render thread (the MCP servlet thread).
 * The settle sleeps on the calling thread; if it ran on the render thread, it would block
 * Minecraft from ticking the very state changes we're waiting on.
 */
public final class DomInteractor {
    /** Tick budget for screen transitions / layout settling after an interaction. */
    private static final int SETTLE_MS = 150;
    /** Bound on each main-thread bounce; protects against an unresponsive render thread. */
    private static final Duration MAIN_THREAD_TIMEOUT = Duration.ofSeconds(10);

    private final DomSnapshotBuilder snapshotBuilder;
    private final DomQueryEngine queryEngine;
    private final AdapterRegistry registry;
    private final VanillaEntryListHelper entryListHelper = new VanillaEntryListHelper();

    public DomInteractor(DomSnapshotBuilder snapshotBuilder, DomQueryEngine queryEngine, AdapterRegistry registry) {
        this.snapshotBuilder = snapshotBuilder;
        this.queryEngine = queryEngine;
        this.registry = registry;
    }

    // ---------- Query-driven entry points ----------

    public Map<String, Object> clickByQueryDetailed(Map<String, Object> filters, int index, int button, boolean doubled) {
        DomSnapshot snapshot = onMain(snapshotBuilder::refreshSnapshotForQuery);
        if (snapshot == null) return Map.of("success", false, "reason", "no_snapshot");

        List<Map<String, Object>> matches = queryEngine.findMatchingElements(snapshot, filters);
        int selectedIndex = Math.max(0, index);

        Map<String, Object> response = baseQueryResponse(snapshot, matches, selectedIndex);
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
            "id", "label", "role", "componentId", "actions", "x", "y", "width", "height"
        ), false, 0));

        Map<String, Object> interaction = clickDetailed(elementId, button, doubled);
        response.put("interaction", interaction);
        response.put("success", asBoolean(interaction.get("success"), false));
        if (!response.containsKey("reason")) {
            response.put("reason", interaction.getOrDefault("reason", "click_result"));
        }
        return response;
    }

    public Map<String, Object> setTextByQueryDetailed(Map<String, Object> filters, int index, String text, boolean submit, boolean typeCharacters, boolean clearFirst) {
        DomSnapshot snapshot = onMain(snapshotBuilder::refreshSnapshotForQuery);
        if (snapshot == null) return Map.of("success", false, "reason", "no_snapshot");

        List<Map<String, Object>> matches = queryEngine.findMatchingElements(snapshot, filters);
        int selectedIndex = Math.max(0, index);

        Map<String, Object> response = baseQueryResponse(snapshot, matches, selectedIndex);
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
            "id", "label", "role", "componentId", "actions", "x", "y", "width", "height", "text"
        ), false, 0));

        Map<String, Object> interaction = setTextDetailed(elementId, text == null ? "" : text, submit, typeCharacters, clearFirst);
        response.put("interaction", interaction);
        response.put("success", asBoolean(interaction.get("success"), false));
        if (!response.containsKey("reason")) {
            response.put("reason", interaction.getOrDefault("reason", "set_text_result"));
        }
        return response;
    }

    // ---------- ID-driven public entry points (3-phase: dispatch → settle → finalize) ----------

    public Map<String, Object> clickDetailed(String id, int button, boolean doubled) {
        String screenBefore = captureScreenClassBefore();
        Map<String, Object> result = onMain(() -> clickInternal(id, button, doubled));
        settleOffMain();
        onMain(() -> { finalizePostSettle(result, screenBefore); return null; });
        return result;
    }

    public Map<String, Object> setTextDetailed(String id, String text, boolean submit, boolean typeCharacters, boolean clearFirst) {
        String screenBefore = captureScreenClassBefore();
        Map<String, Object> result = onMain(() -> setTextInternal(id, text, submit, typeCharacters, clearFirst));
        settleOffMain();
        onMain(() -> { finalizePostSettle(result, screenBefore); return null; });
        return result;
    }

    public Map<String, Object> typeTextDetailed(String id, String text, boolean clearFirst, boolean submit) {
        return setTextDetailed(id, text, submit, true, clearFirst);
    }

    public Map<String, Object> setValueDetailed(String id, Object value) {
        String screenBefore = captureScreenClassBefore();
        Map<String, Object> result = onMain(() -> setValueInternal(id, value));
        settleOffMain();
        onMain(() -> { finalizePostSettle(result, screenBefore); return null; });
        return result;
    }

    public Map<String, Object> pressKeyDetailed(String keyName, int modifiers, int repeat, boolean release) {
        String screenBefore = captureScreenClassBefore();
        Map<String, Object> result = onMain(() -> pressKeyInternal(keyName, modifiers, repeat, release));
        settleOffMain();
        onMain(() -> { finalizePostSettle(result, screenBefore); return null; });
        return result;
    }

    public Map<String, Object> scrollDetailed(String id, double verticalAmount, double horizontalAmount) {
        String screenBefore = captureScreenClassBefore();
        Map<String, Object> result = onMain(() -> scrollInternal(id, verticalAmount, horizontalAmount));
        settleOffMain();
        onMain(() -> { finalizePostSettle(result, screenBefore); return null; });
        return result;
    }

    public Map<String, Object> dragDetailed(String id, double offsetX, double offsetY, int steps, int button) {
        String screenBefore = captureScreenClassBefore();
        Map<String, Object> result = onMain(() -> dragInternal(id, offsetX, offsetY, steps, button));
        settleOffMain();
        onMain(() -> { finalizePostSettle(result, screenBefore); return null; });
        return result;
    }

    public boolean navigateBack() {
        return Boolean.TRUE.equals(onMain(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen == null) return false;
            screen.onClose();
            return true;
        }));
    }

    // ---------- Internal main-thread implementations ----------

    private Map<String, Object> clickInternal(String id, int button, boolean doubled) {
        Map<String, Object> result = interaction("click", id);
        result.put("button", button);
        result.put("doubleClick", doubled);

        ElementRef ref = snapshotBuilder.resolveRef(id);
        Screen screen = Minecraft.getInstance().screen;
        if (ref == null || screen == null) {
            result.put("reason", ref == null ? "element_not_found" : "no_active_screen");
            return result;
        }

        refreshCoordinates(ref);
        if (!ref.clickableCoordinates()) {
            result.put("reason", "missing_click_coordinates");
            return result;
        }

        double targetX = ref.centerX();
        double targetY = ref.centerY();
        result.put("targetX", targetX);
        result.put("targetY", targetY);

        @SuppressWarnings({"rawtypes", "unchecked"})
        Optional<WidgetAdapter> adapter = registry.findWidgetAdapter(ref.backing);
        if (adapter.isPresent()) {
            try {
                @SuppressWarnings("unchecked")
                boolean handled = adapter.get().handleClick(screen, ref.backing, targetX, targetY, button, doubled);
                if (handled) {
                    result.put("reason", "adapter_handled");
                    result.put("path", "adapter");
                    result.put("success", true);
                    return result;
                }
            } catch (Throwable ignored) {
            }
        }

        MouseButtonEvent click = new MouseButtonEvent(targetX, targetY, new MouseButtonInfo(button, 0));

        boolean handled = invokeScreenClick(screen, click, doubled);
        result.put("screenHandled", handled);

        if (!handled && ref.backing instanceof GuiEventListener element && button == 0 && entryListHelper.isEntryElement(screen, element)) {
            if (entryListHelper.trySelectEntryAtCoordinates(screen, click, doubled)) {
                result.put("reason", "entry_selected_via_hover");
                result.put("path", "entry_hover");
                result.put("success", true);
                return result;
            }
            if (entryListHelper.trySelectEntryFallback(screen, element, click, doubled)) {
                result.put("reason", "entry_selected_via_owner");
                result.put("path", "entry_owner");
                result.put("success", true);
                return result;
            }
        }

        if (!handled && ref.backing instanceof GuiEventListener element) {
            try {
                handled = element.mouseClicked(click, doubled);
                if (handled) element.mouseReleased(click);
            } catch (Throwable ignored) {
            }
        }

        result.put("reason", handled ? "handled" : "not_handled");
        result.put("path", handled ? "screen" : "none");
        result.put("success", handled);
        return result;
    }

    private Map<String, Object> setTextInternal(String id, String text, boolean submit, boolean typeCharacters, boolean clearFirst) {
        Map<String, Object> result = interaction("set_text", id);
        result.put("submit", submit);
        result.put("typeCharacters", typeCharacters);
        result.put("clearFirst", clearFirst);

        ElementRef ref = snapshotBuilder.resolveRef(id);
        Screen screen = Minecraft.getInstance().screen;
        if (ref == null) {
            result.put("reason", "element_not_found");
            result.put("path", "none");
            result.put("success", false);
            return result;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        Optional<WidgetAdapter> adapter = registry.findWidgetAdapter(ref.backing);
        if (adapter.isPresent() && !typeCharacters) {
            try {
                @SuppressWarnings("unchecked")
                boolean handled = adapter.get().handleSetText(screen, ref.backing, text);
                if (handled) {
                    result.put("reason", "adapter_handled");
                    result.put("path", "adapter");
                    result.put("success", true);
                    return result;
                }
            } catch (Throwable ignored) {
            }
        }

        if (ref.backing instanceof EditBox textField) {
            if (typeCharacters && screen != null) {
                refreshCoordinates(ref);
                if (ref.clickableCoordinates()) {
                    invokeScreenClick(screen, new MouseButtonEvent(ref.centerX(), ref.centerY(), new MouseButtonInfo(0, 0)), false);
                } else {
                    screen.setFocused(textField);
                    textField.setFocused(true);
                }

                if (clearFirst) textField.setValue("");

                boolean typed = typeChars(screen, text);
                if (!typed) {
                    String current = clearFirst ? "" : textField.getValue();
                    textField.setValue(current + text);
                }

                if (submit) pressKeyOnScreen(screen, GLFW.GLFW_KEY_ENTER, 0, 1, true);

                result.put("reason", typed ? "typed_chars" : "typed_fallback_set_text");
                result.put("path", "typed");
                result.put("success", true);
                return result;
            }

            // Direct setValue may be intercepted by a TextBoxComponent filter (e.g. numeric-only fields).
            // We capture before/after and report whether the assignment actually stuck so callers see
            // filter rejections instead of false-success.
            String previous = textField.getValue();
            textField.setValue(text);
            String actual = textField.getValue();
            boolean stuck = actual.equals(text);

            if (submit && screen != null) pressKeyOnScreen(screen, GLFW.GLFW_KEY_ENTER, 0, 1, true);

            result.put("requestedText", text);
            result.put("appliedText", actual);
            result.put("textBefore", previous);
            result.put("filterAccepted", stuck);
            result.put("reason", stuck ? "set_text_direct" : "filter_rejected_input");
            result.put("path", "direct");
            result.put("success", stuck);
            return result;
        }

        if (typeCharacters && screen != null) {
            refreshCoordinates(ref);
            if (ref.clickableCoordinates()) {
                invokeScreenClick(screen, new MouseButtonEvent(ref.centerX(), ref.centerY(), new MouseButtonInfo(0, 0)), false);
            }
            boolean typed = typeChars(screen, text);
            if (submit) pressKeyOnScreen(screen, GLFW.GLFW_KEY_ENTER, 0, 1, true);
            result.put("reason", typed ? "typed_chars" : "char_typed_not_handled");
            result.put("path", "typed");
            result.put("success", typed);
            return result;
        }

        result.put("reason", "element_does_not_accept_text");
        result.put("path", "none");
        result.put("success", false);
        return result;
    }

    private Map<String, Object> setValueInternal(String id, Object value) {
        Map<String, Object> result = interaction("set_value", id);
        Screen screen = Minecraft.getInstance().screen;

        ElementRef ref = snapshotBuilder.resolveRef(id);
        if (ref == null) {
            result.put("reason", "element_not_found");
            result.put("path", "none");
            result.put("success", false);
            return result;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        Optional<WidgetAdapter> adapter = registry.findWidgetAdapter(ref.backing);
        if (adapter.isPresent()) {
            try {
                @SuppressWarnings("unchecked")
                boolean handled = adapter.get().handleSetValue(screen, ref.backing, value);
                if (handled) {
                    result.put("appliedValue", value);
                    result.put("reason", "adapter_handled");
                    result.put("path", "adapter");
                    result.put("success", true);
                    return result;
                }
            } catch (Throwable t) {
                result.put("adapterError", t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }

        result.put("reason", "element_does_not_support_set_value");
        result.put("path", "none");
        result.put("success", false);
        return result;
    }

    private Map<String, Object> pressKeyInternal(String keyName, int modifiers, int repeat, boolean release) {
        Map<String, Object> result = interaction("press_key", null);
        result.put("key", keyName);
        result.put("modifiers", modifiers);
        result.put("repeat", repeat);
        result.put("release", release);

        Integer keyCode = DomKeyCodec.parseKeyCode(keyName);
        if (keyCode == null) {
            result.put("reason", "unknown_key");
            result.put("path", "none");
            result.put("success", false);
            return result;
        }

        int clampedRepeat = Math.max(1, Math.min(32, repeat));
        Screen screen = Minecraft.getInstance().screen;

        boolean handled = dispatchKeyboardKey(keyCode, modifiers, clampedRepeat, release, result);
        result.put("keyCode", keyCode);
        result.put("reason", handled ? "key_handled" : "key_not_handled");
        result.put("path", screen == null ? "global" : "screen");
        result.put("success", handled);
        return result;
    }

    private Map<String, Object> scrollInternal(String id, double verticalAmount, double horizontalAmount) {
        Map<String, Object> result = interaction("scroll", id);
        result.put("verticalAmount", verticalAmount);
        result.put("horizontalAmount", horizontalAmount);

        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            result.put("reason", "no_active_screen");
            result.put("path", "none");
            result.put("success", false);
            return result;
        }

        ElementRef ref = id == null || id.isBlank() ? null : snapshotBuilder.resolveRef(id);
        if (id != null && !id.isBlank() && ref == null) {
            result.put("reason", "element_not_found");
            result.put("path", "none");
            result.put("success", false);
            return result;
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
        result.put("path", "screen");
        result.put("success", handled);
        return result;
    }

    private Map<String, Object> dragInternal(String id, double offsetX, double offsetY, int steps, int button) {
        Map<String, Object> result = interaction("drag", id);
        result.put("offsetX", offsetX);
        result.put("offsetY", offsetY);
        result.put("steps", steps);
        result.put("button", button);

        ElementRef ref = snapshotBuilder.resolveRef(id);
        Screen screen = Minecraft.getInstance().screen;
        if (ref == null || screen == null) {
            result.put("reason", ref == null ? "element_not_found" : "no_active_screen");
            result.put("path", "none");
            result.put("success", false);
            return result;
        }

        refreshCoordinates(ref);
        if (!ref.clickableCoordinates()) {
            result.put("reason", "missing_click_coordinates");
            result.put("path", "none");
            result.put("success", false);
            return result;
        }

        int clampedSteps = Math.max(1, Math.min(64, steps));
        double startX = ref.centerX();
        double startY = ref.centerY();

        try {
            screen.mouseMoved(startX, startY);
        } catch (Exception ignored) {
        }

        MouseButtonEvent press = new MouseButtonEvent(startX, startY, new MouseButtonInfo(button, 0));
        boolean pressed;
        try {
            pressed = screen.mouseClicked(press, false);
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
            MouseButtonEvent dragEvent = new MouseButtonEvent(nextX, nextY, new MouseButtonInfo(button, 0));
            try {
                dragged = screen.mouseDragged(dragEvent, dx, dy) || dragged;
            } catch (Exception ignored) {
            }
            previousX = nextX;
            previousY = nextY;
        }

        boolean released;
        try {
            released = screen.mouseReleased(new MouseButtonEvent(previousX, previousY, new MouseButtonInfo(button, 0)));
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
        result.put("path", "screen");
        result.put("success", success);
        return result;
    }

    // ---------- Phase coordination helpers ----------

    private String captureScreenClassBefore() {
        return onMain(() -> {
            Screen s = Minecraft.getInstance().screen;
            return s == null ? null : s.getClass().getName();
        });
    }

    /** Read post-settle screen state on the main thread and merge into the result. */
    private void finalizePostSettle(Map<String, Object> result, String screenBeforeClass) {
        Screen after = Minecraft.getInstance().screen;
        String afterClass = after == null ? null : after.getClass().getName();
        boolean changed = !java.util.Objects.equals(screenBeforeClass, afterClass);

        result.put("screenBeforeClass", screenBeforeClass);
        result.put("screenAfterClass", afterClass);
        result.put("screenAfterType", after == null ? null : after.getClass().getSimpleName());
        result.put("hasScreenAfter", after != null);
        result.put("screenChanged", changed);
    }

    private void settleOffMain() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.isSameThread()) {
            // Render thread can't sleep; the immediate post-state is the best we have.
            return;
        }
        try {
            Thread.sleep(SETTLE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private <T> T onMain(Callable<T> fn) {
        return MainThreadInvoker.call(() -> {
            try {
                return fn.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, MAIN_THREAD_TIMEOUT);
    }

    // ---------- Misc helpers ----------

    private Map<String, Object> baseQueryResponse(DomSnapshot snapshot, List<Map<String, Object>> matches, int selectedIndex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("snapshotId", snapshot.id);
        response.put("screenSignature", snapshot.screenSignature);
        response.put("matchCount", matches.size());
        response.put("selectedIndex", selectedIndex);
        return response;
    }

    private Map<String, Object> interaction(String action, String elementId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", action);
        result.put("elementId", elementId);
        result.put("success", false);
        return result;
    }

    private void refreshCoordinates(ElementRef ref) {
        try {
            if (ref.backing instanceof LayoutElement layoutElement) {
                ref.x = layoutElement.getX();
                ref.y = layoutElement.getY();
                ref.width = layoutElement.getWidth();
                ref.height = layoutElement.getHeight();
            } else if (ref.backing instanceof UIComponent component) {
                ref.x = component.x();
                ref.y = component.y();
                ref.width = component.width();
                ref.height = component.height();
            }
        } catch (Throwable ignored) {
        }
    }

    private boolean typeChars(Screen screen, String text) {
        boolean handled = false;
        for (int codepoint : text.codePoints().toArray()) {
            try {
                handled = screen.charTyped(new CharacterEvent(codepoint)) || handled;
            } catch (Exception ignored) {
            }
        }
        return handled;
    }

    private boolean pressKeyOnScreen(Screen screen, int keyCode, int modifiers, int repeat, boolean release) {
        boolean handled = false;
        KeyEvent input = new KeyEvent(keyCode, 0, modifiers);

        for (int i = 0; i < repeat; i++) {
            try {
                handled = screen.keyPressed(input) || handled;
            } catch (Exception ignored) {
            }
        }
        if (release) {
            try {
                handled = screen.keyReleased(input) || handled;
            } catch (Exception ignored) {
            }
        }
        return handled;
    }

    private boolean dispatchKeyboardKey(int keyCode, int modifiers, int repeat, boolean release, Map<String, Object> result) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.keyboardHandler == null || mc.getWindow() == null) {
            result.put("dispatchError", "keyboard_unavailable");
            return false;
        }

        if (!(mc.keyboardHandler instanceof KeyboardInvoker invoker)) {
            result.put("dispatchError", "keyboard_invoker_unavailable");
            return false;
        }

        long windowHandle = mc.getWindow().handle();
        int scancode = GLFW.glfwGetKeyScancode(keyCode);
        if (scancode < 0) scancode = 0;

        KeyEvent input = new KeyEvent(keyCode, scancode, modifiers);
        result.put("scancode", scancode);
        result.put("matchedBindings", countMatchingBindings(input));
        result.put("dispatchMode", "keyboard_keyPress");

        int dispatchedEvents = 0;
        try {
            invoker.universalHarness$invokeKeyPress(windowHandle, GLFW.GLFW_PRESS, input);
            dispatchedEvents++;
            for (int i = 1; i < repeat; i++) {
                invoker.universalHarness$invokeKeyPress(windowHandle, GLFW.GLFW_REPEAT, input);
                dispatchedEvents++;
            }
            if (release) {
                invoker.universalHarness$invokeKeyPress(windowHandle, GLFW.GLFW_RELEASE, input);
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

    private int countMatchingBindings(KeyEvent input) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null || mc.options.keyMappings == null) return 0;
        int count = 0;
        for (KeyMapping binding : mc.options.keyMappings) {
            if (binding != null && binding.matches(input)) count++;
        }
        return count;
    }

    private boolean invokeScreenClick(Screen screen, MouseButtonEvent click, boolean doubled) {
        try {
            screen.mouseMoved(click.x(), click.y());
        } catch (Exception ignored) {
        }
        try {
            boolean pressed = screen.mouseClicked(click, doubled);
            boolean released = screen.mouseReleased(click);
            return pressed || released;
        } catch (Exception ignored) {
            return false;
        }
    }
}

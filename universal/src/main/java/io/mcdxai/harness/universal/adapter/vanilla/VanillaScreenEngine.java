package io.mcdxai.harness.universal.adapter.vanilla;

import io.mcdxai.harness.universal.adapter.AdapterRegistry;
import io.mcdxai.harness.universal.adapter.DomBuildContext;
import io.mcdxai.harness.universal.adapter.ScreenEngine;
import io.mcdxai.harness.universal.adapter.WidgetAdapter;
import io.mcdxai.harness.universal.dom.ElementRef;
import io.mcdxai.harness.universal.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Generic engine for any vanilla Minecraft Screen. Walks screen.children() recursively. */
public final class VanillaScreenEngine implements ScreenEngine {

    @Override
    public String engineName() {
        return "vanilla";
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean canHandle(Screen screen) {
        return screen != null;
    }

    @Override
    public List<Map<String, Object>> buildDom(Screen screen, DomBuildContext ctx) {
        List<Map<String, Object>> elements = new ArrayList<>();
        for (GuiEventListener child : screen.children()) {
            if (isOwoAdapter(child)) continue; // HybridScreenEngine walks owo adapters separately.
            elements.add(mapElement(child, null, ctx));
        }
        // Container screens render slots manually outside the children() tree.
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            enumerateSlots(containerScreen, elements, ctx);
        }
        return elements;
    }

    private void enumerateSlots(AbstractContainerScreen<?> screen, List<Map<String, Object>> sink, DomBuildContext ctx) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int leftPos = accessor.universalHarness$getLeftPos();
        int topPos = accessor.universalHarness$getTopPos();

        var menu = screen.getMenu();
        if (menu == null || menu.slots == null) return;

        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot == null) continue;

            String id = ctx.nextId("v");
            ElementRef ref = new ElementRef(id, slot, this);
            ref.x = leftPos + slot.x;
            ref.y = topPos + slot.y;
            ref.width = 16;
            ref.height = 16;
            ref.role = "container_slot";
            ctx.storeRef(id, ref);

            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("id", id);
            mapped.put("engine", engineName());
            mapped.put("class", Slot.class.getName());
            mapped.put("type", "Slot");
            mapped.put("role", "container_slot");
            mapped.put("slotIndex", i);
            mapped.put("containerIndex", slot.index);
            mapped.put("x", ref.x);
            mapped.put("y", ref.y);
            mapped.put("width", 16);
            mapped.put("height", 16);
            mapped.put("hasItem", slot.hasItem());
            ItemStack stack = slot.getItem();
            if (stack != null && !stack.isEmpty()) {
                var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                mapped.put("itemId", itemId == null ? "unknown" : itemId.toString());
                mapped.put("itemName", stack.getHoverName().getString());
                mapped.put("itemCount", stack.getCount());
            }
            mapped.put("actions", List.of("click"));
            mapped.put("clickable", true);
            mapped.put("actionable", true);
            sink.add(mapped);
        }
    }

    /** Detect OwoUIAdapter without taking a hard compile-time owo dep on this engine. */
    private static boolean isOwoAdapter(Object element) {
        if (element == null) return false;
        Class<?> type = element.getClass();
        while (type != null && type != Object.class) {
            if ("io.wispforest.owo.ui.core.OwoUIAdapter".equals(type.getName())) return true;
            type = type.getSuperclass();
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> mapElement(GuiEventListener element, AbstractSelectionList<?> owningList, DomBuildContext ctx) {
        String id = ctx.nextId("v");
        ElementRef ref = new ElementRef(id, element, this);
        ctx.storeRef(id, ref);

        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("id", id);
        mapped.put("engine", engineName());
        Class<?> clazz = element.getClass();
        mapped.put("class", clazz.getName());
        mapped.put("type", clazz.getSimpleName());

        if (element instanceof AbstractWidget widget) {
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

        if (element instanceof LayoutElement layoutElement) {
            mapped.putIfAbsent("x", layoutElement.getX());
            mapped.putIfAbsent("y", layoutElement.getY());
            mapped.putIfAbsent("width", layoutElement.getWidth());
            mapped.putIfAbsent("height", layoutElement.getHeight());

            if (Double.isNaN(ref.x)) {
                ref.x = layoutElement.getX();
                ref.y = layoutElement.getY();
                ref.width = layoutElement.getWidth();
                ref.height = layoutElement.getHeight();
            }
        }

        if (owningList != null && element instanceof LayoutElement && !(element instanceof AbstractWidget)) {
            mapped.put("selected", isSelectedInList(owningList, element));
        }
        if (owningList != null && owningList.children().contains(element)) {
            mapped.putIfAbsent("role", "list_entry");
        }

        if (element instanceof EditBox textField) {
            mapped.put("text", textField.getValue());
            mapped.put("editable", textField.isActive());
        }

        // Let registered adapters add type-specific metadata.
        Optional<WidgetAdapter> adapter = ctx.registry().findWidgetAdapter(element);
        adapter.ifPresent(a -> {
            try {
                a.extractMetadata(element, mapped);
            } catch (Throwable ignored) {
            }
        });

        // Apply name-based decorators for mod-specific custom widgets.
        try {
            ctx.registry().applyNameDecorators(element, mapped);
        } catch (Throwable ignored) {
        }

        // Compute action hints.
        List<String> actions = new ArrayList<>();
        if (canClick(element, owningList, ref)) actions.add("click");
        if (element instanceof EditBox) {
            actions.add("set_text");
            actions.add("type_text");
        }
        if (element instanceof AbstractScrollArea || element instanceof AbstractSelectionList<?>) {
            actions.add("scroll");
        }
        if (element instanceof AbstractSliderButton) {
            actions.add("drag");
            actions.add("set_value");
        }
        adapter.ifPresent(a -> {
            try {
                List<String> extra = a.supportedActions(element);
                if (extra != null) for (String x : extra) if (!actions.contains(x)) actions.add(x);
            } catch (Throwable ignored) {
            }
        });
        mapped.put("actions", actions);
        mapped.put("clickable", actions.contains("click"));
        mapped.put("actionable", !actions.isEmpty());

        AbstractSelectionList<?> nextList = element instanceof AbstractSelectionList<?> selList ? selList : owningList;
        if (element instanceof ContainerEventHandler parentElement) {
            List<Map<String, Object>> children = new ArrayList<>();
            for (GuiEventListener child : parentElement.children()) {
                if (child == element) continue;
                if (isOwoAdapter(child)) continue;
                children.add(mapElement(child, nextList, ctx));
            }
            mapped.put("children", children);
        }

        return mapped;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean isSelectedInList(AbstractSelectionList<?> list, GuiEventListener element) {
        AbstractSelectionList raw = (AbstractSelectionList) list;
        try {
            return raw.getSelected() == element;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean canClick(GuiEventListener element, AbstractSelectionList<?> owningList, ElementRef ref) {
        if (!ref.clickableCoordinates()) return false;
        if (element instanceof AbstractWidget widget) return widget.visible && widget.active;
        return owningList != null && owningList.children().contains(element);
    }
}

package io.mcdxai.harness.universal.adapter.owo;

import io.mcdxai.harness.universal.adapter.DomBuildContext;
import io.mcdxai.harness.universal.adapter.ScreenEngine;
import io.mcdxai.harness.universal.adapter.WidgetAdapter;
import io.mcdxai.harness.universal.dom.ElementRef;
import io.mcdxai.harness.universal.mixin.owo.BaseOwoScreenAccessor;
import io.wispforest.owo.ui.base.BaseOwoScreen;  // used by canHandle's instanceof
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.ParentUIComponent;
import io.wispforest.owo.ui.core.UIComponent;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Engine that walks the owo UIComponent tree exposed via BaseOwoScreen.uiAdapter.rootComponent. */
public final class OwoScreenEngine implements ScreenEngine {

    @Override
    public String engineName() {
        return "owo";
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean canHandle(Screen screen) {
        return screen instanceof BaseOwoScreen<?>;
    }

    @Override
    public List<Map<String, Object>> buildDom(Screen screen, DomBuildContext ctx) {
        // Cast guarded by canHandle() — only fires when owo is loaded and BaseOwoScreen exists.
        BaseOwoScreenAccessor accessor = (BaseOwoScreenAccessor) screen;
        OwoUIAdapter<?> adapter = accessor.universalHarness$getUiAdapter();
        if (adapter == null) return List.of();

        ParentUIComponent root = adapter.rootComponent;
        if (root == null) return List.of();

        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(walkComponent(root, ctx));
        return elements;
    }

    /** Walk an arbitrary UIComponent and produce its DOM mapping (with children if a parent). */
    public Map<String, Object> walkComponent(UIComponent component, DomBuildContext ctx) {
        String id = ctx.nextId("o");
        ElementRef ref = new ElementRef(id, component, this);
        ctx.storeRef(id, ref);

        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("id", id);
        mapped.put("engine", engineName());

        Class<?> clazz = component.getClass();
        mapped.put("class", clazz.getName());
        mapped.put("type", clazz.getSimpleName());

        try {
            String userId = component.id();
            if (userId != null && !userId.isBlank()) mapped.put("componentId", userId);
        } catch (Throwable ignored) {
        }

        try {
            mapped.put("x", component.x());
            mapped.put("y", component.y());
            mapped.put("width", component.width());
            mapped.put("height", component.height());

            ref.x = component.x();
            ref.y = component.y();
            ref.width = component.width();
            ref.height = component.height();
        } catch (Throwable ignored) {
        }

        // Adapter-driven metadata + actions.
        @SuppressWarnings("rawtypes")
        Optional<WidgetAdapter> adapter = ctx.registry().findWidgetAdapter(component);
        @SuppressWarnings("unchecked")
        List<String> actions = new ArrayList<>();
        adapter.ifPresent(a -> {
            try {
                a.extractMetadata(component, mapped);
            } catch (Throwable ignored) {
            }
            try {
                List<String> extra = a.supportedActions(component);
                if (extra != null) for (String x : extra) if (!actions.contains(x)) actions.add(x);
            } catch (Throwable ignored) {
            }
        });

        // Apply name-based decorators (e.g. item-editor's custom components).
        try {
            ctx.registry().applyNameDecorators(component, mapped);
        } catch (Throwable ignored) {
        }

        // Generic click hint for any sized component (owo dispatches by coords).
        boolean hasCoords = ref.clickableCoordinates();
        if (hasCoords && !actions.contains("click")) actions.add("click");

        if (component instanceof ParentUIComponent) {
            if (!actions.contains("scroll") && component instanceof ScrollContainer) actions.add("scroll");
        }

        mapped.put("actions", actions);
        mapped.put("clickable", hasCoords);
        mapped.put("actionable", !actions.isEmpty());

        // Children — but unwrap ScrollContainer to keep the tree flatter.
        if (component instanceof ScrollContainer<?> scroll) {
            UIComponent inner = scroll.child();
            if (inner != null) {
                List<Map<String, Object>> children = new ArrayList<>();
                children.add(walkComponent(inner, ctx));
                mapped.put("children", children);
            }
        } else if (component instanceof ParentUIComponent parent) {
            try {
                List<UIComponent> kids = parent.children();
                if (kids != null && !kids.isEmpty()) {
                    boolean isStack = parent instanceof StackLayout;
                    List<Map<String, Object>> children = new ArrayList<>();
                    for (int i = 0; i < kids.size(); i++) {
                        UIComponent child = kids.get(i);
                        Map<String, Object> childMap = walkComponent(child, ctx);
                        if (isStack && i < kids.size() - 1 && kids.size() > 1) {
                            childMap.put("occludedByOverlay", true);
                        }
                        if (child instanceof OverlayContainer<?>) {
                            childMap.put("role", "overlay");
                        }
                        children.add(childMap);
                    }
                    mapped.put("children", children);
                }
            } catch (Throwable ignored) {
            }
        }

        return mapped;
    }
}

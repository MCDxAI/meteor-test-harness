package io.mcdxai.harness.universal.adapter.hybrid;

import io.mcdxai.harness.universal.adapter.DomBuildContext;
import io.mcdxai.harness.universal.adapter.ScreenEngine;
import io.mcdxai.harness.universal.adapter.owo.OwoScreenEngine;
import io.mcdxai.harness.universal.adapter.vanilla.VanillaScreenEngine;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.Map;

/**
 * Detects vanilla screens that embed an OwoUIAdapter (e.g. item-editor's StorageScreen
 * which extends ContainerScreen but adds an owo side panel via addRenderableWidget(adapter)).
 * Runs the vanilla engine for the standard widget tree and the owo engine for any embedded
 * adapter's rootComponent. Element IDs stay distinct via the per-engine prefixes.
 */
public final class HybridScreenEngine implements ScreenEngine {
    private final VanillaScreenEngine vanilla;
    private final OwoScreenEngine owo;

    public HybridScreenEngine(VanillaScreenEngine vanilla, OwoScreenEngine owo) {
        this.vanilla = vanilla;
        this.owo = owo;
    }

    @Override
    public String engineName() {
        return "hybrid";
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public boolean canHandle(Screen screen) {
        if (screen == null || screen instanceof BaseOwoScreen<?>) return false;
        return findEmbeddedAdapter(screen) != null;
    }

    @Override
    public List<Map<String, Object>> buildDom(Screen screen, DomBuildContext ctx) {
        // Vanilla side first — covers slots and any standard widgets.
        List<Map<String, Object>> elements = new java.util.ArrayList<>(vanilla.buildDom(screen, ctx));

        // Then walk every embedded owo adapter's rootComponent.
        for (GuiEventListener child : screen.children()) {
            collectOwoRoots(child, ctx, elements);
        }

        return elements;
    }

    private void collectOwoRoots(GuiEventListener element, DomBuildContext ctx, List<Map<String, Object>> sink) {
        if (element instanceof OwoUIAdapter<?> adapter && adapter.rootComponent != null) {
            sink.add(owo.walkComponent(adapter.rootComponent, ctx));
            return;
        }
        if (element instanceof ContainerEventHandler parent) {
            for (GuiEventListener nested : parent.children()) {
                if (nested == element) continue;
                collectOwoRoots(nested, ctx, sink);
            }
        }
    }

    private OwoUIAdapter<?> findEmbeddedAdapter(Screen screen) {
        try {
            for (GuiEventListener child : screen.children()) {
                OwoUIAdapter<?> found = findEmbeddedAdapter(child);
                if (found != null) return found;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private OwoUIAdapter<?> findEmbeddedAdapter(GuiEventListener element) {
        if (element instanceof OwoUIAdapter<?> adapter) return adapter;
        if (element instanceof ContainerEventHandler parent) {
            for (GuiEventListener nested : parent.children()) {
                if (nested == element) continue;
                OwoUIAdapter<?> found = findEmbeddedAdapter(nested);
                if (found != null) return found;
            }
        }
        return null;
    }
}

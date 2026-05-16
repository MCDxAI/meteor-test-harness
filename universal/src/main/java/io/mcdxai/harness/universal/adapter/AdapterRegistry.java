package io.mcdxai.harness.universal.adapter;

import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AdapterRegistry {
    private final List<ScreenEngine> screenEngines = new ArrayList<>();
    /** Insertion-ordered so subclass-first registration is honored when class-hierarchy walking matches a base. */
    private final Map<Class<?>, WidgetAdapter<?>> widgetAdapters = new LinkedHashMap<>();
    private final List<ScreenDescriptor> screenDescriptors = new ArrayList<>();
    private final Map<String, java.util.function.BiConsumer<Object, Map<String, Object>>> nameDecorators = new LinkedHashMap<>();

    public synchronized void registerScreenEngine(ScreenEngine engine) {
        screenEngines.add(engine);
        screenEngines.sort(Comparator.comparingInt(ScreenEngine::priority).reversed());
    }

    public synchronized <W> void registerWidgetAdapter(WidgetAdapter<W> adapter) {
        widgetAdapters.put(adapter.widgetType(), adapter);
    }

    public synchronized void registerScreenDescriptor(ScreenDescriptor descriptor) {
        screenDescriptors.add(descriptor);
    }

    public synchronized List<ScreenEngine> screenEngines() {
        return List.copyOf(screenEngines);
    }

    public synchronized Optional<ScreenEngine> findScreenEngine(Screen screen) {
        if (screen == null) return Optional.empty();
        for (ScreenEngine engine : screenEngines) {
            try {
                if (engine.canHandle(screen)) return Optional.of(engine);
            } catch (Throwable ignored) {
                // Defensive — a broken engine shouldn't block the rest.
            }
        }
        return Optional.empty();
    }

    /** Find the most-specific registered adapter for the given widget instance. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized Optional<WidgetAdapter> findWidgetAdapter(Object widget) {
        if (widget == null) return Optional.empty();
        Class<?> type = widget.getClass();
        while (type != null && type != Object.class) {
            WidgetAdapter<?> adapter = widgetAdapters.get(type);
            if (adapter != null) return Optional.of((WidgetAdapter) adapter);
            type = type.getSuperclass();
        }
        for (Map.Entry<Class<?>, WidgetAdapter<?>> entry : widgetAdapters.entrySet()) {
            if (entry.getKey().isInstance(widget)) {
                return Optional.of((WidgetAdapter) entry.getValue());
            }
        }
        return Optional.empty();
    }

    /**
     * Register a metadata decorator keyed by simple class name. Useful for tagging custom mod components
     * without taking a hard compile-time dependency on them. Decorator runs AFTER the main WidgetAdapter.
     */
    public synchronized void registerNameDecorator(String simpleClassName, java.util.function.BiConsumer<Object, Map<String, Object>> decorator) {
        nameDecorators.put(simpleClassName, decorator);
    }

    /** Apply any name-keyed decorators for the given widget. Walks superclass simple names too. */
    public synchronized void applyNameDecorators(Object widget, Map<String, Object> target) {
        if (widget == null) return;
        Class<?> type = widget.getClass();
        while (type != null && type != Object.class) {
            var decorator = nameDecorators.get(type.getSimpleName());
            if (decorator != null) {
                try {
                    decorator.accept(widget, target);
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
    }

    public synchronized List<ScreenDescriptor> findScreenDescriptors(Screen screen) {
        if (screen == null) return Collections.emptyList();
        List<ScreenDescriptor> matches = new ArrayList<>();
        for (ScreenDescriptor descriptor : screenDescriptors) {
            try {
                if (descriptor.matches(screen)) matches.add(descriptor);
            } catch (Throwable ignored) {
            }
        }
        return matches;
    }
}

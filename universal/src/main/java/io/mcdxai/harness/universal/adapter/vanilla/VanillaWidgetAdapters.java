package io.mcdxai.harness.universal.adapter.vanilla;

import io.mcdxai.harness.universal.adapter.AdapterRegistry;
import io.mcdxai.harness.universal.adapter.WidgetAdapter;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.util.List;
import java.util.Map;

/** Registers metadata-only adapters for common vanilla widgets. */
public final class VanillaWidgetAdapters {
    private VanillaWidgetAdapters() {
    }

    public static void registerAll(AdapterRegistry registry) {
        registry.registerWidgetAdapter(new CheckboxAdapter());
        registry.registerWidgetAdapter(new SliderAdapter());
        registry.registerWidgetAdapter(new CycleButtonAdapter());
        registry.registerWidgetAdapter(new EditBoxAdapter());
    }

    static final class CheckboxAdapter implements WidgetAdapter<Checkbox> {
        @Override
        public Class<Checkbox> widgetType() {
            return Checkbox.class;
        }

        @Override
        public void extractMetadata(Checkbox widget, Map<String, Object> target) {
            target.put("checked", widget.selected());
            target.put("role", "checkbox");
            try {
                var message = widget.getMessage();
                if (message != null) {
                    String label = message.getString();
                    if (label != null && !label.isEmpty()) target.put("label", label);
                }
            } catch (Throwable ignored) {
            }
        }

        @Override
        public List<String> supportedActions(Checkbox widget) {
            return List.of("set_value", "click");
        }

        @Override
        public boolean handleSetValue(Screen screen, Checkbox widget, Object value) {
            boolean desired = io.mcdxai.harness.universal.dom.DomValueUtils.toBoolean(value);
            if (widget.selected() == desired) return true;
            // Dispatch a real click at the widget center so onPress fires and any
            // onValueChange listeners run. The mouseClicked() boolean is unreliable
            // (parent dispatchers can consume the press without returning true), so
            // trust the post-state instead.
            clickAtCenter(screen, widget);
            return widget.selected() == desired;
        }

        private static boolean clickAtCenter(Screen screen, Checkbox widget) {
            if (screen == null) return false;
            double cx = widget.getX() + widget.getWidth() / 2D;
            double cy = widget.getY() + widget.getHeight() / 2D;
            MouseButtonEvent click = new MouseButtonEvent(cx, cy, new MouseButtonInfo(0, 0));
            try {
                screen.mouseMoved(cx, cy);
            } catch (Throwable ignored) {
            }
            try {
                boolean pressed = screen.mouseClicked(click, false);
                screen.mouseReleased(click);
                return pressed;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    static final class SliderAdapter implements WidgetAdapter<AbstractSliderButton> {
        @Override
        public Class<AbstractSliderButton> widgetType() {
            return AbstractSliderButton.class;
        }

        @Override
        public void extractMetadata(AbstractSliderButton widget, Map<String, Object> target) {
            target.put("role", "slider");
            try {
                java.lang.reflect.Field valueField = AbstractSliderButton.class.getDeclaredField("value");
                valueField.setAccessible(true);
                target.put("value", valueField.getDouble(widget));
            } catch (Throwable ignored) {
            }
        }
    }

    static final class CycleButtonAdapter implements WidgetAdapter<CycleButton<?>> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public Class<CycleButton<?>> widgetType() {
            return (Class) CycleButton.class;
        }

        @Override
        public void extractMetadata(CycleButton<?> widget, Map<String, Object> target) {
            target.put("role", "cycle_button");
            try {
                target.put("value", String.valueOf(widget.getValue()));
            } catch (Throwable ignored) {
            }
        }
    }

    static final class EditBoxAdapter implements WidgetAdapter<EditBox> {
        @Override
        public Class<EditBox> widgetType() {
            return EditBox.class;
        }

        @Override
        public void extractMetadata(EditBox widget, Map<String, Object> target) {
            target.put("role", "text_input");
        }
    }
}

package io.mcdxai.harness.universal.adapter.owo;

import io.mcdxai.harness.universal.adapter.AdapterRegistry;
import io.mcdxai.harness.universal.adapter.WidgetAdapter;
import io.mcdxai.harness.universal.dom.DomValueUtils;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.SliderComponent;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.StackLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.util.List;
import java.util.Map;

/** Registers metadata adapters for the standard owo-lib component set. */
public final class OwoWidgetAdapters {
    private OwoWidgetAdapters() {
    }

    public static void registerAll(AdapterRegistry registry) {
        registry.registerWidgetAdapter(new ButtonAdapter());
        registry.registerWidgetAdapter(new LabelAdapter());
        registry.registerWidgetAdapter(new TextBoxAdapter());
        registry.registerWidgetAdapter(new TextAreaAdapter());
        registry.registerWidgetAdapter(new CheckboxAdapter());
        registry.registerWidgetAdapter(new SliderAdapter());
        registry.registerWidgetAdapter(new DiscreteSliderAdapter());
        registry.registerWidgetAdapter(new ItemAdapter());
        registry.registerWidgetAdapter(new EntityAdapter());
        registry.registerWidgetAdapter(new DropdownAdapter());
        registry.registerWidgetAdapter(new TextureAdapter());
        registry.registerWidgetAdapter(new BoxAdapter());

        // Containers — metadata only, but expose role for LLM context.
        registry.registerWidgetAdapter(new FlowLayoutAdapter());
        registry.registerWidgetAdapter(new GridLayoutAdapter());
        registry.registerWidgetAdapter(new StackLayoutAdapter());
        registry.registerWidgetAdapter(new ScrollContainerAdapter());
        registry.registerWidgetAdapter(new OverlayContainerAdapter());
        registry.registerWidgetAdapter(new CollapsibleContainerAdapter());
    }

    static final class ButtonAdapter implements WidgetAdapter<ButtonComponent> {
        @Override
        public Class<ButtonComponent> widgetType() {
            return ButtonComponent.class;
        }

        @Override
        public void extractMetadata(ButtonComponent widget, Map<String, Object> target) {
            target.put("role", "button");
            target.put("label", widget.getMessage() == null ? "" : widget.getMessage().getString());
            target.put("active", widget.active());
        }

        @Override
        public List<String> supportedActions(ButtonComponent widget) {
            return widget.active() ? List.of("click") : List.of();
        }
    }

    static final class LabelAdapter implements WidgetAdapter<LabelComponent> {
        @Override
        public Class<LabelComponent> widgetType() {
            return LabelComponent.class;
        }

        @Override
        public void extractMetadata(LabelComponent widget, Map<String, Object> target) {
            target.put("role", "label");
            try {
                target.put("text", widget.text() == null ? "" : widget.text().getString());
            } catch (Throwable ignored) {
            }
        }
    }

    static final class TextBoxAdapter implements WidgetAdapter<TextBoxComponent> {
        @Override
        public Class<TextBoxComponent> widgetType() {
            return TextBoxComponent.class;
        }

        @Override
        public void extractMetadata(TextBoxComponent widget, Map<String, Object> target) {
            target.put("role", "text_input");
            target.put("text", widget.getValue());
            target.put("editable", widget.isActive());
        }

        @Override
        public List<String> supportedActions(TextBoxComponent widget) {
            return List.of("set_text", "type_text");
        }
    }

    static final class TextAreaAdapter implements WidgetAdapter<TextAreaComponent> {
        @Override
        public Class<TextAreaComponent> widgetType() {
            return TextAreaComponent.class;
        }

        @Override
        public void extractMetadata(TextAreaComponent widget, Map<String, Object> target) {
            target.put("role", "text_area");
            try {
                target.put("text", widget.getValue());
            } catch (Throwable ignored) {
            }
        }

        @Override
        public List<String> supportedActions(TextAreaComponent widget) {
            return List.of("set_text", "type_text");
        }
    }

    static final class CheckboxAdapter implements WidgetAdapter<CheckboxComponent> {
        @Override
        public Class<CheckboxComponent> widgetType() {
            return CheckboxComponent.class;
        }

        @Override
        public void extractMetadata(CheckboxComponent widget, Map<String, Object> target) {
            target.put("role", "checkbox");
            target.put("checked", widget.selected());
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
        public List<String> supportedActions(CheckboxComponent widget) {
            return List.of("set_value", "click");
        }

        @Override
        public boolean handleSetValue(Screen screen, CheckboxComponent widget, Object value) {
            boolean desired = DomValueUtils.toBoolean(value);
            if (widget.selected() == desired) return true;
            // Dispatch a real click at the widget center. CheckboxComponent.checked(bool)
            // updates the field but does not fire the onChanged callback, so models bound
            // via onChanged (the common owo pattern) would silently miss the change.
            if (screen == null) return false;
            double cx = widget.getX() + widget.getWidth() / 2D;
            double cy = widget.getY() + widget.getHeight() / 2D;
            MouseButtonEvent click = new MouseButtonEvent(cx, cy, new MouseButtonInfo(0, 0));
            try {
                screen.mouseMoved(cx, cy);
            } catch (Throwable ignored) {
            }
            try {
                screen.mouseClicked(click, false);
                screen.mouseReleased(click);
            } catch (Throwable ignored) {
                return false;
            }
            // screen.mouseClicked() returns false for owo components routed through
            // OwoUIAdapter even when the toggle actually happened; trust the post-state.
            if (widget.selected() == desired) return true;

            // Fallback for tiny checkboxes (width ~16): OwoUIAdapter's hit-testing inside
            // nested StackLayout/ScrollContainer trees sometimes refuses to route the
            // screen-level press. Dispatching directly to the widget bypasses the adapter
            // and triggers onPress + onChanged.
            try {
                widget.mouseClicked(click, false);
                widget.mouseReleased(click);
            } catch (Throwable ignored) {
                return false;
            }
            return widget.selected() == desired;
        }
    }

    static final class SliderAdapter implements WidgetAdapter<SliderComponent> {
        @Override
        public Class<SliderComponent> widgetType() {
            return SliderComponent.class;
        }

        @Override
        public void extractMetadata(SliderComponent widget, Map<String, Object> target) {
            target.put("role", "slider");
            target.put("value", widget.value());
            target.put("active", widget.active());
        }

        @Override
        public List<String> supportedActions(SliderComponent widget) {
            return List.of("set_value", "drag");
        }

        @Override
        public boolean handleSetValue(Screen screen, SliderComponent widget, Object value) {
            widget.value(DomValueUtils.toDouble(value));
            return true;
        }
    }

    static final class DiscreteSliderAdapter implements WidgetAdapter<DiscreteSliderComponent> {
        @Override
        public Class<DiscreteSliderComponent> widgetType() {
            return DiscreteSliderComponent.class;
        }

        @Override
        public void extractMetadata(DiscreteSliderComponent widget, Map<String, Object> target) {
            target.put("role", "discrete_slider");
            target.put("value", widget.value());
        }

        @Override
        public List<String> supportedActions(DiscreteSliderComponent widget) {
            return List.of("set_value", "drag");
        }

        @Override
        public boolean handleSetValue(Screen screen, DiscreteSliderComponent widget, Object value) {
            widget.value(DomValueUtils.toDouble(value));
            return true;
        }
    }

    static final class ItemAdapter implements WidgetAdapter<ItemComponent> {
        @Override
        public Class<ItemComponent> widgetType() {
            return ItemComponent.class;
        }

        @Override
        public void extractMetadata(ItemComponent widget, Map<String, Object> target) {
            target.put("role", "item");
            try {
                var stack = widget.stack();
                if (stack != null && !stack.isEmpty()) {
                    target.put("itemName", stack.getHoverName().getString());
                    target.put("itemCount", stack.getCount());
                }
            } catch (Throwable ignored) {
            }
        }
    }

    static final class EntityAdapter implements WidgetAdapter<EntityComponent<?>> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public Class<EntityComponent<?>> widgetType() {
            return (Class) EntityComponent.class;
        }

        @Override
        public void extractMetadata(EntityComponent<?> widget, Map<String, Object> target) {
            target.put("role", "entity_preview");
        }
    }

    static final class DropdownAdapter implements WidgetAdapter<DropdownComponent> {
        @Override
        public Class<DropdownComponent> widgetType() {
            return DropdownComponent.class;
        }

        @Override
        public void extractMetadata(DropdownComponent widget, Map<String, Object> target) {
            target.put("role", "dropdown");
        }
    }

    static final class TextureAdapter implements WidgetAdapter<TextureComponent> {
        @Override
        public Class<TextureComponent> widgetType() {
            return TextureComponent.class;
        }

        @Override
        public void extractMetadata(TextureComponent widget, Map<String, Object> target) {
            target.put("role", "texture");
        }
    }

    static final class BoxAdapter implements WidgetAdapter<BoxComponent> {
        @Override
        public Class<BoxComponent> widgetType() {
            return BoxComponent.class;
        }

        @Override
        public void extractMetadata(BoxComponent widget, Map<String, Object> target) {
            target.put("role", "box");
        }
    }

    static final class FlowLayoutAdapter implements WidgetAdapter<FlowLayout> {
        @Override
        public Class<FlowLayout> widgetType() {
            return FlowLayout.class;
        }

        @Override
        public void extractMetadata(FlowLayout widget, Map<String, Object> target) {
            target.put("role", "flow_layout");
        }
    }

    static final class GridLayoutAdapter implements WidgetAdapter<GridLayout> {
        @Override
        public Class<GridLayout> widgetType() {
            return GridLayout.class;
        }

        @Override
        public void extractMetadata(GridLayout widget, Map<String, Object> target) {
            target.put("role", "grid_layout");
        }
    }

    static final class StackLayoutAdapter implements WidgetAdapter<StackLayout> {
        @Override
        public Class<StackLayout> widgetType() {
            return StackLayout.class;
        }

        @Override
        public void extractMetadata(StackLayout widget, Map<String, Object> target) {
            target.put("role", "stack_layout");
        }
    }

    static final class ScrollContainerAdapter implements WidgetAdapter<ScrollContainer<?>> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public Class<ScrollContainer<?>> widgetType() {
            return (Class) ScrollContainer.class;
        }

        @Override
        public void extractMetadata(ScrollContainer<?> widget, Map<String, Object> target) {
            target.put("role", "scroll_container");
        }

        @Override
        public List<String> supportedActions(ScrollContainer<?> widget) {
            return List.of("scroll");
        }
    }

    static final class OverlayContainerAdapter implements WidgetAdapter<OverlayContainer<?>> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public Class<OverlayContainer<?>> widgetType() {
            return (Class) OverlayContainer.class;
        }

        @Override
        public void extractMetadata(OverlayContainer<?> widget, Map<String, Object> target) {
            target.put("role", "overlay");
        }
    }

    static final class CollapsibleContainerAdapter implements WidgetAdapter<CollapsibleContainer> {
        @Override
        public Class<CollapsibleContainer> widgetType() {
            return CollapsibleContainer.class;
        }

        @Override
        public void extractMetadata(CollapsibleContainer widget, Map<String, Object> target) {
            target.put("role", "collapsible");
        }
    }
}

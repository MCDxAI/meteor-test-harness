package io.mcdxai.harness.universal.modspec.itemeditor;

import io.mcdxai.harness.universal.adapter.AdapterRegistry;

import java.util.List;
import java.util.Map;

/**
 * Item-editor specific metadata decorators. Registered by simple class name so we never
 * take a compile-time dep on item-editor (which is in active development and may rename).
 *
 * Each decorator only adds richer "role" hints — the underlying owo adapter (TextAreaComponent,
 * ItemComponent, EntityComponent, etc.) already handles text/value extraction via class hierarchy.
 */
public final class ItemEditorAdapters {
    private ItemEditorAdapters() {
    }

    public static void registerAll(AdapterRegistry registry) {
        // Custom owo components (extend BaseUIComponent / TextAreaComponent / etc.).
        decorate(registry, "VirtualItemGridComponent", "item_grid", List.of("click", "scroll"));
        decorate(registry, "RichTextAreaComponent", "rich_text_area", List.of("set_text", "type_text"));
        decorate(registry, "RawTextAreaComponent", "raw_text_area", List.of("set_text", "type_text"));
        decorate(registry, "RichTextHorizontalScrollbarComponent", "rich_text_scrollbar", List.of("drag", "scroll"));
        decorate(registry, "OrbitingArmorStandComponent", "armor_stand_preview", List.of());
        decorate(registry, "RotatableItemPreviewComponent", "item_preview_3d", List.of("drag"));
        decorate(registry, "SafeDiscreteSliderComponent", "discrete_slider", List.of("set_value", "drag"));
        decorate(registry, "ScaledLabelComponent", "label", List.of());
        decorate(registry, "InputSafeScrollContainer", "scroll_container", List.of("scroll"));

        // Dialog factory output is a FlowLayout with semi-transparent ModalOverlayLayout root.
        // The factory class names appear because item-editor centralizes them; the actual instance
        // is a ModalOverlayLayout. We tag the modal wrapper if it shows up by name.
        decorate(registry, "ModalOverlayLayout", "modal_overlay", List.of());
    }

    @SuppressWarnings("unchecked")
    private static void decorate(AdapterRegistry registry, String simpleName, String role, List<String> extraActions) {
        registry.registerNameDecorator(simpleName, (widget, target) -> {
            target.put("role", role);
            target.put("modOrigin", "itemeditor");
            if (!extraActions.isEmpty()) {
                Object existing = target.get("actions");
                if (existing instanceof List<?> list) {
                    List<String> merged = new java.util.ArrayList<>();
                    for (Object item : list) merged.add(String.valueOf(item));
                    for (String x : extraActions) if (!merged.contains(x)) merged.add(x);
                    target.put("actions", merged);
                } else {
                    target.put("actions", new java.util.ArrayList<>(extraActions));
                }
            }
        });
    }
}

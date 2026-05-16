package io.mcdxai.harness.universal.modspec.itemeditor;

import io.mcdxai.harness.universal.adapter.AdapterRegistry;
import io.mcdxai.harness.universal.adapter.ScreenDescriptor;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.Map;

public final class ItemEditorScreenDescriptors {
    private ItemEditorScreenDescriptors() {
    }

    public static void registerAll(AdapterRegistry registry) {
        registry.registerScreenDescriptor(simpleName("ItemEditorScreen", "item_editor",
            List.of(
                "Main item editor with category tabs and editor panels.",
                "Apply changes via Ctrl+S, reset via Ctrl+R, switch tab via Ctrl+Tab.",
                "Switching category destroys and rebuilds the panel subtree — re-snapshot after switching."
            ),
            Map.of(
                "Ctrl+S", "apply",
                "Ctrl+R", "reset",
                "Ctrl+Tab", "next_category"
            )));

        registry.registerScreenDescriptor(simpleName("ItemEntryScreen", "item_entry",
            List.of("Entry hub when the player has an empty hand: Create, Storage, Import, Cancel."),
            Map.of()));

        registry.registerScreenDescriptor(simpleName("ItemPickerScreen", "item_picker",
            List.of("Item grid browser with search filter and modded toggle. Click an item to pick it."),
            Map.of()));

        registry.registerScreenDescriptor(simpleName("ImportScreen", "import",
            List.of("Choose to import from paste or from file."),
            Map.of()));

        registry.registerScreenDescriptor(simpleName("RawImportScreen", "raw_import",
            List.of("Paste raw SNBT/JSON text to import an item."),
            Map.of()));

        registry.registerScreenDescriptor(simpleName("ImportedItemsScreen", "imported_items",
            List.of("Chest-like multi-item import viewer. Click slots to inspect."),
            Map.of()));

        registry.registerScreenDescriptor(simpleName("StorageScreen", "storage",
            List.of(
                "Hybrid screen: vanilla container slots + owo side panel.",
                "Side panel offers search/sort and pick-for-edit actions.",
                "DOM contains both v-prefixed (slots) and o-prefixed (panel) elements."
            ),
            Map.of()));
    }

    private static ScreenDescriptor simpleName(String simpleName, String role, List<String> hints, Map<String, String> shortcuts) {
        return new ScreenDescriptor() {
            @Override
            public boolean matches(Screen screen) {
                if (screen == null) return false;
                Class<?> type = screen.getClass();
                while (type != null && type != Object.class) {
                    if (type.getSimpleName().equals(simpleName)) return true;
                    type = type.getSuperclass();
                }
                return false;
            }

            @Override
            public List<String> hints() {
                return hints;
            }

            @Override
            public Map<String, String> keyboardShortcuts() {
                return shortcuts;
            }

            @Override
            public String screenRole() {
                return role;
            }
        };
    }
}

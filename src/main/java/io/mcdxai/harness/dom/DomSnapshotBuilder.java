package io.mcdxai.harness.dom;

import io.mcdxai.harness.services.NameMappingService;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.mcdxai.harness.dom.DomValueUtils.asBoolean;
import static io.mcdxai.harness.dom.DomValueUtils.asString;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class DomSnapshotBuilder {
    private static final Field WIDGET_SCREEN_ROOT_FIELD = resolveWidgetScreenRootField();
    private static final int SNAPSHOT_CACHE_LIMIT = 8;

    private final Map<String, ElementRef> refs = new HashMap<>();
    private final LinkedHashMap<String, DomSnapshot> snapshotCache = new LinkedHashMap<>();
    private final NameMappingService nameMappingService;
    private final DomMetadataHelper metadataHelper;
    private int idCounter = 0;
    private long snapshotCounter = 0;
    private String latestSnapshotId;

    public DomSnapshotBuilder(NameMappingService nameMappingService, DomMetadataHelper metadataHelper) {
        this.nameMappingService = nameMappingService;
        this.metadataHelper = metadataHelper;
    }

    public Map<String, Object> snapshot() {
        refs.clear();
        idCounter = 0;

        Screen screen = mc.screen;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hasScreen", screen != null);
        payload.put("mappingRuntimeNamespace", nameMappingService.getRuntimeNamespace());
        payload.put("mappingPreferredNamespace", nameMappingService.getPreferredNamespace());
        payload.put("mappingMode", nameMappingService.getMappingMode());
        payload.put("mappingNamespaces", nameMappingService.getNamespaces());
        payload.put("mappingRuntimeNamedAvailable", nameMappingService.hasRuntimeNamedMappings());
        payload.put("mappingBundledNamedAvailable", nameMappingService.hasBundledNamedMappings());
        payload.put("mappingBundledNamedClassCount", nameMappingService.getBundledNamedClassCount());
        payload.put("mappingBundledSource", nameMappingService.getBundledMappingsSource());
        payload.put("mappingBundledError", nameMappingService.getBundledMappingsError());

        if (screen == null) {
            payload.put("screen", null);
            payload.put("elements", List.of());
            finalizeAndCacheSnapshot(payload, List.of(), null);
            return payload;
        }

        String screenClass = screen.getClass().getName();
        String mappedScreenClass = nameMappingService.mapClassName(screenClass);
        Map<String, Object> screenInfo = new LinkedHashMap<>();
        screenInfo.put("class", screenClass);
        screenInfo.put("classMapped", mappedScreenClass);
        screenInfo.put("type", nameMappingService.simpleName(screenClass));
        screenInfo.put("typeMapped", nameMappingService.simpleName(mappedScreenClass));
        screenInfo.put("title", screen.getTitle() == null ? "" : screen.getTitle().getString());
        screenInfo.put("meteorWidgetScreen", screen instanceof WidgetScreen);
        payload.put("screen", screenInfo);

        List<Map<String, Object>> elements;
        if (screen instanceof WidgetScreen widgetScreen) {
            elements = buildMeteorDom(widgetScreen);
            payload.put("engine", "meteor");
            if (screenClass.endsWith(".ModulesScreen")) {
                payload.put("screenHints", List.of(
                    "Module rows expose moduleName/moduleTitle/category/active metadata.",
                    "Use click_dom_element with button=0 to toggle modules and button=1 to open module settings."
                ));
            }
        } else {
            elements = buildVanillaDom(screen);
            payload.put("engine", "vanilla");
        }

        payload.put("elements", elements);
        finalizeAndCacheSnapshot(payload, elements, screenInfo);
        return payload;
    }

    public Map<String, Object> snapshotSummary(boolean refresh) {
        DomSnapshot snapshot = refresh ? refreshSnapshotForQuery() : resolveSnapshotForRead(null, true);
        if (snapshot == null) {
            return Map.of("hasSnapshot", false, "reason", "no_snapshot");
        }

        int actionable = 0;
        int clickable = 0;
        int moduleEntries = 0;

        for (Map<String, Object> element : snapshot.elementsById.values()) {
            if (asBoolean(element.get("actionable"), false)) actionable++;
            if (asBoolean(element.get("clickable"), false)) clickable++;
            if ("module_entry".equals(String.valueOf(element.get("role")))) moduleEntries++;
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("hasSnapshot", true);
        summary.put("snapshotId", snapshot.id);
        summary.put("snapshotCreatedAtMs", snapshot.createdAtMs);
        summary.put("screenSignature", snapshot.screenSignature);
        summary.put("hasScreen", asBoolean(snapshot.payload.get("hasScreen"), false));
        summary.put("engine", snapshot.payload.get("engine"));
        summary.put("elementCount", snapshot.elementCount);
        summary.put("actionableCount", actionable);
        summary.put("clickableCount", clickable);
        summary.put("moduleEntryCount", moduleEntries);
        summary.put("screen", snapshot.payload.get("screen"));
        return summary;
    }

    public DomSnapshot refreshSnapshotForQuery() {
        snapshot();
        return latestSnapshotId == null ? null : snapshotCache.get(latestSnapshotId);
    }

    public DomSnapshot resolveSnapshotForRead(String snapshotId, boolean refreshIfMissing) {
        if (snapshotId != null && !snapshotId.isBlank()) {
            DomSnapshot snapshot = snapshotCache.get(snapshotId);
            if (snapshot != null) return snapshot;
            return null;
        }

        if (latestSnapshotId != null) {
            DomSnapshot snapshot = snapshotCache.get(latestSnapshotId);
            if (snapshot != null) return snapshot;
        }

        return refreshIfMissing ? refreshSnapshotForQuery() : null;
    }

    public ElementRef resolveRef(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        ElementRef ref = refs.get(id);
        if (ref != null) {
            return ref;
        }

        snapshot();
        return refs.get(id);
    }

    private List<Map<String, Object>> buildVanillaDom(Screen screen) {
        List<Map<String, Object>> elements = new ArrayList<>();

        List<? extends GuiEventListener> children = screen.children();
        for (GuiEventListener child : children) {
            elements.add(mapVanillaElement(child, null));
        }

        return elements;
    }

    private Map<String, Object> mapVanillaElement(GuiEventListener element, AbstractSelectionList<?> owningList) {
        String id = nextId("v");

        ElementRef ref = new ElementRef(id, element);
        refs.put(id, ref);

        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("id", id);
        mapped.put("engine", "vanilla");
        metadataHelper.putClassMetadata(mapped, element.getClass());

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

        if (element instanceof LayoutElement widget) {
            mapped.putIfAbsent("x", widget.getX());
            mapped.putIfAbsent("y", widget.getY());
            mapped.putIfAbsent("width", widget.getWidth());
            mapped.putIfAbsent("height", widget.getHeight());

            if (Double.isNaN(ref.x)) {
                ref.x = widget.getX();
                ref.y = widget.getY();
                ref.width = widget.getWidth();
                ref.height = widget.getHeight();
            }
        }

        if (owningList != null && element instanceof LayoutElement && !(element instanceof AbstractWidget)) {
            mapped.put("selected", isSelectedInList(owningList, element));
        }

        if (owningList != null && owningList.children().contains(element)) {
            mapped.putIfAbsent("role", "list_entry");
        }

        if (element instanceof EditBox textFieldWidget) {
            mapped.put("text", textFieldWidget.getValue());
            mapped.put("editable", textFieldWidget.isActive());
        }

        DomActionHints.addVanillaHints(mapped, element, owningList, ref.x, ref.y, ref.width, ref.height);

        AbstractSelectionList<?> nextOwningList = element instanceof AbstractSelectionList<?> entryListWidget ? entryListWidget : owningList;
        if (element instanceof ContainerEventHandler parentElement) {
            List<Map<String, Object>> children = new ArrayList<>();
            for (GuiEventListener child : parentElement.children()) {
                if (child == element) continue;
                children.add(mapVanillaElement(child, nextOwningList));
            }
            mapped.put("children", children);
        }

        return mapped;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean isSelectedInList(AbstractSelectionList<?> entryList, GuiEventListener element) {
        AbstractSelectionList rawList = (AbstractSelectionList) entryList;
        return rawList.getSelected() == element;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildMeteorDom(WidgetScreen screen) {
        List<Map<String, Object>> elements = new ArrayList<>();
        if (WIDGET_SCREEN_ROOT_FIELD == null) {
            return elements;
        }

        try {
            WContainer root = (WContainer) WIDGET_SCREEN_ROOT_FIELD.get(screen);
            if (root == null) return elements;

            for (Cell<?> cell : root.cells) {
                elements.add(mapMeteorWidget(cell.widget()));
            }
        } catch (Exception ignored) {
            // Return whatever we have if reflection fails.
        }

        return elements;
    }

    private static Field resolveWidgetScreenRootField() {
        try {
            Field field = WidgetScreen.class.getDeclaredField("root");
            field.setAccessible(true);
            return field;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void finalizeAndCacheSnapshot(Map<String, Object> payload, List<Map<String, Object>> rootElements, Map<String, Object> screenInfo) {
        long now = System.currentTimeMillis();
        String snapshotId = "s-" + (++snapshotCounter);
        int elementCount = countElements(rootElements);
        String screenSignature = buildScreenSignature(payload, screenInfo, elementCount);

        payload.put("snapshotId", snapshotId);
        payload.put("snapshotCreatedAtMs", now);
        payload.put("screenSignature", screenSignature);
        payload.put("elementCount", elementCount);

        Map<String, Map<String, Object>> elementIndex = new LinkedHashMap<>();
        indexElements(rootElements, elementIndex);

        DomSnapshot snapshot = new DomSnapshot(snapshotId, now, screenSignature, elementCount, payload, rootElements, elementIndex);
        snapshotCache.put(snapshotId, snapshot);
        latestSnapshotId = snapshotId;

        while (snapshotCache.size() > SNAPSHOT_CACHE_LIMIT) {
            String oldestId = snapshotCache.keySet().iterator().next();
            snapshotCache.remove(oldestId);
            if (oldestId.equals(latestSnapshotId)) {
                latestSnapshotId = null;
            }
        }
    }

    private String buildScreenSignature(Map<String, Object> payload, Map<String, Object> screenInfo, int elementCount) {
        if (screenInfo == null) {
            return "none|" + elementCount;
        }

        String className = asString(screenInfo.get("class"), "none");
        String title = asString(screenInfo.get("title"), "");
        String engine = asString(payload.get("engine"), "unknown");
        return className + "|" + title + "|" + engine + "|" + elementCount;
    }

    private int countElements(List<Map<String, Object>> elements) {
        int total = 0;
        for (Map<String, Object> element : elements) {
            total++;
            total += countElements(childMaps(element));
        }
        return total;
    }

    private void indexElements(List<Map<String, Object>> elements, Map<String, Map<String, Object>> index) {
        for (Map<String, Object> element : elements) {
            Object id = element.get("id");
            if (id != null) {
                index.put(String.valueOf(id), element);
            }
            indexElements(childMaps(element), index);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> childMaps(Map<String, Object> element) {
        Object children = element.get("children");
        if (!(children instanceof List<?> rawChildren) || rawChildren.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> mappedChildren = new ArrayList<>();
        for (Object raw : rawChildren) {
            if (raw instanceof Map<?, ?> child) {
                mappedChildren.add((Map<String, Object>) child);
            }
        }
        return mappedChildren;
    }

    private Map<String, Object> mapMeteorWidget(WWidget widget) {
        String id = nextId("m");

        ElementRef ref = new ElementRef(id, widget);
        refs.put(id, ref);

        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("id", id);
        mapped.put("engine", "meteor");
        metadataHelper.putClassMetadata(mapped, widget.getClass());
        mapped.put("visible", widget.visible);
        mapped.put("focused", widget.isFocused());
        mapped.put("x", widget.x);
        mapped.put("y", widget.y);
        mapped.put("width", widget.width);
        mapped.put("height", widget.height);

        ref.x = widget.x;
        ref.y = widget.y;
        ref.width = widget.width;
        ref.height = widget.height;

        if (widget instanceof WButton button) {
            mapped.put("label", button.getText());
        }
        if (widget instanceof WLabel label) {
            mapped.put("label", label.get());
        }
        if (widget instanceof WTextBox textBox) {
            mapped.put("text", textBox.get());
        }
        if (widget instanceof WCheckbox checkBox) {
            mapped.put("checked", checkBox.checked);
        }
        if (widget instanceof WSlider slider) {
            mapped.put("value", slider.get());
        }

        MeteorModuleMetadata moduleMetadata = extractMeteorModuleMetadata(widget);
        if (moduleMetadata != null) {
            mapped.put("role", "module_entry");
            mapped.put("moduleName", moduleMetadata.name);
            mapped.put("moduleTitle", moduleMetadata.title);
            mapped.put("moduleCategory", moduleMetadata.category);
            mapped.put("moduleActive", moduleMetadata.active);
            mapped.put("moduleUiLabel", moduleMetadata.uiLabel);
            mapped.put("leftClickAction", "toggle_module");
            mapped.put("rightClickAction", "open_module_settings");
            mapped.put("label", moduleMetadata.uiLabel);

            ref.role = "module_entry";
            ref.moduleName = moduleMetadata.name;
            ref.moduleTitle = moduleMetadata.title;
        }

        DomActionHints.addMeteorHints(mapped, widget, ref.width, ref.height, moduleMetadata != null);

        if (widget instanceof WContainer container) {
            List<Map<String, Object>> children = new ArrayList<>();
            for (Cell<?> cell : container.cells) {
                children.add(mapMeteorWidget(cell.widget()));
            }
            mapped.put("children", children);
        }

        return mapped;
    }

    private MeteorModuleMetadata extractMeteorModuleMetadata(WWidget widget) {
        if (!(widget instanceof WPressable)) {
            return null;
        }

        Object moduleObject = metadataHelper.findFieldValueByType(widget, Module.class.getName());
        if (!(moduleObject instanceof Module module)) {
            return null;
        }

        String uiLabel = null;
        Object titleFieldValue = metadataHelper.findFieldValue(widget, "title");
        if (titleFieldValue instanceof String titleText && !titleText.isBlank()) {
            uiLabel = titleText;
        }

        if (uiLabel == null || uiLabel.isBlank()) {
            uiLabel = module.title != null && !module.title.isBlank() ? module.title : module.name;
        }

        String category = module.category == null ? "" : module.category.name;
        return new MeteorModuleMetadata(module.name, module.title, category, uiLabel, module.isActive());
    }

    private String nextId(String prefix) {
        idCounter++;
        return prefix + "-" + idCounter;
    }
}

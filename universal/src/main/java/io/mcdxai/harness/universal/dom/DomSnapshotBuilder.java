package io.mcdxai.harness.universal.dom;

import io.mcdxai.harness.universal.adapter.AdapterRegistry;
import io.mcdxai.harness.universal.adapter.DomBuildContext;
import io.mcdxai.harness.universal.adapter.ScreenDescriptor;
import io.mcdxai.harness.universal.adapter.ScreenEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.mcdxai.harness.universal.dom.DomValueUtils.asString;

/** Builds DomSnapshot instances by dispatching to a registered ScreenEngine. */
public final class DomSnapshotBuilder {
    private static final int SNAPSHOT_CACHE_LIMIT = 8;

    private final AdapterRegistry registry;
    private final LinkedHashMap<String, DomSnapshot> snapshotCache = new LinkedHashMap<>();
    private Map<String, ElementRef> latestRefs = new java.util.HashMap<>();
    private long snapshotCounter = 0;
    private String latestSnapshotId;

    public DomSnapshotBuilder(AdapterRegistry registry) {
        this.registry = registry;
    }

    public synchronized Map<String, Object> snapshot() {
        Screen screen = Minecraft.getInstance().screen;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hasScreen", screen != null);

        if (screen == null) {
            payload.put("screen", null);
            payload.put("elements", List.of());
            payload.put("engine", null);
            finalize(payload, List.of(), null, null);
            return payload;
        }

        Optional<ScreenEngine> engineOpt = registry.findScreenEngine(screen);
        if (engineOpt.isEmpty()) {
            payload.put("screen", screenInfo(screen, null));
            payload.put("elements", List.of());
            payload.put("engine", "none");
            payload.put("engineMessage", "No registered ScreenEngine matched this screen.");
            finalize(payload, List.of(), screenInfo(screen, null), null);
            return payload;
        }

        ScreenEngine engine = engineOpt.get();
        DomBuildContext ctx = new DomBuildContext(registry);
        List<Map<String, Object>> elements;
        try {
            elements = engine.buildDom(screen, ctx);
        } catch (Throwable t) {
            payload.put("screen", screenInfo(screen, engine));
            payload.put("elements", List.of());
            payload.put("engine", engine.engineName());
            payload.put("engineError", t.getClass().getSimpleName() + ": " + t.getMessage());
            finalize(payload, List.of(), screenInfo(screen, engine), ctx);
            return payload;
        }

        Map<String, Object> screenInfo = screenInfo(screen, engine);
        List<ScreenDescriptor> descriptors = registry.findScreenDescriptors(screen);
        if (!descriptors.isEmpty()) {
            List<String> hints = new ArrayList<>();
            Map<String, String> shortcuts = new LinkedHashMap<>();
            String role = null;
            for (ScreenDescriptor descriptor : descriptors) {
                hints.addAll(descriptor.hints());
                shortcuts.putAll(descriptor.keyboardShortcuts());
                if (role == null && descriptor.screenRole() != null) role = descriptor.screenRole();
            }
            if (!hints.isEmpty()) screenInfo.put("hints", hints);
            if (!shortcuts.isEmpty()) screenInfo.put("keyboardShortcuts", shortcuts);
            if (role != null) screenInfo.put("role", role);
        }

        payload.put("screen", screenInfo);
        payload.put("engine", engine.engineName());
        payload.put("elements", elements);
        finalize(payload, elements, screenInfo, ctx);
        return payload;
    }

    public synchronized Map<String, Object> snapshotSummary(boolean refresh) {
        DomSnapshot snapshot = refresh ? refreshSnapshotForQuery() : resolveSnapshotForRead(null, true);
        if (snapshot == null) {
            return Map.of("hasSnapshot", false, "reason", "no_snapshot");
        }

        int actionable = 0;
        int clickable = 0;
        for (Map<String, Object> element : snapshot.elementsById.values()) {
            if (DomValueUtils.asBoolean(element.get("actionable"), false)) actionable++;
            if (DomValueUtils.asBoolean(element.get("clickable"), false)) clickable++;
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("hasSnapshot", true);
        summary.put("snapshotId", snapshot.id);
        summary.put("snapshotCreatedAtMs", snapshot.createdAtMs);
        summary.put("screenSignature", snapshot.screenSignature);
        summary.put("hasScreen", DomValueUtils.asBoolean(snapshot.payload.get("hasScreen"), false));
        summary.put("engine", snapshot.payload.get("engine"));
        summary.put("elementCount", snapshot.elementCount);
        summary.put("actionableCount", actionable);
        summary.put("clickableCount", clickable);
        summary.put("screen", snapshot.payload.get("screen"));
        return summary;
    }

    public synchronized DomSnapshot refreshSnapshotForQuery() {
        snapshot();
        return latestSnapshotId == null ? null : snapshotCache.get(latestSnapshotId);
    }

    public synchronized DomSnapshot resolveSnapshotForRead(String snapshotId, boolean refreshIfMissing) {
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

    public synchronized ElementRef resolveRef(String id) {
        if (id == null || id.isBlank()) return null;
        // No auto-refresh here — taking a fresh snapshot would invalidate every other
        // outstanding ID at once. If the caller's ID is gone, they must re-snapshot
        // explicitly and pick a new ID from the new tree.
        return latestRefs.get(id);
    }

    private Map<String, Object> screenInfo(Screen screen, ScreenEngine engine) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = screen.getClass();
        info.put("class", clazz.getName());
        info.put("type", clazz.getSimpleName());
        try {
            info.put("title", screen.getTitle() == null ? "" : screen.getTitle().getString());
        } catch (Throwable ignored) {
            info.put("title", "");
        }
        if (engine != null) info.put("engine", engine.engineName());
        return info;
    }

    private void finalize(Map<String, Object> payload, List<Map<String, Object>> rootElements, Map<String, Object> screenInfo, DomBuildContext ctx) {
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
        latestRefs = ctx == null ? new java.util.HashMap<>() : ctx.refs();

        while (snapshotCache.size() > SNAPSHOT_CACHE_LIMIT) {
            String oldestId = snapshotCache.keySet().iterator().next();
            snapshotCache.remove(oldestId);
            if (oldestId.equals(latestSnapshotId)) latestSnapshotId = null;
        }
    }

    private String buildScreenSignature(Map<String, Object> payload, Map<String, Object> screenInfo, int elementCount) {
        if (screenInfo == null) return "none|" + elementCount;
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
            if (id != null) index.put(String.valueOf(id), element);
            indexElements(childMaps(element), index);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> childMaps(Map<String, Object> element) {
        Object children = element.get("children");
        if (!(children instanceof List<?> rawChildren) || rawChildren.isEmpty()) return Collections.emptyList();
        List<Map<String, Object>> mappedChildren = new ArrayList<>();
        for (Object raw : rawChildren) {
            if (raw instanceof Map<?, ?> child) mappedChildren.add((Map<String, Object>) child);
        }
        return mappedChildren;
    }
}

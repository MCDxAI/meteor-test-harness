package io.mcdxai.harness.universal.mcp.tools;

import io.mcdxai.harness.universal.services.ScreenDomService;
import io.mcdxai.harness.universal.util.MainThreadInvoker;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DomToolHelper {
    private static final Duration MAIN_THREAD_TIMEOUT = Duration.ofSeconds(10);

    private final ScreenDomService screenDomService;

    DomToolHelper(ScreenDomService screenDomService) {
        this.screenDomService = screenDomService;
    }

    Map<String, Object> withInteraction(Map<String, Object> interaction) {
        Map<String, Object> dom = new LinkedHashMap<>(snapshotOnMain());
        dom.put("interaction", interaction);
        dom.put("success", isSuccess(interaction));
        return dom;
    }

    /**
     * Lean error envelope: include screen-level context and (if available) just the target
     * element's self-row — never the full tree, which can be thousands of tokens on a deep
     * screen. Callers that need the tree can re-query with the returned snapshotId.
     */
    Map<String, Object> errorDetails(Map<String, Object> interaction) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("interaction", interaction);

        Map<String, Object> snapshot = snapshotOnMain();
        Map<String, Object> trimmed = new LinkedHashMap<>();
        copyIfPresent(snapshot, trimmed, "hasScreen", "screen", "engine",
            "snapshotId", "snapshotCreatedAtMs", "screenSignature", "elementCount",
            "engineMessage", "engineError");

        Object elementId = interaction.get("elementId");
        if (elementId instanceof String idStr && !idStr.isBlank()) {
            Map<String, Object> target = findElementShallow(snapshot.get("elements"), idStr);
            if (target != null) trimmed.put("targetElement", target);
        }

        details.put("dom", trimmed);
        details.put("hint", "Re-query the full DOM via get_screen_dom or find_dom_elements using the snapshotId above.");
        return details;
    }

    boolean isSuccess(Map<String, Object> result) {
        Object value = result.get("success");
        return value instanceof Boolean booleanValue && booleanValue;
    }

    private static void copyIfPresent(Map<String, Object> src, Map<String, Object> dest, String... keys) {
        for (String key : keys) {
            if (src.containsKey(key)) dest.put(key, src.get(key));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> findElementShallow(Object elementsRaw, String id) {
        if (!(elementsRaw instanceof List<?> elements)) return null;
        for (Object raw : elements) {
            if (!(raw instanceof Map<?, ?> element)) continue;
            Map<String, Object> mapped = (Map<String, Object>) element;
            if (id.equals(String.valueOf(mapped.get("id")))) {
                Map<String, Object> shallow = new LinkedHashMap<>(mapped);
                shallow.remove("children");
                return shallow;
            }
            Map<String, Object> nested = findElementShallow(mapped.get("children"), id);
            if (nested != null) return nested;
        }
        return null;
    }

    /** Snapshots must read MC state on the render thread; interaction tools run on the MCP thread. */
    private Map<String, Object> snapshotOnMain() {
        return MainThreadInvoker.call(screenDomService::snapshot, MAIN_THREAD_TIMEOUT);
    }
}

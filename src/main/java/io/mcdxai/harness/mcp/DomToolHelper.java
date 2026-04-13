package io.mcdxai.harness.mcp;

import io.mcdxai.harness.services.ScreenDomService;

import java.util.LinkedHashMap;
import java.util.Map;

final class DomToolHelper {
    private final ScreenDomService screenDomService;

    DomToolHelper(ScreenDomService screenDomService) {
        this.screenDomService = screenDomService;
    }

    Map<String, Object> withInteraction(Map<String, Object> interaction) {
        Map<String, Object> dom = new LinkedHashMap<>(screenDomService.snapshot());
        dom.put("interaction", interaction);
        dom.put("success", isSuccess(interaction));
        return dom;
    }

    Map<String, Object> errorDetails(Map<String, Object> interaction) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("interaction", interaction);
        details.put("dom", screenDomService.snapshot());
        return details;
    }

    boolean isSuccess(Map<String, Object> result) {
        Object value = result.get("success");
        return value instanceof Boolean booleanValue && booleanValue;
    }
}

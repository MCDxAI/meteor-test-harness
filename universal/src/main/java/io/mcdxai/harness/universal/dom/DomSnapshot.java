package io.mcdxai.harness.universal.dom;

import java.util.List;
import java.util.Map;

public final class DomSnapshot {
    public final String id;
    public final long createdAtMs;
    public final String screenSignature;
    public final int elementCount;
    public final Map<String, Object> payload;
    public final List<Map<String, Object>> rootElements;
    public final Map<String, Map<String, Object>> elementsById;

    public DomSnapshot(
        String id,
        long createdAtMs,
        String screenSignature,
        int elementCount,
        Map<String, Object> payload,
        List<Map<String, Object>> rootElements,
        Map<String, Map<String, Object>> elementsById
    ) {
        this.id = id;
        this.createdAtMs = createdAtMs;
        this.screenSignature = screenSignature;
        this.elementCount = elementCount;
        this.payload = payload;
        this.rootElements = rootElements;
        this.elementsById = elementsById;
    }
}

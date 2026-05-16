package io.mcdxai.harness.universal.adapter;

import io.mcdxai.harness.universal.dom.ElementRef;

import java.util.HashMap;
import java.util.Map;

/** Per-snapshot mutable state shared across engine traversal. */
public final class DomBuildContext {
    private final AdapterRegistry registry;
    private final Map<String, ElementRef> refs = new HashMap<>();
    private int idCounter = 0;

    public DomBuildContext(AdapterRegistry registry) {
        this.registry = registry;
    }

    public AdapterRegistry registry() {
        return registry;
    }

    public String nextId(String prefix) {
        idCounter++;
        return prefix + "-" + idCounter;
    }

    public void storeRef(String id, ElementRef ref) {
        refs.put(id, ref);
    }

    public Map<String, ElementRef> refs() {
        return refs;
    }
}

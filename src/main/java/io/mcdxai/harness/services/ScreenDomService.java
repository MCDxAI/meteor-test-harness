package io.mcdxai.harness.services;

import io.mcdxai.harness.dom.DomEntryListHelper;
import io.mcdxai.harness.dom.DomInteractor;
import io.mcdxai.harness.dom.DomMetadataHelper;
import io.mcdxai.harness.dom.DomQueryEngine;
import io.mcdxai.harness.dom.DomSnapshot;
import io.mcdxai.harness.dom.DomSnapshotBuilder;

import java.util.Map;
import java.util.List;

public final class ScreenDomService {
    private final DomSnapshotBuilder snapshotBuilder;
    private final DomQueryEngine queryEngine;
    private final DomInteractor interactor;

    public ScreenDomService(NameMappingService nameMappingService) {
        DomMetadataHelper metadataHelper = new DomMetadataHelper(nameMappingService);
        this.snapshotBuilder = new DomSnapshotBuilder(nameMappingService, metadataHelper);
        this.queryEngine = new DomQueryEngine();
        this.interactor = new DomInteractor(snapshotBuilder, queryEngine, new DomEntryListHelper(), metadataHelper);
    }

    public synchronized Map<String, Object> snapshot() {
        return snapshotBuilder.snapshot();
    }

    public synchronized Map<String, Object> snapshotSummary(boolean refresh) {
        return snapshotBuilder.snapshotSummary(refresh);
    }

    public synchronized Map<String, Object> findElements(
        String snapshotId,
        Map<String, Object> filters,
        int limit,
        List<Object> fieldsRaw,
        boolean includeChildren
    ) {
        DomSnapshot snapshot = snapshotBuilder.resolveSnapshotForRead(snapshotId, true);
        return queryEngine.findElements(snapshotId, snapshot, filters, limit, fieldsRaw, includeChildren);
    }

    public synchronized Map<String, Object> getElement(
        String snapshotId,
        String elementId,
        List<Object> fieldsRaw,
        boolean includeChildren
    ) {
        DomSnapshot snapshot = snapshotBuilder.resolveSnapshotForRead(snapshotId, true);
        return queryEngine.getElement(snapshotId, snapshot, elementId, fieldsRaw, includeChildren);
    }

    public synchronized Map<String, Object> getSubtree(
        String snapshotId,
        String elementId,
        int depth,
        List<Object> fieldsRaw
    ) {
        DomSnapshot snapshot = snapshotBuilder.resolveSnapshotForRead(snapshotId, true);
        return queryEngine.getSubtree(snapshotId, snapshot, elementId, depth, fieldsRaw);
    }

    public synchronized Map<String, Object> clickByQueryDetailed(
        Map<String, Object> filters,
        int index,
        int button,
        boolean doubled
    ) {
        return interactor.clickByQueryDetailed(filters, index, button, doubled);
    }

    public synchronized Map<String, Object> setTextByQueryDetailed(
        Map<String, Object> filters,
        int index,
        String text,
        boolean submit,
        boolean typeCharacters,
        boolean clearFirst
    ) {
        return interactor.setTextByQueryDetailed(filters, index, text, submit, typeCharacters, clearFirst);
    }

    public synchronized Map<String, Object> clickDetailed(String id, int button, boolean doubled) {
        return interactor.clickDetailed(id, button, doubled);
    }

    public synchronized Map<String, Object> setTextDetailed(String id, String text, boolean submit, boolean typeCharacters, boolean clearFirst) {
        return interactor.setTextDetailed(id, text, submit, typeCharacters, clearFirst);
    }

    public synchronized Map<String, Object> typeTextDetailed(String id, String text, boolean clearFirst, boolean submit) {
        return interactor.typeTextDetailed(id, text, clearFirst, submit);
    }

    public synchronized Map<String, Object> setValueDetailed(String id, Object value) {
        return interactor.setValueDetailed(id, value);
    }

    public synchronized Map<String, Object> pressKeyDetailed(String keyName, int modifiers, int repeat, boolean release) {
        return interactor.pressKeyDetailed(keyName, modifiers, repeat, release);
    }

    public synchronized Map<String, Object> scrollDetailed(String id, double verticalAmount, double horizontalAmount) {
        return interactor.scrollDetailed(id, verticalAmount, horizontalAmount);
    }

    public synchronized Map<String, Object> dragDetailed(String id, double offsetX, double offsetY, int steps, int button) {
        return interactor.dragDetailed(id, offsetX, offsetY, steps, button);
    }

    public boolean navigateBack() {
        return interactor.navigateBack();
    }
}

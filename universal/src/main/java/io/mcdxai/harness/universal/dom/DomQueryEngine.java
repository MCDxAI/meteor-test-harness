package io.mcdxai.harness.universal.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static io.mcdxai.harness.universal.dom.DomValueUtils.asBoolean;
import static io.mcdxai.harness.universal.dom.DomValueUtils.asString;
import static io.mcdxai.harness.universal.dom.DomValueUtils.asStringList;

public final class DomQueryEngine {
    private static final int DEFAULT_QUERY_LIMIT = 32;
    private static final int MAX_QUERY_LIMIT = 512;

    public Map<String, Object> findElements(
        String snapshotId,
        DomSnapshot snapshot,
        Map<String, Object> filters,
        int limit,
        List<Object> fieldsRaw,
        boolean includeChildren
    ) {
        if (snapshot == null) {
            return Map.of("success", false, "reason", "snapshot_not_found", "snapshotId", snapshotId);
        }

        int clampedLimit = clampLimit(limit);
        List<String> fields = normalizeFields(fieldsRaw);

        List<Map<String, Object>> allMatches = findMatchingElements(snapshot, filters);
        int returned = Math.min(allMatches.size(), clampedLimit);

        List<Map<String, Object>> projected = new ArrayList<>(returned);
        for (int i = 0; i < returned; i++) {
            projected.add(projectElement(allMatches.get(i), fields, includeChildren, includeChildren ? -1 : 0));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("snapshotId", snapshot.id);
        response.put("screenSignature", snapshot.screenSignature);
        response.put("totalMatches", allMatches.size());
        response.put("returned", projected.size());
        response.put("limit", clampedLimit);
        response.put("elements", projected);
        return response;
    }

    public Map<String, Object> getElement(
        String snapshotId,
        DomSnapshot snapshot,
        String elementId,
        List<Object> fieldsRaw,
        boolean includeChildren
    ) {
        if (snapshot == null) {
            return Map.of("success", false, "reason", "snapshot_not_found", "snapshotId", snapshotId);
        }

        if (elementId == null || elementId.isBlank()) {
            return Map.of("success", false, "reason", "element_id_required", "snapshotId", snapshot.id);
        }

        Map<String, Object> element = snapshot.elementsById.get(elementId);
        if (element == null) {
            return Map.of("success", false, "reason", "element_not_found", "snapshotId", snapshot.id, "elementId", elementId);
        }

        List<String> fields = normalizeFields(fieldsRaw);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("snapshotId", snapshot.id);
        response.put("screenSignature", snapshot.screenSignature);
        response.put("element", projectElement(element, fields, includeChildren, includeChildren ? -1 : 0));
        return response;
    }

    public Map<String, Object> getSubtree(
        String snapshotId,
        DomSnapshot snapshot,
        String elementId,
        int depth,
        List<Object> fieldsRaw
    ) {
        if (snapshot == null) {
            return Map.of("success", false, "reason", "snapshot_not_found", "snapshotId", snapshotId);
        }

        if (elementId == null || elementId.isBlank()) {
            return Map.of("success", false, "reason", "element_id_required", "snapshotId", snapshot.id);
        }

        Map<String, Object> element = snapshot.elementsById.get(elementId);
        if (element == null) {
            return Map.of("success", false, "reason", "element_not_found", "snapshotId", snapshot.id, "elementId", elementId);
        }

        int clampedDepth = Math.max(0, Math.min(32, depth));
        List<String> fields = normalizeFields(fieldsRaw);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("snapshotId", snapshot.id);
        response.put("screenSignature", snapshot.screenSignature);
        response.put("depth", clampedDepth);
        response.put("element", projectElement(element, fields, true, clampedDepth));
        return response;
    }

    public List<Map<String, Object>> findMatchingElements(DomSnapshot snapshot, Map<String, Object> filters) {
        if (snapshot == null) return List.of();

        List<Map<String, Object>> matches = new ArrayList<>();
        for (Map<String, Object> element : snapshot.elementsById.values()) {
            if (matchesFilters(element, filters)) {
                matches.add(element);
            }
        }
        return matches;
    }

    public List<String> normalizeFields(List<Object> fieldsRaw) {
        if (fieldsRaw == null || fieldsRaw.isEmpty()) return List.of();

        Set<String> fields = new LinkedHashSet<>();
        for (Object raw : fieldsRaw) {
            if (raw == null) continue;
            String field = String.valueOf(raw).trim();
            if (field.isEmpty()) continue;
            fields.add(normalizeFilterKey(field));
        }
        fields.add("id");
        return new ArrayList<>(fields);
    }

    public Map<String, Object> projectElement(
        Map<String, Object> element,
        List<String> fields,
        boolean includeChildren,
        int depth
    ) {
        if (element == null) return Map.of();
        return projectElementRecursive(element, fields, includeChildren, depth);
    }

    private boolean matchesFilters(Map<String, Object> element, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) return true;

        boolean exact = asBoolean(filters.get("exact"), false);
        boolean regex = asBoolean(filters.get("regex"), false);
        boolean caseSensitive = asBoolean(filters.get("case_sensitive"), false);
        if (!filters.containsKey("case_sensitive")) {
            caseSensitive = asBoolean(filters.get("caseSensitive"), false);
        }

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = normalizeFilterKey(entry.getKey());
            Object expected = entry.getValue();

            if (key.equals("exact") || key.equals("regex") || key.equals("case_sensitive") || key.equals("casesensitive")) {
                continue;
            }

            if (key.equals("text") || key.equals("query") || key.equals("q")) {
                if (!matchesGlobalText(element, expected, exact, regex, caseSensitive)) return false;
                continue;
            }

            if (key.equals("action") || key.equals("actions_any")) {
                if (!matchesActionsAny(element, expected, exact, regex, caseSensitive)) return false;
                continue;
            }

            if (key.equals("actions_all")) {
                if (!matchesActionsAll(element, expected, exact, regex, caseSensitive)) return false;
                continue;
            }

            Object actual = element.get(key);
            if (!matchesExpected(actual, expected, exact, regex, caseSensitive)) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesGlobalText(
        Map<String, Object> element,
        Object expected,
        boolean exact,
        boolean regex,
        boolean caseSensitive
    ) {
        List<String> needles = asStringList(expected);
        if (needles.isEmpty()) return false;

        List<String> haystacks = List.of(
            asString(element.get("id"), ""),
            asString(element.get("label"), ""),
            asString(element.get("text"), ""),
            asString(element.get("role"), ""),
            asString(element.get("type"), ""),
            asString(element.get("class"), ""),
            asString(element.get("componentId"), "")
        );

        for (String needle : needles) {
            if (needle.isBlank()) continue;
            for (String haystack : haystacks) {
                if (haystack.isBlank()) continue;
                if (matchesString(haystack, needle, exact, regex, caseSensitive)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean matchesActionsAny(
        Map<String, Object> element,
        Object expected,
        boolean exact,
        boolean regex,
        boolean caseSensitive
    ) {
        List<String> expectedActions = asStringList(expected);
        if (expectedActions.isEmpty()) return false;

        List<String> actions = asStringList(element.get("actions"));
        if (actions.isEmpty()) return false;

        for (String expectedAction : expectedActions) {
            for (String action : actions) {
                if (matchesString(action, expectedAction, exact, regex, caseSensitive)) return true;
            }
        }
        return false;
    }

    private boolean matchesActionsAll(
        Map<String, Object> element,
        Object expected,
        boolean exact,
        boolean regex,
        boolean caseSensitive
    ) {
        List<String> expectedActions = asStringList(expected);
        if (expectedActions.isEmpty()) return false;

        List<String> actions = asStringList(element.get("actions"));
        if (actions.isEmpty()) return false;

        for (String expectedAction : expectedActions) {
            boolean found = false;
            for (String action : actions) {
                if (matchesString(action, expectedAction, exact, regex, caseSensitive)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        return true;
    }

    private boolean matchesExpected(
        Object actual,
        Object expected,
        boolean exact,
        boolean regex,
        boolean caseSensitive
    ) {
        if (expected instanceof List<?> expectedList) {
            if (expectedList.isEmpty()) return false;
            for (Object item : expectedList) {
                if (matchesExpected(actual, item, exact, regex, caseSensitive)) return true;
            }
            return false;
        }

        if (actual instanceof List<?> actualList) {
            for (Object item : actualList) {
                if (matchesExpected(item, expected, exact, regex, caseSensitive)) return true;
            }
            return false;
        }

        if (expected instanceof Boolean expectedBoolean) {
            return asBoolean(actual, false) == expectedBoolean;
        }

        if (expected instanceof Number expectedNumber) {
            if (!(actual instanceof Number actualNumber)) return false;
            return Double.compare(actualNumber.doubleValue(), expectedNumber.doubleValue()) == 0;
        }

        String expectedString = asString(expected, "");
        if (expectedString.isBlank()) {
            return asString(actual, "").isBlank();
        }

        String actualString = asString(actual, "");
        return matchesString(actualString, expectedString, exact, regex, caseSensitive);
    }

    private boolean matchesString(String actual, String expected, boolean exact, boolean regex, boolean caseSensitive) {
        if (actual == null || expected == null) return false;

        if (regex) {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            try {
                return Pattern.compile(expected, flags).matcher(actual).find();
            } catch (PatternSyntaxException ignored) {
                return false;
            }
        }

        if (exact) {
            return caseSensitive ? actual.equals(expected) : actual.equalsIgnoreCase(expected);
        }

        if (!caseSensitive) {
            return actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
        }

        return actual.contains(expected);
    }

    private String normalizeFilterKey(String key) {
        if (key == null) return "";
        return key.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private Map<String, Object> projectElementRecursive(
        Map<String, Object> element,
        List<String> fields,
        boolean includeChildren,
        int depth
    ) {
        Map<String, Object> projected = new LinkedHashMap<>();

        if (fields == null || fields.isEmpty()) {
            for (Map.Entry<String, Object> entry : element.entrySet()) {
                if ("children".equals(entry.getKey())) continue;
                projected.put(entry.getKey(), entry.getValue());
            }
        } else {
            for (String field : fields) {
                if ("children".equals(field)) continue;
                if (element.containsKey(field)) {
                    projected.put(field, element.get(field));
                }
            }
            projected.putIfAbsent("id", element.get("id"));
        }

        List<Map<String, Object>> children = childMaps(element);
        if (includeChildren && depth != 0 && !children.isEmpty()) {
            int nextDepth = depth < 0 ? -1 : depth - 1;
            List<Map<String, Object>> projectedChildren = new ArrayList<>(children.size());
            for (Map<String, Object> child : children) {
                projectedChildren.add(projectElementRecursive(child, fields, true, nextDepth));
            }
            projected.put("children", projectedChildren);
        } else if (!includeChildren) {
            projected.put("childCount", children.size());
        }

        return projected;
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

    private int clampLimit(int limit) {
        int requested = limit <= 0 ? DEFAULT_QUERY_LIMIT : limit;
        return Math.max(1, Math.min(MAX_QUERY_LIMIT, requested));
    }
}

package com.mcdxai.meteortestharness.services;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class NameMappingService {
    private static final String NAMED_NAMESPACE = "named";
    private static final String INTERMEDIARY_NAMESPACE = "intermediary";
    private static final String UNKNOWN_NAMESPACE = "unknown";
    private static final String BUNDLED_YARN_MAPPINGS_RESOURCE = "/mappings/yarn.tiny";

    private final MappingResolver mappingResolver;
    private final Set<String> namespaces;
    private final String runtimeNamespace;
    private final String preferredNamespace;
    private final Map<String, String> bundledIntermediaryToNamed;
    private final boolean runtimeNamedAvailable;
    private final boolean bundledNamedAvailable;
    private final String mappingMode;
    private final ConcurrentMap<String, String> classNameCache = new ConcurrentHashMap<>();

    public NameMappingService() {
        MappingResolver resolver = null;
        Set<String> availableNamespaces = Set.of();
        String runtime = UNKNOWN_NAMESPACE;

        try {
            resolver = FabricLoader.getInstance().getMappingResolver();
            availableNamespaces = Set.copyOf(new LinkedHashSet<>(resolver.getNamespaces()));
            runtime = resolver.getCurrentRuntimeNamespace();
        } catch (Exception ignored) {
            // Leave mapping disabled when resolver is unavailable.
        }

        Map<String, String> bundledMappings = loadBundledIntermediaryToNamed();
        boolean hasRuntimeNamed = availableNamespaces.contains(NAMED_NAMESPACE);
        boolean hasBundledNamed = !bundledMappings.isEmpty();

        this.mappingResolver = resolver;
        this.namespaces = availableNamespaces;
        this.runtimeNamespace = runtime;
        this.preferredNamespace = resolvePreferredNamespace(runtime, availableNamespaces, hasBundledNamed);
        this.bundledIntermediaryToNamed = bundledMappings;
        this.runtimeNamedAvailable = hasRuntimeNamed;
        this.bundledNamedAvailable = hasBundledNamed;
        this.mappingMode = resolveMappingMode(runtime, hasRuntimeNamed, hasBundledNamed);
    }

    public String mapClassName(String rawClassName) {
        if (rawClassName == null || rawClassName.isBlank()) {
            return rawClassName;
        }

        return classNameCache.computeIfAbsent(rawClassName, this::remapToPreferredNamespace);
    }

    public String simpleName(String className) {
        if (className == null || className.isBlank()) {
            return "";
        }

        int dotIndex = className.lastIndexOf('.');
        String shortName = dotIndex >= 0 ? className.substring(dotIndex + 1) : className;
        int dollarIndex = shortName.lastIndexOf('$');
        if (dollarIndex >= 0 && dollarIndex + 1 < shortName.length()) {
            return shortName.substring(dollarIndex + 1);
        }

        return shortName;
    }

    public String getRuntimeNamespace() {
        return runtimeNamespace;
    }

    public String getPreferredNamespace() {
        return preferredNamespace;
    }

    public String getMappingMode() {
        return mappingMode;
    }

    public boolean hasRuntimeNamedMappings() {
        return runtimeNamedAvailable;
    }

    public boolean hasBundledNamedMappings() {
        return bundledNamedAvailable;
    }

    public Set<String> getNamespaces() {
        if (!bundledNamedAvailable || namespaces.contains(NAMED_NAMESPACE)) {
            return namespaces;
        }

        Set<String> merged = new LinkedHashSet<>(namespaces);
        merged.add(NAMED_NAMESPACE);
        return Collections.unmodifiableSet(merged);
    }

    public int getBundledNamedClassCount() {
        return bundledIntermediaryToNamed.size();
    }

    private String remapToPreferredNamespace(String rawClassName) {
        if (rawClassName.startsWith("[")) {
            return rawClassName;
        }

        if (NAMED_NAMESPACE.equals(preferredNamespace)) {
            return remapToNamed(rawClassName);
        }

        return remapToRuntimeTarget(rawClassName, preferredNamespace);
    }

    private String remapToNamed(String rawClassName) {
        if (runtimeNamedAvailable && mappingResolver != null) {
            try {
                String mapped = mappingResolver.unmapClassName(NAMED_NAMESPACE, rawClassName);
                if (mapped != null && !mapped.isBlank()) {
                    return mapped;
                }
            } catch (Exception ignored) {
                // Fall through to bundled map.
            }
        }

        if (bundledNamedAvailable) {
            String intermediaryName = toIntermediary(rawClassName);
            if (intermediaryName != null && !intermediaryName.isBlank()) {
                String mapped = bundledIntermediaryToNamed.get(intermediaryName);
                if (mapped != null && !mapped.isBlank()) {
                    return mapped;
                }
            }
        }

        return rawClassName;
    }

    private String toIntermediary(String className) {
        if (className == null || className.isBlank()) {
            return className;
        }

        if (INTERMEDIARY_NAMESPACE.equals(runtimeNamespace)) {
            return className;
        }

        if (mappingResolver == null) {
            return className;
        }

        try {
            String mapped = mappingResolver.unmapClassName(INTERMEDIARY_NAMESPACE, className);
            return mapped == null || mapped.isBlank() ? className : mapped;
        } catch (Exception ignored) {
            return className;
        }
    }

    private String remapToRuntimeTarget(String rawClassName, String targetNamespace) {
        if (mappingResolver == null) {
            return rawClassName;
        }

        if (targetNamespace == null || targetNamespace.isBlank() || targetNamespace.equals(runtimeNamespace)) {
            return rawClassName;
        }

        try {
            return mappingResolver.unmapClassName(targetNamespace, rawClassName);
        } catch (Exception ignored) {
            return rawClassName;
        }
    }

    private static String resolvePreferredNamespace(String runtimeNamespace, Collection<String> namespaces, boolean bundledNamedAvailable) {
        if (namespaces.contains(NAMED_NAMESPACE)) {
            return NAMED_NAMESPACE;
        }
        if (bundledNamedAvailable) {
            return NAMED_NAMESPACE;
        }
        if (namespaces.contains(INTERMEDIARY_NAMESPACE)) {
            return INTERMEDIARY_NAMESPACE;
        }
        if (runtimeNamespace != null && !runtimeNamespace.isBlank()) {
            return runtimeNamespace;
        }
        return UNKNOWN_NAMESPACE;
    }

    private static String resolveMappingMode(String runtimeNamespace, boolean runtimeNamedAvailable, boolean bundledNamedAvailable) {
        if (runtimeNamedAvailable) {
            return "runtime_named";
        }
        if (bundledNamedAvailable) {
            return "bundled_yarn_classes";
        }
        if (runtimeNamespace == null || runtimeNamespace.isBlank() || UNKNOWN_NAMESPACE.equals(runtimeNamespace)) {
            return "unavailable";
        }
        return "runtime_only";
    }

    private Map<String, String> loadBundledIntermediaryToNamed() {
        try (InputStream stream = NameMappingService.class.getResourceAsStream(BUNDLED_YARN_MAPPINGS_RESOURCE)) {
            if (stream == null) {
                return Map.of();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                if (header == null || !header.startsWith("tiny\t")) {
                    return Map.of();
                }

                String[] headerParts = header.split("\t", -1);
                if (headerParts.length < 5) {
                    return Map.of();
                }

                int namespaceStart = 3;
                int intermediaryColumn = findNamespaceColumn(headerParts, namespaceStart, INTERMEDIARY_NAMESPACE);
                int namedColumn = findNamespaceColumn(headerParts, namespaceStart, NAMED_NAMESPACE);
                if (intermediaryColumn < 0 || namedColumn < 0) {
                    return Map.of();
                }

                Map<String, String> mappings = new HashMap<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.charAt(0) == '#') {
                        continue;
                    }
                    if (!line.startsWith("c\t")) {
                        continue;
                    }

                    String[] parts = line.split("\t", -1);
                    if (parts.length <= Math.max(intermediaryColumn, namedColumn)) {
                        continue;
                    }

                    String intermediary = normalizeClassName(parts[intermediaryColumn]);
                    String named = normalizeClassName(parts[namedColumn]);
                    if (intermediary.isBlank() || named.isBlank()) {
                        continue;
                    }

                    mappings.putIfAbsent(intermediary, named);
                }

                return Map.copyOf(mappings);
            }
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private static int findNamespaceColumn(String[] headerParts, int namespaceStart, String namespace) {
        for (int i = namespaceStart; i < headerParts.length; i++) {
            if (namespace.equals(headerParts[i])) {
                return i;
            }
        }
        return -1;
    }

    private static String normalizeClassName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.replace('/', '.');
    }
}

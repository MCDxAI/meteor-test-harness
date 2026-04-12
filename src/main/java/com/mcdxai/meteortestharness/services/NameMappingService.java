package com.mcdxai.meteortestharness.services;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.fabricmc.loader.api.ModContainer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class NameMappingService {
    private static final String NAMED_NAMESPACE = "named";
    private static final String INTERMEDIARY_NAMESPACE = "intermediary";
    private static final String UNKNOWN_NAMESPACE = "unknown";

    private static final String PRIMARY_MAPPINGS_RESOURCE = "mappings/yarn.tiny";
    private static final String LEGACY_MAPPINGS_RESOURCE = "mappings/mappings.tiny";
    private static final String MOD_ID = "meteor-test-harness";

    private final MappingResolver mappingResolver;
    private final Set<String> namespaces;
    private final String runtimeNamespace;
    private final String preferredNamespace;
    private final Map<String, String> bundledIntermediaryToNamed;
    private final boolean runtimeNamedAvailable;
    private final boolean bundledNamedAvailable;
    private final String mappingMode;
    private final String bundledMappingsSource;
    private final String bundledMappingsError;
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
            // Leave mapping resolver unavailable when loader API is not ready.
        }

        BundledMappingLoadResult bundledResult = loadBundledIntermediaryToNamed();
        Map<String, String> bundledMappings = bundledResult.mappings;
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
        this.bundledMappingsSource = bundledResult.source;
        this.bundledMappingsError = bundledResult.error;
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

    public String getBundledMappingsSource() {
        return bundledMappingsSource;
    }

    public String getBundledMappingsError() {
        return bundledMappingsError;
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

    private BundledMappingLoadResult loadBundledIntermediaryToNamed() {
        List<String> attempts = new ArrayList<>();

        try {
            OpenedStream openedStream = openBundledMappingsStream(attempts);
            if (openedStream == null || openedStream.stream == null) {
                String details = attempts.isEmpty() ? "none" : String.join(" | ", attempts);
                return BundledMappingLoadResult.failure("not_found", details);
            }

            try (InputStream stream = openedStream.stream;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                if (header == null) {
                    return BundledMappingLoadResult.failure(openedStream.source, "empty_file");
                }

                header = stripBom(header).trim();
                if (!header.startsWith("tiny\t")) {
                    return BundledMappingLoadResult.failure(openedStream.source, "invalid_header:" + header);
                }

                String[] headerParts = header.split("\t", -1);
                if (headerParts.length < 5) {
                    return BundledMappingLoadResult.failure(openedStream.source, "header_too_short:" + headerParts.length);
                }

                int namespaceStart = 3;
                int intermediaryColumn = findNamespaceLineColumn(headerParts, namespaceStart, INTERMEDIARY_NAMESPACE);
                int namedColumn = findNamespaceLineColumn(headerParts, namespaceStart, NAMED_NAMESPACE);
                if (intermediaryColumn < 0 || namedColumn < 0) {
                    return BundledMappingLoadResult.failure(openedStream.source, "missing_namespaces:intermediary_or_named");
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

                if (mappings.isEmpty()) {
                    return BundledMappingLoadResult.failure(openedStream.source, "no_class_entries_parsed");
                }

                return BundledMappingLoadResult.success(Map.copyOf(mappings), openedStream.source);
            }
        } catch (Exception e) {
            String details = attempts.isEmpty() ? "none" : String.join(" | ", attempts);
            return BundledMappingLoadResult.failure("exception", e.getClass().getSimpleName() + ":" + e.getMessage() + " attempts=" + details);
        }
    }

    private OpenedStream openBundledMappingsStream(List<String> attempts) {
        OpenedStream stream;

        stream = tryClassResource("/" + PRIMARY_MAPPINGS_RESOURCE);
        if (stream != null) {
            return stream;
        }
        attempts.add("class:/" + PRIMARY_MAPPINGS_RESOURCE + "=missing");

        stream = tryClassLoaderResource(PRIMARY_MAPPINGS_RESOURCE);
        if (stream != null) {
            return stream;
        }
        attempts.add("classloader:" + PRIMARY_MAPPINGS_RESOURCE + "=missing");

        stream = tryClassResource("/" + LEGACY_MAPPINGS_RESOURCE);
        if (stream != null) {
            return stream;
        }
        attempts.add("class:/" + LEGACY_MAPPINGS_RESOURCE + "=missing");

        stream = tryClassLoaderResource(LEGACY_MAPPINGS_RESOURCE);
        if (stream != null) {
            return stream;
        }
        attempts.add("classloader:" + LEGACY_MAPPINGS_RESOURCE + "=missing");

        stream = tryModContainerPath(PRIMARY_MAPPINGS_RESOURCE, attempts);
        if (stream != null) {
            return stream;
        }

        stream = tryModContainerPath(LEGACY_MAPPINGS_RESOURCE, attempts);
        if (stream != null) {
            return stream;
        }

        return null;
    }

    private OpenedStream tryClassResource(String path) {
        InputStream stream = NameMappingService.class.getResourceAsStream(path);
        if (stream == null) {
            return null;
        }
        return new OpenedStream(stream, "class:" + path);
    }

    private OpenedStream tryClassLoaderResource(String path) {
        ClassLoader classLoader = NameMappingService.class.getClassLoader();
        if (classLoader == null) {
            return null;
        }

        InputStream stream = classLoader.getResourceAsStream(path);
        if (stream == null) {
            return null;
        }
        return new OpenedStream(stream, "classloader:" + path);
    }

    private OpenedStream tryModContainerPath(String relativePath, List<String> attempts) {
        try {
            Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(MOD_ID);
            if (container.isEmpty()) {
                attempts.add("mod_container:" + MOD_ID + "=missing");
                return null;
            }

            Optional<Path> path = container.get().findPath(relativePath);
            if (path.isEmpty()) {
                attempts.add("mod_path:" + relativePath + "=missing");
                return null;
            }

            if (!Files.exists(path.get())) {
                attempts.add("mod_path:" + path.get() + "=not_exists");
                return null;
            }

            return new OpenedStream(Files.newInputStream(path.get()), "mod:" + path.get());
        } catch (Exception e) {
            attempts.add("mod_lookup_error:" + e.getClass().getSimpleName());
            return null;
        }
    }

    private static int findNamespaceLineColumn(String[] headerParts, int namespaceStart, String namespace) {
        for (int i = namespaceStart; i < headerParts.length; i++) {
            if (namespace.equals(stripBom(headerParts[i]).trim())) {
                return 1 + (i - namespaceStart);
            }
        }
        return -1;
    }

    private static String normalizeClassName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return stripBom(name).replace('/', '.').trim();
    }

    private static String stripBom(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    private static final class OpenedStream {
        private final InputStream stream;
        private final String source;

        private OpenedStream(InputStream stream, String source) {
            this.stream = stream;
            this.source = source;
        }
    }

    private static final class BundledMappingLoadResult {
        private final Map<String, String> mappings;
        private final String source;
        private final String error;

        private BundledMappingLoadResult(Map<String, String> mappings, String source, String error) {
            this.mappings = mappings;
            this.source = source;
            this.error = error;
        }

        private static BundledMappingLoadResult success(Map<String, String> mappings, String source) {
            return new BundledMappingLoadResult(mappings, source, null);
        }

        private static BundledMappingLoadResult failure(String source, String error) {
            return new BundledMappingLoadResult(Map.of(), source, error);
        }
    }
}

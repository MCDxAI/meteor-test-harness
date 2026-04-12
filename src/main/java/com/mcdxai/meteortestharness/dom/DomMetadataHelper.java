package com.mcdxai.meteortestharness.dom;

import com.mcdxai.meteortestharness.services.NameMappingService;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DomMetadataHelper {
    private final NameMappingService nameMappingService;

    public DomMetadataHelper(NameMappingService nameMappingService) {
        this.nameMappingService = nameMappingService;
    }

    public Map<String, Object> classMetadata(Class<?> type) {
        String className = type.getName();
        String mappedClassName = nameMappingService.mapClassName(className);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("class", className);
        metadata.put("classMapped", mappedClassName);
        metadata.put("type", nameMappingService.simpleName(className));
        metadata.put("typeMapped", nameMappingService.simpleName(mappedClassName));
        return metadata;
    }

    public void putClassMetadata(Map<String, Object> target, Class<?> type) {
        target.putAll(classMetadata(type));
    }

    public Object findFieldValueByType(Object target, String fieldTypeName) {
        if (target == null || fieldTypeName == null || fieldTypeName.isBlank()) {
            return null;
        }

        Class<?> type = target.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.getType().getName().equals(fieldTypeName)) continue;

                try {
                    field.setAccessible(true);
                    return field.get(target);
                } catch (Exception ignored) {
                    // Keep scanning.
                }
            }
            type = type.getSuperclass();
        }

        return null;
    }

    public Object findFieldValue(Object target, String fieldName) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }

        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }
}

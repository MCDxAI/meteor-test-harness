package io.mcdxai.harness.mcp;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

public final class ToolSchemas {
    private ToolSchemas() {
    }

    public static McpSchema.JsonSchema emptyObject() {
        return object(Map.of(), List.of());
    }

    public static McpSchema.JsonSchema object(Map<String, Object> properties, List<String> required) {
        return new McpSchema.JsonSchema("object", properties, required, false, null, null);
    }

    public static Map<String, Object> stringProperty(String description) {
        return Map.of("type", "string", "description", description);
    }

    public static Map<String, Object> boolProperty(String description) {
        return Map.of("type", "boolean", "description", description);
    }

    public static Map<String, Object> intProperty(String description) {
        return Map.of("type", "integer", "description", description);
    }

    public static Map<String, Object> numberProperty(String description) {
        return Map.of("type", "number", "description", description);
    }

    public static Map<String, Object> objectProperty(String description) {
        return Map.of("type", "object", "description", description);
    }

    public static Map<String, Object> arrayProperty(String description) {
        return Map.of("type", "array", "description", description);
    }
}
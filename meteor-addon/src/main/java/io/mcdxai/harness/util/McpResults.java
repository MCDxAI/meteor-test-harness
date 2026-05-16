package io.mcdxai.harness.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import java.util.Map;

public final class McpResults {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private McpResults() {
    }

    public static CallToolResult ok(String text) {
        return CallToolResult.builder().addTextContent(text).isError(false).build();
    }

    public static CallToolResult ok(String text, Object structuredContent) {
        return CallToolResult.builder()
            .addTextContent(text)
            .addTextContent(toJson(structuredContent))
            .structuredContent(structuredContent)
            .isError(false)
            .build();
    }

    public static CallToolResult ok(Object structuredContent) {
        return CallToolResult.builder()
            .addTextContent(toJson(structuredContent))
            .structuredContent(structuredContent)
            .isError(false)
            .build();
    }

    public static CallToolResult error(String message) {
        return CallToolResult.builder().addTextContent(message).isError(true).build();
    }

    public static CallToolResult error(String message, Map<String, Object> details) {
        return CallToolResult.builder()
            .addTextContent(message)
            .addTextContent(toJson(details))
            .structuredContent(details)
            .isError(true)
            .build();
    }

    private static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
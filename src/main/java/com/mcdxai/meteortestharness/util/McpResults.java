package com.mcdxai.meteortestharness.util;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import java.util.Map;

public final class McpResults {
    private McpResults() {
    }

    public static CallToolResult ok(String text) {
        return CallToolResult.builder().addTextContent(text).isError(false).build();
    }

    public static CallToolResult ok(String text, Object structuredContent) {
        return CallToolResult.builder()
            .addTextContent(text)
            .structuredContent(structuredContent)
            .isError(false)
            .build();
    }

    public static CallToolResult ok(Object structuredContent) {
        return CallToolResult.builder()
            .addTextContent("ok")
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
            .structuredContent(details)
            .isError(true)
            .build();
    }
}
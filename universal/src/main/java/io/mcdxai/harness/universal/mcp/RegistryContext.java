package io.mcdxai.harness.universal.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mcdxai.harness.universal.adapter.AdapterRegistry;
import io.mcdxai.harness.universal.config.HarnessConfig;
import io.mcdxai.harness.universal.services.GameStateService;
import io.mcdxai.harness.universal.services.HarnessService;
import io.mcdxai.harness.universal.services.ScreenDomService;
import io.mcdxai.harness.universal.util.ArgReader;
import io.mcdxai.harness.universal.util.MainThreadInvoker;
import io.mcdxai.harness.universal.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class RegistryContext {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HarnessConfig config;
    private final SessionGate sessionGate;
    private final AdapterRegistry adapterRegistry;
    private final ScreenDomService screenDomService;
    private final GameStateService gameStateService;
    private final HarnessService harnessService;

    RegistryContext(HarnessConfig config, SessionGate sessionGate, AdapterRegistry adapterRegistry) {
        this.config = config;
        this.sessionGate = sessionGate;
        this.adapterRegistry = adapterRegistry;
        this.screenDomService = new ScreenDomService(adapterRegistry);
        this.gameStateService = new GameStateService();
        this.harnessService = new HarnessService(config, sessionGate, adapterRegistry);
    }

    public SessionGate sessionGate() {
        return sessionGate;
    }

    public AdapterRegistry adapterRegistry() {
        return adapterRegistry;
    }

    public ScreenDomService screenDomService() {
        return screenDomService;
    }

    public GameStateService gameStateService() {
        return gameStateService;
    }

    public HarnessService harnessService() {
        return harnessService;
    }

    public McpServerFeatures.SyncToolSpecification tool(String name, String description, McpSchema.JsonSchema schema, ToolHandler handler) {
        return tool(name, description, schema, true, handler);
    }

    public McpServerFeatures.SyncToolSpecification tool(String name, String description, McpSchema.JsonSchema schema, boolean runOnMainThread, ToolHandler handler) {
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder().name(name).description(description).inputSchema(schema).build())
            .callHandler((exchange, request) -> handleCall(exchange, request, runOnMainThread, handler))
            .build();
    }

    McpSchema.CallToolResult handleCall(McpSyncServerExchange exchange, McpSchema.CallToolRequest request, boolean runOnMainThread, ToolHandler handler) {
        String sessionId = exchange.sessionId();
        if (!sessionGate.claimOrValidate(sessionId, config.singleSessionMode)) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("requestSession", sessionId);
            details.put("ownerSession", sessionGate.ownerSessionId());
            details.put("singleSessionMode", true);
            return McpResults.error("Rejected: single-session mode is enabled and another session owns the harness.", details);
        }

        try {
            Duration timeout = Duration.ofSeconds(config.requestTimeoutSeconds);
            ArgReader args = new ArgReader(request.arguments());
            if (runOnMainThread) {
                return MainThreadInvoker.call(() -> handler.handle(exchange, args), timeout);
            }
            return handler.handle(exchange, args);
        } catch (Exception e) {
            return McpResults.error("Tool call failed: " + e.getMessage());
        }
    }

    public int requestTimeoutMillis() {
        return config.requestTimeoutSeconds * 1000;
    }

    public McpServerFeatures.SyncResourceSpecification resource(String uri, String name, String description, Supplier<Object> supplier) {
        return new McpServerFeatures.SyncResourceSpecification(
            McpSchema.Resource.builder()
                .uri(uri)
                .name(name)
                .description(description)
                .mimeType("application/json")
                .build(),
            (exchange, request) -> readJsonResource(request.uri(), supplier)
        );
    }

    McpSchema.ReadResourceResult readJsonResource(String uri, Supplier<Object> supplier) {
        String json;
        try {
            Duration timeout = Duration.ofSeconds(config.requestTimeoutSeconds);
            Object data = MainThreadInvoker.call(supplier::get, timeout);
            json = toJson(data);
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            json = toJson(Map.of("error", "Resource read failed.", "message", message));
        }
        return new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents(uri, "application/json", json)));
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @FunctionalInterface
    public interface ToolHandler {
        McpSchema.CallToolResult handle(McpSyncServerExchange exchange, ArgReader args) throws Exception;
    }
}

package io.mcdxai.harness.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mcdxai.harness.config.HarnessConfig;
import io.mcdxai.harness.services.ChatLogService;
import io.mcdxai.harness.services.GameStateService;
import io.mcdxai.harness.services.HarnessService;
import io.mcdxai.harness.services.MeteorInfoService;
import io.mcdxai.harness.services.ModuleService;
import io.mcdxai.harness.services.NameMappingService;
import io.mcdxai.harness.services.PathingService;
import io.mcdxai.harness.services.ScreenDomService;
import io.mcdxai.harness.services.SettingValueCodec;
import io.mcdxai.harness.util.ArgReader;
import io.mcdxai.harness.util.MainThreadInvoker;
import io.mcdxai.harness.util.McpResults;
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
    private final SettingValueCodec settingValueCodec;
    private final ModuleService moduleService;
    private final GameStateService gameStateService;
    private final ScreenDomService screenDomService;
    private final PathingService pathingService;
    private final ChatLogService chatLogService;
    private final HarnessService harnessService;
    private final MeteorInfoService meteorInfoService;

    RegistryContext(HarnessConfig config, SessionGate sessionGate, ChatLogService chatLogService) {
        this.config = config;
        this.sessionGate = sessionGate;
        this.chatLogService = chatLogService;

        NameMappingService nameMappingService = new NameMappingService();
        this.settingValueCodec = new SettingValueCodec();
        this.moduleService = new ModuleService(settingValueCodec);
        this.gameStateService = new GameStateService();
        this.screenDomService = new ScreenDomService(nameMappingService);
        this.pathingService = new PathingService();
        this.harnessService = new HarnessService(config, sessionGate, nameMappingService);
        this.meteorInfoService = new MeteorInfoService();
    }

    public SessionGate sessionGate() {
        return sessionGate;
    }

    public SettingValueCodec settingValueCodec() {
        return settingValueCodec;
    }

    public ModuleService moduleService() {
        return moduleService;
    }

    public GameStateService gameStateService() {
        return gameStateService;
    }

    public ScreenDomService screenDomService() {
        return screenDomService;
    }

    public PathingService pathingService() {
        return pathingService;
    }

    public ChatLogService chatLogService() {
        return chatLogService;
    }

    public HarnessService harnessService() {
        return harnessService;
    }

    public MeteorInfoService meteorInfoService() {
        return meteorInfoService;
    }

    public McpServerFeatures.SyncToolSpecification tool(
        String name,
        String description,
        McpSchema.JsonSchema schema,
        ToolHandler handler
    ) {
        return tool(name, description, schema, true, handler);
    }

    public McpServerFeatures.SyncToolSpecification tool(
        String name,
        String description,
        McpSchema.JsonSchema schema,
        boolean runOnMainThread,
        ToolHandler handler
    ) {
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder().name(name).description(description).inputSchema(schema).build())
            .callHandler((exchange, request) -> handleCall(exchange, request, runOnMainThread, handler))
            .build();
    }

    McpSchema.CallToolResult handleCall(
        McpSyncServerExchange exchange,
        McpSchema.CallToolRequest request,
        boolean runOnMainThread,
        ToolHandler handler
    ) {
        String sessionId = exchange.sessionId();
        if (!sessionGate.claimOrValidate(sessionId, config.singleSessionMode.get())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("requestSession", sessionId);
            details.put("ownerSession", sessionGate.ownerSessionId());
            details.put("singleSessionMode", true);
            return McpResults.error("Rejected: single-session mode is enabled and another session owns the harness.", details);
        }

        try {
            Duration timeout = Duration.ofSeconds(config.requestTimeoutSeconds.get());
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
        return config.requestTimeoutSeconds.get() * 1000;
    }

    public McpServerFeatures.SyncResourceSpecification resource(
        String uri,
        String name,
        String description,
        Supplier<Object> supplier
    ) {
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
            Duration timeout = Duration.ofSeconds(config.requestTimeoutSeconds.get());
            Object data = MainThreadInvoker.call(supplier::get, timeout);
            json = toJson(data);
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            json = toJson(Map.of("error", "Resource read failed.", "message", message));
        }

        return new McpSchema.ReadResourceResult(List.of(
            new McpSchema.TextResourceContents(uri, "application/json", json)
        ));
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

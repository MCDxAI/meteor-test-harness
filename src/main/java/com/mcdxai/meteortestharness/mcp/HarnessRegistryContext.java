package com.mcdxai.meteortestharness.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcdxai.meteortestharness.config.HarnessConfig;
import com.mcdxai.meteortestharness.services.ChatLogService;
import com.mcdxai.meteortestharness.services.GameStateService;
import com.mcdxai.meteortestharness.services.ModuleService;
import com.mcdxai.meteortestharness.services.NameMappingService;
import com.mcdxai.meteortestharness.services.PathingService;
import com.mcdxai.meteortestharness.services.ScreenDomService;
import com.mcdxai.meteortestharness.services.SettingValueCodec;
import com.mcdxai.meteortestharness.util.ArgReader;
import com.mcdxai.meteortestharness.util.MainThreadInvoker;
import com.mcdxai.meteortestharness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

final class HarnessRegistryContext {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HarnessConfig config;
    private final SessionGate sessionGate;
    private final SettingValueCodec settingValueCodec;
    private final NameMappingService nameMappingService;
    private final ModuleService moduleService;
    private final GameStateService gameStateService;
    private final ScreenDomService screenDomService;
    private final PathingService pathingService;
    private final ChatLogService chatLogService;

    HarnessRegistryContext(HarnessConfig config, SessionGate sessionGate, ChatLogService chatLogService) {
        this.config = config;
        this.sessionGate = sessionGate;
        this.chatLogService = chatLogService;

        this.settingValueCodec = new SettingValueCodec();
        this.nameMappingService = new NameMappingService();
        this.moduleService = new ModuleService(settingValueCodec);
        this.gameStateService = new GameStateService();
        this.screenDomService = new ScreenDomService(nameMappingService);
        this.pathingService = new PathingService();
    }

    SessionGate sessionGate() {
        return sessionGate;
    }

    SettingValueCodec settingValueCodec() {
        return settingValueCodec;
    }

    ModuleService moduleService() {
        return moduleService;
    }

    GameStateService gameStateService() {
        return gameStateService;
    }

    ScreenDomService screenDomService() {
        return screenDomService;
    }

    PathingService pathingService() {
        return pathingService;
    }

    ChatLogService chatLogService() {
        return chatLogService;
    }

    McpServerFeatures.SyncToolSpecification tool(
        String name,
        String description,
        McpSchema.JsonSchema schema,
        ToolHandler handler
    ) {
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(McpSchema.Tool.builder().name(name).description(description).inputSchema(schema).build())
            .callHandler((exchange, request) -> handleCall(exchange, request, handler))
            .build();
    }

    McpSchema.CallToolResult handleCall(
        McpSyncServerExchange exchange,
        McpSchema.CallToolRequest request,
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
            return MainThreadInvoker.call(() -> handler.handle(exchange, new ArgReader(request.arguments())), timeout);
        } catch (Exception e) {
            return McpResults.error("Tool call failed: " + e.getMessage());
        }
    }

    McpServerFeatures.SyncResourceSpecification resource(
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

    Map<String, Object> withInteraction(Map<String, Object> interaction) {
        Map<String, Object> dom = new LinkedHashMap<>(screenDomService.snapshot());
        dom.put("interaction", interaction);
        dom.put("success", interactionSuccess(interaction));
        return dom;
    }

    Map<String, Object> domErrorDetails(Map<String, Object> interaction) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("interaction", interaction);
        details.put("dom", screenDomService.snapshot());
        return details;
    }

    boolean interactionSuccess(Map<String, Object> interaction) {
        Object value = interaction.get("success");
        return value instanceof Boolean booleanValue && booleanValue;
    }

    boolean resultOk(Map<String, Object> result) {
        Object value = result.get("success");
        return value instanceof Boolean booleanValue && booleanValue;
    }

    Map<String, Object> harnessStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ownerSession", sessionGate.ownerSessionId());
        status.put("singleSessionMode", config.singleSessionMode.get());
        status.put("bindHost", config.bindHost.get());
        status.put("bindPort", config.bindPort.get());
        status.put("mcpEndpoint", config.mcpEndpoint.get());
        status.put("inWorld", mc.world != null);
        status.put("hasPlayer", mc.player != null);

        Screen currentScreen = mc.currentScreen;
        if (currentScreen == null) {
            status.put("currentScreen", null);
            status.put("currentScreenMapped", null);
            status.put("currentScreenType", null);
            status.put("currentScreenTypeMapped", null);
        } else {
            String rawClass = currentScreen.getClass().getName();
            String mappedClass = nameMappingService.mapClassName(rawClass);
            status.put("currentScreen", rawClass);
            status.put("currentScreenMapped", mappedClass);
            status.put("currentScreenType", nameMappingService.simpleName(rawClass));
            status.put("currentScreenTypeMapped", nameMappingService.simpleName(mappedClass));
        }

        return status;
    }

    Map<String, Object> harnessDebugInfo() {
        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("mappingRuntimeNamespace", nameMappingService.getRuntimeNamespace());
        debug.put("mappingPreferredNamespace", nameMappingService.getPreferredNamespace());
        debug.put("mappingMode", nameMappingService.getMappingMode());
        debug.put("mappingNamespaces", nameMappingService.getNamespaces());
        debug.put("mappingRuntimeNamedAvailable", nameMappingService.hasRuntimeNamedMappings());
        debug.put("mappingBundledNamedAvailable", nameMappingService.hasBundledNamedMappings());
        debug.put("mappingBundledNamedClassCount", nameMappingService.getBundledNamedClassCount());
        debug.put("mappingBundledSource", nameMappingService.getBundledMappingsSource());
        debug.put("mappingBundledError", nameMappingService.getBundledMappingsError());
        debug.put("inputKeySimulationMode", "screen_and_global");
        debug.put("inputKeyDispatchMode", "keyboard_onKey");

        Screen currentScreen = mc.currentScreen;
        if (currentScreen == null) {
            debug.put("currentScreen", null);
            debug.put("currentScreenMapped", null);
            debug.put("currentScreenType", null);
            debug.put("currentScreenTypeMapped", null);
        } else {
            String rawClass = currentScreen.getClass().getName();
            String mappedClass = nameMappingService.mapClassName(rawClass);
            debug.put("currentScreen", rawClass);
            debug.put("currentScreenMapped", mappedClass);
            debug.put("currentScreenType", nameMappingService.simpleName(rawClass));
            debug.put("currentScreenTypeMapped", nameMappingService.simpleName(mappedClass));
        }

        return debug;
    }

    void disconnectToTitle() {
        ClientWorld world = mc.world;
        if (world != null) {
            try {
                world.disconnect(Text.literal("Disconnected by meteor-test-harness"));
            } catch (Exception ignored) {
                // Fall through to other strategies.
            }
        }

        invokeDisconnect();
    }

    private void invokeDisconnect() {
        mc.disconnect(new TitleScreen(), false, false);
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @FunctionalInterface
    interface ToolHandler {
        McpSchema.CallToolResult handle(McpSyncServerExchange exchange, ArgReader args) throws Exception;
    }
}

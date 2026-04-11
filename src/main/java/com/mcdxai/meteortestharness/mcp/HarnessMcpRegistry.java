package com.mcdxai.meteortestharness.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcdxai.meteortestharness.config.HarnessConfig;
import com.mcdxai.meteortestharness.services.ChatLogService;
import com.mcdxai.meteortestharness.services.GameStateService;
import com.mcdxai.meteortestharness.services.ModuleService;
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
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class HarnessMcpRegistry {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HarnessConfig config;
    private final SessionGate sessionGate;

    private final SettingValueCodec settingValueCodec;
    private final ModuleService moduleService;
    private final GameStateService gameStateService;
    private final ScreenDomService screenDomService;
    private final PathingService pathingService;
    private final ChatLogService chatLogService;

    public HarnessMcpRegistry(HarnessConfig config, SessionGate sessionGate, ChatLogService chatLogService) {
        this.config = config;
        this.sessionGate = sessionGate;

        this.settingValueCodec = new SettingValueCodec();
        this.moduleService = new ModuleService(settingValueCodec);
        this.gameStateService = new GameStateService();
        this.screenDomService = new ScreenDomService();
        this.pathingService = new PathingService();
        this.chatLogService = chatLogService;
    }

    public List<McpServerFeatures.SyncToolSpecification> tools() {
        List<McpServerFeatures.SyncToolSpecification> tools = new ArrayList<>();

        tools.add(tool("get_harness_status", "Get harness runtime/session status.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(harnessStatus())));

        tools.add(tool("release_session", "Release current session ownership lock.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                sessionGate.release(exchange.sessionId());
                return McpResults.ok("Session ownership released.", harnessStatus());
            }));

        tools.add(tool(
            "list_modules",
            "List all Meteor and addon modules.",
            ToolSchemas.object(
                Map.of("include_settings", ToolSchemas.boolProperty("Include each module's full settings tree.")),
                List.of()
            ),
            (exchange, args) -> McpResults.ok(moduleService.listModules(args.bool("include_settings", false)))
        ));

        tools.add(tool(
            "get_module",
            "Get one module and optionally its settings.",
            ToolSchemas.object(
                Map.of(
                    "module_name", ToolSchemas.stringProperty("Module name/title."),
                    "include_settings", ToolSchemas.boolProperty("Include settings tree.")
                ),
                List.of("module_name")
            ),
            (exchange, args) -> {
                Optional<meteordevelopment.meteorclient.systems.modules.Module> module = moduleService.findModule(args.string("module_name"));
                if (module.isEmpty()) return McpResults.error("Module not found.");
                return McpResults.ok(moduleService.describeModule(module.get(), args.bool("include_settings", true)));
            }
        ));

        tools.add(tool(
            "set_module_state",
            "Enable or disable a module.",
            ToolSchemas.object(
                Map.of(
                    "module_name", ToolSchemas.stringProperty("Module name/title."),
                    "active", ToolSchemas.boolProperty("Desired active state.")
                ),
                List.of("module_name", "active")
            ),
            (exchange, args) -> {
                Optional<meteordevelopment.meteorclient.systems.modules.Module> module = moduleService.findModule(args.string("module_name"));
                if (module.isEmpty()) return McpResults.error("Module not found.");

                boolean success = moduleService.setModuleState(module.get(), args.bool("active", false));
                if (!success) return McpResults.error("Failed to set module state.");

                return McpResults.ok(moduleService.describeModule(module.get(), false));
            }
        ));

        tools.add(tool(
            "list_module_settings",
            "List settings for a module.",
            ToolSchemas.object(
                Map.of("module_name", ToolSchemas.stringProperty("Module name/title.")),
                List.of("module_name")
            ),
            (exchange, args) -> {
                Optional<meteordevelopment.meteorclient.systems.modules.Module> module = moduleService.findModule(args.string("module_name"));
                if (module.isEmpty()) return McpResults.error("Module not found.");
                return McpResults.ok(moduleService.listModuleSettings(module.get()));
            }
        ));

        tools.add(tool(
            "get_module_setting",
            "Get one setting from a module.",
            ToolSchemas.object(
                Map.of(
                    "module_name", ToolSchemas.stringProperty("Module name/title."),
                    "setting_name", ToolSchemas.stringProperty("Setting name/title.")
                ),
                List.of("module_name", "setting_name")
            ),
            (exchange, args) -> {
                Optional<meteordevelopment.meteorclient.systems.modules.Module> module = moduleService.findModule(args.string("module_name"));
                if (module.isEmpty()) return McpResults.error("Module not found.");

                Optional<meteordevelopment.meteorclient.settings.Setting<?>> setting = moduleService.findSetting(module.get(), args.string("setting_name"));
                if (setting.isEmpty()) return McpResults.error("Setting not found.");

                return McpResults.ok(settingValueCodec.describeSetting(setting.get()));
            }
        ));

        tools.add(tool(
            "set_module_setting",
            "Set one module setting value.",
            ToolSchemas.object(
                Map.of(
                    "module_name", ToolSchemas.stringProperty("Module name/title."),
                    "setting_name", ToolSchemas.stringProperty("Setting name/title."),
                    "value", Map.of("description", "New value. Scalars/maps/lists supported depending on setting type.")
                ),
                List.of("module_name", "setting_name", "value")
            ),
            (exchange, args) -> {
                Optional<meteordevelopment.meteorclient.systems.modules.Module> module = moduleService.findModule(args.string("module_name"));
                if (module.isEmpty()) return McpResults.error("Module not found.");

                Optional<meteordevelopment.meteorclient.settings.Setting<?>> setting = moduleService.findSetting(module.get(), args.string("setting_name"));
                if (setting.isEmpty()) return McpResults.error("Setting not found.");

                Object value = args.raw("value");
                boolean success = moduleService.setSetting(setting.get(), value);
                if (!success) return McpResults.error("Failed to apply setting value.");

                return McpResults.ok(settingValueCodec.describeSetting(setting.get()));
            }
        ));

        tools.add(tool("get_player_state", "Get current player state stream.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(gameStateService.getPlayerState())));

        tools.add(tool("get_world_state", "Get current world state stream.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(gameStateService.getWorldState())));

        tools.add(tool("get_inventory_state", "Get current inventory state stream.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(gameStateService.getInventoryState())));

        tools.add(tool(
            "get_nearby_entities",
            "Get nearby entities around the player.",
            ToolSchemas.object(
                Map.of(
                    "radius", ToolSchemas.numberProperty("Search radius in blocks. Default 32."),
                    "max_count", ToolSchemas.intProperty("Maximum entities to return. Default 64.")
                ),
                List.of()
            ),
            (exchange, args) -> McpResults.ok(
                gameStateService.getNearbyEntities(
                    args.doubleValue("radius", 32D),
                    args.intValue("max_count", 64)
                )
            )
        ));

        tools.add(tool("get_screen_dom", "Get current DOM tree for active screen.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(screenDomService.snapshot())));

        tools.add(tool(
            "click_dom_element",
            "Click a DOM element by id.",
            ToolSchemas.object(Map.of("element_id", ToolSchemas.stringProperty("Element id from get_screen_dom.")), List.of("element_id")),
            (exchange, args) -> {
                boolean success = screenDomService.click(args.string("element_id"));
                if (!success) return McpResults.error("Element not found or not clickable.");
                return McpResults.ok(screenDomService.snapshot());
            }
        ));

        tools.add(tool(
            "set_dom_text",
            "Set text content on a DOM text input by id.",
            ToolSchemas.object(
                Map.of(
                    "element_id", ToolSchemas.stringProperty("Element id from get_screen_dom."),
                    "text", ToolSchemas.stringProperty("Text to set.")
                ),
                List.of("element_id", "text")
            ),
            (exchange, args) -> {
                boolean success = screenDomService.setText(args.string("element_id"), args.string("text", ""));
                if (!success) return McpResults.error("Element does not accept text input.");
                return McpResults.ok(screenDomService.snapshot());
            }
        ));

        tools.add(tool(
            "set_dom_value",
            "Set value on a DOM control (checkbox/slider) by id.",
            ToolSchemas.object(
                Map.of(
                    "element_id", ToolSchemas.stringProperty("Element id from get_screen_dom."),
                    "value", ToolSchemas.objectProperty("Value payload.")
                ),
                List.of("element_id", "value")
            ),
            (exchange, args) -> {
                boolean success = screenDomService.setValue(args.string("element_id"), args.raw("value"));
                if (!success) return McpResults.error("Element does not support set_value.");
                return McpResults.ok(screenDomService.snapshot());
            }
        ));

        tools.add(tool("navigate_back", "Close current screen/go back.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(Map.of("success", screenDomService.navigateBack()))));

        tools.add(tool(
            "send_chat",
            "Send chat message as player.",
            ToolSchemas.object(Map.of("message", ToolSchemas.stringProperty("Chat message text.")), List.of("message")),
            (exchange, args) -> {
                ClientPlayerEntity player = mc.player;
                if (player == null) return McpResults.error("No local player.");

                ClientPlayNetworkHandler networkHandler = player.networkHandler;
                if (networkHandler == null) return McpResults.error("Network handler unavailable.");

                String message = args.string("message", "");
                if (message.isBlank()) return McpResults.error("Message cannot be empty.");

                networkHandler.sendChatMessage(message);
                return McpResults.ok("Message sent.");
            }
        ));

        tools.add(tool(
            "send_command",
            "Send command as player.",
            ToolSchemas.object(Map.of("command", ToolSchemas.stringProperty("Command with or without leading slash.")), List.of("command")),
            (exchange, args) -> {
                ClientPlayerEntity player = mc.player;
                if (player == null) return McpResults.error("No local player.");

                ClientPlayNetworkHandler networkHandler = player.networkHandler;
                if (networkHandler == null) return McpResults.error("Network handler unavailable.");

                String command = args.string("command", "").trim();
                if (command.isEmpty()) return McpResults.error("Command cannot be empty.");
                if (command.startsWith("/")) command = command.substring(1);
                if (command.isEmpty()) return McpResults.error("Command cannot be empty.");

                networkHandler.sendChatCommand(command);
                return McpResults.ok("Command sent.");
            }
        ));

        tools.add(tool("disconnect_world", "Disconnect from current world/server.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                disconnectToTitle();
                return McpResults.ok(Map.of("inWorld", mc.world != null));
            }
        ));

        tools.add(tool(
            "get_chat_history",
            "Get captured chat history.",
            ToolSchemas.object(Map.of("count", ToolSchemas.intProperty("Number of newest lines to return.")), List.of()),
            (exchange, args) -> McpResults.ok(chatLogService.snapshot(args.intValue("count", 100)))
        ));

        tools.add(tool("clear_chat_history", "Clear captured chat history buffer.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                chatLogService.clear();
                return McpResults.ok("Chat history cleared.");
            }
        ));

        tools.add(tool("get_pathing_status", "Get Baritone/PathManager status.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(pathingService.getStatus())));

        tools.add(tool(
            "pathing_move_to",
            "Move player to target coordinates using PathManager/Baritone.",
            ToolSchemas.object(
                Map.of(
                    "x", ToolSchemas.intProperty("Target block X."),
                    "y", ToolSchemas.intProperty("Target block Y."),
                    "z", ToolSchemas.intProperty("Target block Z."),
                    "ignore_y", ToolSchemas.boolProperty("Ignore Y and path in XZ only.")
                ),
                List.of("x", "y", "z")
            ),
            (exchange, args) -> {
                boolean success = pathingService.moveTo(
                    args.intValue("x", 0),
                    args.intValue("y", 0),
                    args.intValue("z", 0),
                    args.bool("ignore_y", false)
                );
                if (!success) return McpResults.error("Player is not in a world.");
                return McpResults.ok(pathingService.getStatus());
            }
        ));

        tools.add(tool(
            "pathing_move_in_direction",
            "Move player continuously in a yaw direction.",
            ToolSchemas.object(Map.of("yaw", ToolSchemas.numberProperty("Yaw in degrees.")), List.of("yaw")),
            (exchange, args) -> {
                boolean success = pathingService.moveInDirection((float) args.doubleValue("yaw", 0));
                if (!success) return McpResults.error("Player is not in a world.");
                return McpResults.ok(pathingService.getStatus());
            }
        ));

        tools.add(tool("pathing_pause", "Pause current pathing process.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                boolean success = pathingService.pause();
                if (!success) return McpResults.error("Player is not in a world.");
                return McpResults.ok(pathingService.getStatus());
            }
        ));

        tools.add(tool("pathing_resume", "Resume paused pathing process.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                boolean success = pathingService.resume();
                if (!success) return McpResults.error("Player is not in a world.");
                return McpResults.ok(pathingService.getStatus());
            }
        ));

        tools.add(tool("pathing_stop", "Stop current pathing process.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                boolean success = pathingService.stop();
                if (!success) return McpResults.error("Player is not in a world.");
                return McpResults.ok(pathingService.getStatus());
            }
        ));

        return tools;
    }

    public List<McpServerFeatures.SyncResourceSpecification> resources() {
        List<McpServerFeatures.SyncResourceSpecification> resources = new ArrayList<>();

        resources.add(resource(
            "meteor://modules",
            "Meteor Module Schema",
            "All modules with setting schema and current values.",
            () -> moduleService.listModules(true)
        ));

        resources.add(resource(
            "meteor://state/player",
            "Player State",
            "Latest player state snapshot.",
            gameStateService::getPlayerState
        ));

        resources.add(resource(
            "meteor://state/world",
            "World State",
            "Latest world state snapshot.",
            gameStateService::getWorldState
        ));

        resources.add(resource(
            "meteor://state/inventory",
            "Inventory State",
            "Latest inventory snapshot.",
            gameStateService::getInventoryState
        ));

        resources.add(resource(
            "meteor://state/entities",
            "Nearby Entities",
            "Nearby entities around the player.",
            () -> gameStateService.getNearbyEntities(32D, 64)
        ));

        resources.add(resource(
            "meteor://state/pathing",
            "Pathing Status",
            "Current pathing manager status.",
            pathingService::getStatus
        ));

        resources.add(resource(
            "meteor://state/screen-dom",
            "Screen DOM",
            "DOM snapshot of the active screen.",
            screenDomService::snapshot
        ));

        resources.add(resource(
            "meteor://chat/history",
            "Chat History",
            "Buffered incoming/outgoing chat lines.",
            () -> chatLogService.snapshot(200)
        ));

        return resources;
    }

    private McpServerFeatures.SyncToolSpecification tool(
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

    private McpSchema.CallToolResult handleCall(
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

    private McpServerFeatures.SyncResourceSpecification resource(
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

    private McpSchema.ReadResourceResult readJsonResource(String uri, Supplier<Object> supplier) {
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

    private Map<String, Object> harnessStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ownerSession", sessionGate.ownerSessionId());
        status.put("singleSessionMode", config.singleSessionMode.get());
        status.put("bindHost", config.bindHost.get());
        status.put("bindPort", config.bindPort.get());
        status.put("mcpEndpoint", config.mcpEndpoint.get());
        status.put("inWorld", mc.world != null);
        status.put("hasPlayer", mc.player != null);
        status.put("currentScreen", mc.currentScreen == null ? null : mc.currentScreen.getClass().getName());

        return status;
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void disconnectToTitle() {
        ClientWorld world = mc.world;
        if (world != null) {
            try {
                world.disconnect(Text.literal("Disconnected by meteor-test-harness"));
            } catch (Exception ignored) {
                // Fall through to other strategies.
            }
        }

        invokeDisconnectReflection();
    }

    private void invokeDisconnectReflection() {
        try {
            Method noArg = mc.getClass().getMethod("disconnect");
            noArg.invoke(mc);
            return;
        } catch (Exception ignored) {
            // Try overloaded signatures.
        }

        try {
            Method oneArg = mc.getClass().getMethod("disconnect", Screen.class);
            oneArg.invoke(mc, new TitleScreen());
            return;
        } catch (Exception ignored) {
            // Try overloaded signatures.
        }

        try {
            Method threeArgs = mc.getClass().getMethod("disconnect", Screen.class, boolean.class, boolean.class);
            threeArgs.invoke(mc, new TitleScreen(), false, false);
        } catch (Exception ignored) {
            // Last resort: no-op.
        }
    }

    @FunctionalInterface
    private interface ToolHandler {
        McpSchema.CallToolResult handle(McpSyncServerExchange exchange, ArgReader args) throws Exception;
    }
}

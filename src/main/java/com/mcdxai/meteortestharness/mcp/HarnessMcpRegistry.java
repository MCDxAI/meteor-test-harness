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
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;


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
    private final NameMappingService nameMappingService;
    private final ModuleService moduleService;
    private final GameStateService gameStateService;
    private final ScreenDomService screenDomService;
    private final PathingService pathingService;
    private final ChatLogService chatLogService;

    public HarnessMcpRegistry(HarnessConfig config, SessionGate sessionGate, ChatLogService chatLogService) {
        this.config = config;
        this.sessionGate = sessionGate;

        this.settingValueCodec = new SettingValueCodec();
        this.nameMappingService = new NameMappingService();
        this.moduleService = new ModuleService(settingValueCodec);
        this.gameStateService = new GameStateService();
        this.screenDomService = new ScreenDomService(nameMappingService);
        this.pathingService = new PathingService();
        this.chatLogService = chatLogService;
    }

    public List<McpServerFeatures.SyncToolSpecification> tools() {
        List<McpServerFeatures.SyncToolSpecification> tools = new ArrayList<>();

        tools.add(tool("get_harness_status", "Get harness runtime/session status.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(harnessStatus())));

        tools.add(tool("get_harness_debug_info", "Get harness diagnostics (mapping/input internals).", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(harnessDebugInfo())));

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
            (exchange, args) -> McpResults.ok(Map.of("modules", moduleService.listModules(args.bool("include_settings", false))))
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
                return McpResults.ok(Map.of("settings", moduleService.listModuleSettings(module.get())));
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

        tools.add(tool("get_player_state", "Get core player state (position, vitals, movement flags, effects).", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(gameStateService.getPlayerState())));

        tools.add(tool("get_world_state", "Get current world state stream.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(gameStateService.getWorldState())));

        tools.add(tool(
            "get_player_inventory",
            "Get granular player inventory slices (hotbar/main/row/range/armor/offhand/hands/selected/all).",
            ToolSchemas.object(
                Map.of(
                    "section", ToolSchemas.stringProperty("Inventory section: all, inventory, hotbar, main, row, range, selected, armor, offhand, hands."),
                    "row", ToolSchemas.intProperty("Main inventory row index (0-2). Used when section=row."),
                    "slot_start", ToolSchemas.intProperty("Start slot index. Used when section=range."),
                    "slot_end", ToolSchemas.intProperty("End slot index. Used when section=range."),
                    "include_empty", ToolSchemas.boolProperty("Include empty slots in slot results. Default false.")
                ),
                List.of()
            ),
            (exchange, args) -> McpResults.ok(gameStateService.getPlayerInventory(
                args.string("section", "all"),
                args.intValue("row", 0),
                args.intValue("slot_start", -1),
                args.intValue("slot_end", -1),
                args.bool("include_empty", false)
            ))
        ));

        tools.add(tool("get_crosshair_target", "Get the current crosshair hit target only.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(gameStateService.getCrosshairTarget())));

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
            "get_screen_dom_summary",
            "Get a compact summary for the current or latest DOM snapshot.",
            ToolSchemas.object(
                Map.of("refresh", ToolSchemas.boolProperty("Capture a fresh snapshot before summarizing. Default true.")),
                List.of()
            ),
            (exchange, args) -> McpResults.ok(screenDomService.snapshotSummary(args.bool("refresh", true)))
        ));

        tools.add(tool(
            "find_dom_elements",
            "Query DOM elements server-side using filters and return only matched records.",
            ToolSchemas.object(
                Map.of(
                    "snapshot_id", ToolSchemas.stringProperty("Optional snapshot id from get_screen_dom."),
                    "filters", ToolSchemas.objectProperty("Filter object (label/moduleName/role/actions/text/etc)."),
                    "limit", ToolSchemas.intProperty("Maximum matched elements to return. Default 32."),
                    "fields", ToolSchemas.arrayProperty("Optional field whitelist for each returned element."),
                    "include_children", ToolSchemas.boolProperty("Include children/subtrees for each result.")
                ),
                List.of()
            ),
            (exchange, args) -> {
                Map<String, Object> result = screenDomService.findElements(
                    args.string("snapshot_id"),
                    args.object("filters"),
                    args.intValue("limit", 32),
                    args.list("fields"),
                    args.bool("include_children", false)
                );

                if (!resultOk(result)) {
                    return McpResults.error(String.valueOf(result.getOrDefault("reason", "dom_query_failed")), result);
                }
                return McpResults.ok(result);
            }
        ));

        tools.add(tool(
            "get_dom_element",
            "Get one DOM element by id from a snapshot (or latest snapshot).",
            ToolSchemas.object(
                Map.of(
                    "snapshot_id", ToolSchemas.stringProperty("Optional snapshot id from get_screen_dom."),
                    "element_id", ToolSchemas.stringProperty("Element id."),
                    "fields", ToolSchemas.arrayProperty("Optional field whitelist for returned element."),
                    "include_children", ToolSchemas.boolProperty("Include nested children for this element.")
                ),
                List.of("element_id")
            ),
            (exchange, args) -> {
                Map<String, Object> result = screenDomService.getElement(
                    args.string("snapshot_id"),
                    args.string("element_id"),
                    args.list("fields"),
                    args.bool("include_children", false)
                );

                if (!resultOk(result)) {
                    return McpResults.error(String.valueOf(result.getOrDefault("reason", "dom_element_not_found")), result);
                }
                return McpResults.ok(result);
            }
        ));

        tools.add(tool(
            "get_dom_subtree",
            "Get an element subtree by id with bounded depth.",
            ToolSchemas.object(
                Map.of(
                    "snapshot_id", ToolSchemas.stringProperty("Optional snapshot id from get_screen_dom."),
                    "element_id", ToolSchemas.stringProperty("Root element id."),
                    "depth", ToolSchemas.intProperty("Child depth to include. Default 2."),
                    "fields", ToolSchemas.arrayProperty("Optional field whitelist for nodes in the subtree.")
                ),
                List.of("element_id")
            ),
            (exchange, args) -> {
                Map<String, Object> result = screenDomService.getSubtree(
                    args.string("snapshot_id"),
                    args.string("element_id"),
                    args.intValue("depth", 2),
                    args.list("fields")
                );

                if (!resultOk(result)) {
                    return McpResults.error(String.valueOf(result.getOrDefault("reason", "dom_subtree_not_found")), result);
                }
                return McpResults.ok(result);
            }
        ));

        tools.add(tool(
            "click_dom_query",
            "Find a DOM element with filters and click it atomically.",
            ToolSchemas.object(
                Map.of(
                    "filters", ToolSchemas.objectProperty("Filter object used to select element(s)."),
                    "index", ToolSchemas.intProperty("Match index to click. Default 0."),
                    "button", ToolSchemas.intProperty("Mouse button code. 0=left, 1=right, 2=middle. Default 0."),
                    "double_click", ToolSchemas.boolProperty("Whether to send click as double-click.")
                ),
                List.of("filters")
            ),
            (exchange, args) -> {
                Map<String, Object> result = screenDomService.clickByQueryDetailed(
                    args.object("filters"),
                    args.intValue("index", 0),
                    args.intValue("button", 0),
                    args.bool("double_click", false)
                );

                if (!resultOk(result)) {
                    return McpResults.error(String.valueOf(result.getOrDefault("reason", "dom_query_click_failed")), result);
                }
                return McpResults.ok(result);
            }
        ));

        tools.add(tool(
            "set_dom_text_query",
            "Find a DOM text-capable element with filters and set text atomically.",
            ToolSchemas.object(
                Map.of(
                    "filters", ToolSchemas.objectProperty("Filter object used to select element(s)."),
                    "text", ToolSchemas.stringProperty("Text to apply."),
                    "index", ToolSchemas.intProperty("Match index. Default 0."),
                    "submit", ToolSchemas.boolProperty("Press Enter after setting text."),
                    "type_characters", ToolSchemas.boolProperty("Type through char events instead of direct assignment."),
                    "clear_first", ToolSchemas.boolProperty("Clear current text before typing.")
                ),
                List.of("filters", "text")
            ),
            (exchange, args) -> {
                Map<String, Object> result = screenDomService.setTextByQueryDetailed(
                    args.object("filters"),
                    args.intValue("index", 0),
                    args.string("text", ""),
                    args.bool("submit", false),
                    args.bool("type_characters", false),
                    args.bool("clear_first", true)
                );

                if (!resultOk(result)) {
                    return McpResults.error(String.valueOf(result.getOrDefault("reason", "dom_query_set_text_failed")), result);
                }
                return McpResults.ok(result);
            }
        ));

        tools.add(tool(
            "click_dom_element",
            "Click a DOM element by id.",
            ToolSchemas.object(
                Map.of(
                    "element_id", ToolSchemas.stringProperty("Element id from get_screen_dom."),
                    "button", ToolSchemas.intProperty("Mouse button code. 0=left, 1=right, 2=middle. Default 0."),
                    "double_click", ToolSchemas.boolProperty("Whether to send click as double-click.")
                ),
                List.of("element_id")
            ),
            (exchange, args) -> {
                Map<String, Object> interaction = screenDomService.clickDetailed(
                    args.string("element_id"),
                    args.intValue("button", 0),
                    args.bool("double_click", false)
                );
                if (!interactionSuccess(interaction)) {
                    return McpResults.error("Element not found or click was not handled.", domErrorDetails(interaction));
                }
                return McpResults.ok(withInteraction(interaction));
            }
        ));

        tools.add(tool(
            "set_dom_text",
            "Set text content on a DOM text input by id.",
            ToolSchemas.object(
                Map.of(
                    "element_id", ToolSchemas.stringProperty("Element id from get_screen_dom."),
                    "text", ToolSchemas.stringProperty("Text to set."),
                    "submit", ToolSchemas.boolProperty("Press Enter after setting text."),
                    "type_characters", ToolSchemas.boolProperty("Type through char events instead of direct assignment."),
                    "clear_first", ToolSchemas.boolProperty("Clear current text before typing.")
                ),
                List.of("element_id", "text")
            ),
            (exchange, args) -> {
                Map<String, Object> interaction = screenDomService.setTextDetailed(
                    args.string("element_id"),
                    args.string("text", ""),
                    args.bool("submit", false),
                    args.bool("type_characters", false),
                    args.bool("clear_first", true)
                );
                if (!interactionSuccess(interaction)) {
                    return McpResults.error("Element does not accept text input.", domErrorDetails(interaction));
                }
                return McpResults.ok(withInteraction(interaction));
            }
        ));

        tools.add(tool(
            "type_dom_text",
            "Type text into a DOM element through keyboard char events.",
            ToolSchemas.object(
                Map.of(
                    "element_id", ToolSchemas.stringProperty("Element id from get_screen_dom."),
                    "text", ToolSchemas.stringProperty("Text to type."),
                    "clear_first", ToolSchemas.boolProperty("Clear existing text first. Default true."),
                    "submit", ToolSchemas.boolProperty("Press Enter after typing.")
                ),
                List.of("element_id", "text")
            ),
            (exchange, args) -> {
                Map<String, Object> interaction = screenDomService.typeTextDetailed(
                    args.string("element_id"),
                    args.string("text", ""),
                    args.bool("clear_first", true),
                    args.bool("submit", false)
                );
                if (!interactionSuccess(interaction)) {
                    return McpResults.error("Typing failed for the selected element.", domErrorDetails(interaction));
                }
                return McpResults.ok(withInteraction(interaction));
            }
        ));

        tools.add(tool(
            "scroll_dom_element",
            "Scroll at a DOM element location (or screen center if no element id is provided).",
            ToolSchemas.object(
                Map.of(
                    "element_id", ToolSchemas.stringProperty("Optional element id from get_screen_dom."),
                    "vertical", ToolSchemas.numberProperty("Vertical scroll amount. Positive/negative follows Minecraft screen semantics."),
                    "horizontal", ToolSchemas.numberProperty("Horizontal scroll amount.")
                ),
                List.of()
            ),
            (exchange, args) -> {
                Map<String, Object> interaction = screenDomService.scrollDetailed(
                    args.string("element_id"),
                    args.doubleValue("vertical", -1D),
                    args.doubleValue("horizontal", 0D)
                );
                if (!interactionSuccess(interaction)) {
                    return McpResults.error("Scroll was not handled.", domErrorDetails(interaction));
                }
                return McpResults.ok(withInteraction(interaction));
            }
        ));

        tools.add(tool(
            "drag_dom_element",
            "Drag from the center of a DOM element by offsets.",
            ToolSchemas.object(
                Map.of(
                    "element_id", ToolSchemas.stringProperty("Element id from get_screen_dom."),
                    "offset_x", ToolSchemas.numberProperty("Drag offset on X axis in screen pixels."),
                    "offset_y", ToolSchemas.numberProperty("Drag offset on Y axis in screen pixels."),
                    "steps", ToolSchemas.intProperty("Number of drag interpolation steps. Default 8."),
                    "button", ToolSchemas.intProperty("Mouse button code. Default 0 (left).")
                ),
                List.of("element_id", "offset_x", "offset_y")
            ),
            (exchange, args) -> {
                Map<String, Object> interaction = screenDomService.dragDetailed(
                    args.string("element_id"),
                    args.doubleValue("offset_x", 0D),
                    args.doubleValue("offset_y", 0D),
                    args.intValue("steps", 8),
                    args.intValue("button", 0)
                );
                if (!interactionSuccess(interaction)) {
                    return McpResults.error("Drag was not handled.", domErrorDetails(interaction));
                }
                return McpResults.ok(withInteraction(interaction));
            }
        ));

        tools.add(tool(
            "press_screen_key",
            "Send a key press/release. Targets active screen when present, otherwise uses global in-game key handling.",
            ToolSchemas.object(
                Map.of(
                    "key", ToolSchemas.stringProperty("Key name (e.g. ENTER, ESCAPE, TAB, UP, A, F5)."),
                    "modifiers", ToolSchemas.intProperty("Modifier bitmask. Default 0."),
                    "repeat", ToolSchemas.intProperty("Number of keyPressed repeats. Default 1."),
                    "release", ToolSchemas.boolProperty("Whether to send keyReleased after presses. Default true.")
                ),
                List.of("key")
            ),
            (exchange, args) -> {
                Map<String, Object> interaction = screenDomService.pressKeyDetailed(
                    args.string("key"),
                    args.intValue("modifiers", 0),
                    args.intValue("repeat", 1),
                    args.bool("release", true)
                );
                if (!interactionSuccess(interaction)) {
                    return McpResults.error("Key press was not handled.", domErrorDetails(interaction));
                }
                return McpResults.ok(withInteraction(interaction));
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
                Map<String, Object> interaction = screenDomService.setValueDetailed(args.string("element_id"), args.raw("value"));
                if (!interactionSuccess(interaction)) {
                    return McpResults.error("Element does not support set_value.", domErrorDetails(interaction));
                }
                return McpResults.ok(withInteraction(interaction));
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
            (exchange, args) -> McpResults.ok(Map.of("messages", chatLogService.snapshot(args.intValue("count", 100))))
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
                int x = args.intValue("x", 0);
                int y = args.intValue("y", 0);
                int z = args.intValue("z", 0);
                boolean ignoreY = args.bool("ignore_y", false);

                boolean success = pathingService.moveTo(x, y, z, ignoreY);
                if (!success) return McpResults.error("Player is not in a world.");

                Map<String, Object> ack = new LinkedHashMap<>();
                ack.put("success", true);
                ack.put("target", Map.of("x", x, "y", y, "z", z));
                ack.put("ignoreY", ignoreY);
                ack.put("message", "Pathing request submitted");
                return McpResults.ok(ack);
            }
        ));

        tools.add(tool(
            "pathing_move_in_direction",
            "Move player continuously in a yaw direction.",
            ToolSchemas.object(Map.of("yaw", ToolSchemas.numberProperty("Yaw in degrees.")), List.of("yaw")),
            (exchange, args) -> {
                float yaw = (float) args.doubleValue("yaw", 0);
                boolean success = pathingService.moveInDirection(yaw);
                if (!success) return McpResults.error("Player is not in a world.");

                Map<String, Object> ack = new LinkedHashMap<>();
                ack.put("success", true);
                ack.put("yaw", yaw);
                ack.put("message", "Direction pathing request submitted");
                return McpResults.ok(ack);
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
            "meteor://state/crosshair",
            "Crosshair Target",
            "Latest crosshair target snapshot.",
            gameStateService::getCrosshairTarget
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

    private Map<String, Object> withInteraction(Map<String, Object> interaction) {
        Map<String, Object> dom = new LinkedHashMap<>(screenDomService.snapshot());
        dom.put("interaction", interaction);
        dom.put("success", interactionSuccess(interaction));
        return dom;
    }

    private Map<String, Object> domErrorDetails(Map<String, Object> interaction) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("interaction", interaction);
        details.put("dom", screenDomService.snapshot());
        return details;
    }

    private boolean interactionSuccess(Map<String, Object> interaction) {
        Object value = interaction.get("success");
        return value instanceof Boolean booleanValue && booleanValue;
    }

    private boolean resultOk(Map<String, Object> result) {
        Object value = result.get("success");
        return value instanceof Boolean booleanValue && booleanValue;
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

    private Map<String, Object> harnessDebugInfo() {
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

        invokeDisconnect();
    }

    private void invokeDisconnect() {
        mc.disconnect(new TitleScreen(), false, false);
    }

    @FunctionalInterface
    private interface ToolHandler {
        McpSchema.CallToolResult handle(McpSyncServerExchange exchange, ArgReader args) throws Exception;
    }
}

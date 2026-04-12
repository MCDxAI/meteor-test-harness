package com.mcdxai.meteortestharness.mcp;

import com.mcdxai.meteortestharness.services.ScreenDomService;
import com.mcdxai.meteortestharness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;
import java.util.Map;

final class HarnessDomTools {
    private HarnessDomTools() {
    }

    static void register(List<McpServerFeatures.SyncToolSpecification> tools, HarnessRegistryContext context) {
        ScreenDomService screenDomService = context.screenDomService();

        tools.add(context.tool("get_screen_dom", "Get current DOM tree for active screen.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(screenDomService.snapshot())));

        tools.add(context.tool(
            "get_screen_dom_summary",
            "Get a compact summary for the current or latest DOM snapshot.",
            ToolSchemas.object(
                Map.of("refresh", ToolSchemas.boolProperty("Capture a fresh snapshot before summarizing. Default true.")),
                List.of()
            ),
            (exchange, args) -> McpResults.ok(screenDomService.snapshotSummary(args.bool("refresh", true)))
        ));

        tools.add(context.tool(
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

                if (!context.resultOk(result)) {
                    return McpResults.error(String.valueOf(result.getOrDefault("reason", "dom_query_failed")), result);
                }
                return McpResults.ok(result);
            }
        ));

        tools.add(context.tool(
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

                if (!context.resultOk(result)) {
                    return McpResults.error(String.valueOf(result.getOrDefault("reason", "dom_element_not_found")), result);
                }
                return McpResults.ok(result);
            }
        ));

        tools.add(context.tool(
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

                if (!context.resultOk(result)) {
                    return McpResults.error(String.valueOf(result.getOrDefault("reason", "dom_subtree_not_found")), result);
                }
                return McpResults.ok(result);
            }
        ));

        tools.add(context.tool(
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

                if (!context.resultOk(result)) {
                    return McpResults.error(String.valueOf(result.getOrDefault("reason", "dom_query_click_failed")), result);
                }
                return McpResults.ok(result);
            }
        ));

        tools.add(context.tool(
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

                if (!context.resultOk(result)) {
                    return McpResults.error(String.valueOf(result.getOrDefault("reason", "dom_query_set_text_failed")), result);
                }
                return McpResults.ok(result);
            }
        ));

        tools.add(context.tool(
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
                if (!context.interactionSuccess(interaction)) {
                    return McpResults.error("Element not found or click was not handled.", context.domErrorDetails(interaction));
                }
                return McpResults.ok(context.withInteraction(interaction));
            }
        ));

        tools.add(context.tool(
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
                if (!context.interactionSuccess(interaction)) {
                    return McpResults.error("Element does not accept text input.", context.domErrorDetails(interaction));
                }
                return McpResults.ok(context.withInteraction(interaction));
            }
        ));

        tools.add(context.tool(
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
                if (!context.interactionSuccess(interaction)) {
                    return McpResults.error("Typing failed for the selected element.", context.domErrorDetails(interaction));
                }
                return McpResults.ok(context.withInteraction(interaction));
            }
        ));

        tools.add(context.tool(
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
                if (!context.interactionSuccess(interaction)) {
                    return McpResults.error("Scroll was not handled.", context.domErrorDetails(interaction));
                }
                return McpResults.ok(context.withInteraction(interaction));
            }
        ));

        tools.add(context.tool(
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
                if (!context.interactionSuccess(interaction)) {
                    return McpResults.error("Drag was not handled.", context.domErrorDetails(interaction));
                }
                return McpResults.ok(context.withInteraction(interaction));
            }
        ));

        tools.add(context.tool(
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
                if (!context.interactionSuccess(interaction)) {
                    return McpResults.error("Key press was not handled.", context.domErrorDetails(interaction));
                }
                return McpResults.ok(context.withInteraction(interaction));
            }
        ));

        tools.add(context.tool(
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
                if (!context.interactionSuccess(interaction)) {
                    return McpResults.error("Element does not support set_value.", context.domErrorDetails(interaction));
                }
                return McpResults.ok(context.withInteraction(interaction));
            }
        ));

        tools.add(context.tool("navigate_back", "Close current screen/go back.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(Map.of("success", screenDomService.navigateBack()))));
    }
}

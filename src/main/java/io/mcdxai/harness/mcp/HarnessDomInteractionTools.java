package io.mcdxai.harness.mcp;

import io.mcdxai.harness.services.ScreenDomService;
import io.mcdxai.harness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;
import java.util.Map;

final class HarnessDomInteractionTools {
    private HarnessDomInteractionTools() {
    }

    static void register(List<McpServerFeatures.SyncToolSpecification> tools, HarnessRegistryContext context) {
        ScreenDomService domService = context.screenDomService();
        DomToolHelper helper = new DomToolHelper(domService);

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
                Map<String, Object> result = domService.clickByQueryDetailed(
                    args.object("filters"),
                    args.intValue("index", 0),
                    args.intValue("button", 0),
                    args.bool("double_click", false)
                );

                if (!helper.isSuccess(result)) {
                    return McpResults.error(String.valueOf(result.getOrDefault("reason", "dom_query_click_failed")), result);
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
                Map<String, Object> interaction = domService.clickDetailed(
                    args.string("element_id"),
                    args.intValue("button", 0),
                    args.bool("double_click", false)
                );
                if (!helper.isSuccess(interaction)) {
                    return McpResults.error("Element not found or click was not handled.", helper.errorDetails(interaction));
                }
                return McpResults.ok(helper.withInteraction(interaction));
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
                Map<String, Object> interaction = domService.scrollDetailed(
                    args.string("element_id"),
                    args.doubleValue("vertical", -1D),
                    args.doubleValue("horizontal", 0D)
                );
                if (!helper.isSuccess(interaction)) {
                    return McpResults.error("Scroll was not handled.", helper.errorDetails(interaction));
                }
                return McpResults.ok(helper.withInteraction(interaction));
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
                Map<String, Object> interaction = domService.dragDetailed(
                    args.string("element_id"),
                    args.doubleValue("offset_x", 0D),
                    args.doubleValue("offset_y", 0D),
                    args.intValue("steps", 8),
                    args.intValue("button", 0)
                );
                if (!helper.isSuccess(interaction)) {
                    return McpResults.error("Drag was not handled.", helper.errorDetails(interaction));
                }
                return McpResults.ok(helper.withInteraction(interaction));
            }
        ));

        tools.add(context.tool("navigate_back", "Close current screen/go back.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(Map.of("success", domService.navigateBack()))));
    }
}

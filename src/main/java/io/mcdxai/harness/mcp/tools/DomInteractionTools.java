package io.mcdxai.harness.mcp.tools;

import io.mcdxai.harness.mcp.RegistryContext;
import io.mcdxai.harness.mcp.ToolSchemas;
import io.mcdxai.harness.services.ScreenDomService;
import io.mcdxai.harness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;
import java.util.Map;

public final class DomInteractionTools {
    private DomInteractionTools() {
    }

    public static void register(List<McpServerFeatures.SyncToolSpecification> tools, RegistryContext context) {
        ScreenDomService domService = context.screenDomService();
        DomToolHelper helper = new DomToolHelper(domService);

        tools.add(context.tool(
            "click_dom_query",
            "Find a DOM element by filters and click it atomically.",
            ToolSchemas.object(
                Map.of(
                    "filters", ToolSchemas.objectProperty("Filter object to match elements."),
                    "index", ToolSchemas.intProperty("Match index to click. Default 0."),
                    "button", ToolSchemas.intProperty("Mouse button: 0=left, 1=right, 2=middle. Default 0."),
                    "double_click", ToolSchemas.boolProperty("Send as double-click. Default false.")
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
                    "element_id", ToolSchemas.stringProperty("Element id."),
                    "button", ToolSchemas.intProperty("Mouse button: 0=left, 1=right, 2=middle. Default 0."),
                    "double_click", ToolSchemas.boolProperty("Send as double-click. Default false.")
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
            "Scroll at a DOM element or screen center.",
            ToolSchemas.object(
                Map.of(
                    "element_id", ToolSchemas.stringProperty("Element id. Omit to scroll at screen center."),
                    "vertical", ToolSchemas.numberProperty("Vertical scroll. Negative=up, positive=down. Default -1."),
                    "horizontal", ToolSchemas.numberProperty("Horizontal scroll. Default 0.")
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
                    "element_id", ToolSchemas.stringProperty("Element id."),
                    "offset_x", ToolSchemas.numberProperty("Drag offset X in screen pixels."),
                    "offset_y", ToolSchemas.numberProperty("Drag offset Y in screen pixels."),
                    "steps", ToolSchemas.intProperty("Drag interpolation steps. Default 8."),
                    "button", ToolSchemas.intProperty("Mouse button: 0=left, 1=right, 2=middle. Default 0.")
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

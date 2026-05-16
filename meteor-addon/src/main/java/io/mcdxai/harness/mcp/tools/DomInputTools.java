package io.mcdxai.harness.mcp.tools;

import io.mcdxai.harness.mcp.RegistryContext;
import io.mcdxai.harness.mcp.ToolSchemas;
import io.mcdxai.harness.services.ScreenDomService;
import io.mcdxai.harness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;
import java.util.Map;

public final class DomInputTools {
    private DomInputTools() {
    }

    public static void register(List<McpServerFeatures.SyncToolSpecification> tools, RegistryContext context) {
        ScreenDomService domService = context.screenDomService();
        DomToolHelper helper = new DomToolHelper(domService);

        tools.add(context.tool(
            "set_dom_text_query",
            "Find a text-capable DOM element by filters and set text atomically.",
            ToolSchemas.object(
                Map.of(
                    "filters", ToolSchemas.objectProperty("Filter object to match elements."),
                    "text", ToolSchemas.stringProperty("Text to apply."),
                    "index", ToolSchemas.intProperty("Match index. Default 0."),
                    "submit", ToolSchemas.boolProperty("Press Enter after setting text. Default false."),
                    "type_characters", ToolSchemas.boolProperty("Type via char events instead of direct set. Default false."),
                    "clear_first", ToolSchemas.boolProperty("Clear existing text first. Default true.")
                ),
                List.of("filters", "text")
            ),
            (exchange, args) -> {
                Map<String, Object> result = domService.setTextByQueryDetailed(
                    args.object("filters"),
                    args.intValue("index", 0),
                    args.string("text", ""),
                    args.bool("submit", false),
                    args.bool("type_characters", false),
                    args.bool("clear_first", true)
                );

                if (!helper.isSuccess(result)) {
                    return McpResults.error(String.valueOf(result.getOrDefault("reason", "dom_query_set_text_failed")), result);
                }
                return McpResults.ok(result);
            }
        ));

        tools.add(context.tool(
            "set_dom_text",
            "Set text on a DOM text input by id.",
            ToolSchemas.object(
                Map.of(
                    "element_id", ToolSchemas.stringProperty("Element id."),
                    "text", ToolSchemas.stringProperty("Text to set."),
                    "submit", ToolSchemas.boolProperty("Press Enter after setting text. Default false."),
                    "type_characters", ToolSchemas.boolProperty("Type via char events instead of direct set. Default false."),
                    "clear_first", ToolSchemas.boolProperty("Clear existing text first. Default true.")
                ),
                List.of("element_id", "text")
            ),
            (exchange, args) -> {
                Map<String, Object> interaction = domService.setTextDetailed(
                    args.string("element_id"),
                    args.string("text", ""),
                    args.bool("submit", false),
                    args.bool("type_characters", false),
                    args.bool("clear_first", true)
                );
                if (!helper.isSuccess(interaction)) {
                    return McpResults.error("Element does not accept text input.", helper.errorDetails(interaction));
                }
                return McpResults.ok(helper.withInteraction(interaction));
            }
        ));

        tools.add(context.tool(
            "type_dom_text",
            "Type text into a DOM element via keyboard char events.",
            ToolSchemas.object(
                Map.of(
                    "element_id", ToolSchemas.stringProperty("Element id."),
                    "text", ToolSchemas.stringProperty("Text to type."),
                    "clear_first", ToolSchemas.boolProperty("Clear existing text first. Default true."),
                    "submit", ToolSchemas.boolProperty("Press Enter after typing. Default false.")
                ),
                List.of("element_id", "text")
            ),
            (exchange, args) -> {
                Map<String, Object> interaction = domService.typeTextDetailed(
                    args.string("element_id"),
                    args.string("text", ""),
                    args.bool("clear_first", true),
                    args.bool("submit", false)
                );
                if (!helper.isSuccess(interaction)) {
                    return McpResults.error("Typing failed for the selected element.", helper.errorDetails(interaction));
                }
                return McpResults.ok(helper.withInteraction(interaction));
            }
        ));

        tools.add(context.tool(
            "press_screen_key",
            "Send a key press/release to active screen or global input.",
            ToolSchemas.object(
                Map.of(
                    "key", ToolSchemas.stringProperty("Key name (e.g. ENTER, ESCAPE, TAB, UP, A, F5)."),
                    "modifiers", ToolSchemas.intProperty("Modifier bitmask: 1=Shift, 2=Ctrl, 4=Alt. Default 0."),
                    "repeat", ToolSchemas.intProperty("Number of key press repeats. Default 1."),
                    "release", ToolSchemas.boolProperty("Send keyReleased after press. Default true.")
                ),
                List.of("key")
            ),
            (exchange, args) -> {
                Map<String, Object> interaction = domService.pressKeyDetailed(
                    args.string("key"),
                    args.intValue("modifiers", 0),
                    args.intValue("repeat", 1),
                    args.bool("release", true)
                );
                if (!helper.isSuccess(interaction)) {
                    return McpResults.error("Key press was not handled.", helper.errorDetails(interaction));
                }
                return McpResults.ok(helper.withInteraction(interaction));
            }
        ));

        tools.add(context.tool(
            "set_dom_value",
            "Set value on a DOM control (checkbox/slider) by id.",
            ToolSchemas.object(
                Map.of(
                    "element_id", ToolSchemas.stringProperty("Element id."),
                    "value", ToolSchemas.objectProperty("Value to set. Boolean for toggles, number for sliders.")
                ),
                List.of("element_id", "value")
            ),
            (exchange, args) -> {
                Map<String, Object> interaction = domService.setValueDetailed(args.string("element_id"), args.raw("value"));
                if (!helper.isSuccess(interaction)) {
                    return McpResults.error("Element does not support set_value.", helper.errorDetails(interaction));
                }
                return McpResults.ok(helper.withInteraction(interaction));
            }
        ));
    }
}

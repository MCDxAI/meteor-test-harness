package io.mcdxai.harness.universal.mcp.tools;

import io.mcdxai.harness.universal.mcp.RegistryContext;
import io.mcdxai.harness.universal.mcp.ToolSchemas;
import io.mcdxai.harness.universal.services.ScreenDomService;
import io.mcdxai.harness.universal.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;
import java.util.Map;

public final class DomQueryTools {
    private DomQueryTools() {
    }

    public static void register(List<McpServerFeatures.SyncToolSpecification> tools, RegistryContext context) {
        ScreenDomService domService = context.screenDomService();
        DomToolHelper helper = new DomToolHelper(domService);

        tools.add(context.tool("get_screen_dom", "Get full DOM tree for the active screen via the registered ScreenEngine.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(domService.snapshot())));

        tools.add(context.tool(
            "get_screen_dom_summary",
            "Get a compact summary of the current screen DOM.",
            ToolSchemas.object(Map.of("refresh", ToolSchemas.boolProperty("Take fresh snapshot first. Default true.")), List.of()),
            (exchange, args) -> McpResults.ok(domService.snapshotSummary(args.bool("refresh", true)))
        ));

        tools.add(context.tool(
            "find_dom_elements",
            "Query DOM elements by filters (label, text, role, type, actions, componentId).",
            ToolSchemas.object(
                Map.of(
                    "snapshot_id", ToolSchemas.stringProperty("Snapshot id. Omit for latest."),
                    "filters", ToolSchemas.objectProperty("Filter object. Keys: label, text, role, actions, type, componentId."),
                    "limit", ToolSchemas.intProperty("Max elements to return. Default 32."),
                    "fields", ToolSchemas.arrayProperty("Field whitelist for returned elements."),
                    "include_children", ToolSchemas.boolProperty("Include child elements. Default false.")
                ),
                List.of()
            ),
            (exchange, args) -> {
                Map<String, Object> result = domService.findElements(
                    args.string("snapshot_id"),
                    args.object("filters"),
                    args.intValue("limit", 32),
                    args.list("fields"),
                    args.bool("include_children", false)
                );
                if (!helper.isSuccess(result)) {
                    return McpResults.error(String.valueOf(result.getOrDefault("reason", "dom_query_failed")), result);
                }
                return McpResults.ok(result);
            }
        ));

        tools.add(context.tool(
            "get_dom_element",
            "Get one DOM element by id.",
            ToolSchemas.object(
                Map.of(
                    "snapshot_id", ToolSchemas.stringProperty("Snapshot id. Omit for latest."),
                    "element_id", ToolSchemas.stringProperty("Element id."),
                    "fields", ToolSchemas.arrayProperty("Field whitelist for returned element."),
                    "include_children", ToolSchemas.boolProperty("Include child elements. Default false.")
                ),
                List.of("element_id")
            ),
            (exchange, args) -> {
                Map<String, Object> result = domService.getElement(
                    args.string("snapshot_id"),
                    args.string("element_id"),
                    args.list("fields"),
                    args.bool("include_children", false)
                );
                if (!helper.isSuccess(result)) {
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
                    "snapshot_id", ToolSchemas.stringProperty("Snapshot id. Omit for latest."),
                    "element_id", ToolSchemas.stringProperty("Root element id."),
                    "depth", ToolSchemas.intProperty("Child depth to include. Default 2."),
                    "fields", ToolSchemas.arrayProperty("Field whitelist for subtree nodes.")
                ),
                List.of("element_id")
            ),
            (exchange, args) -> {
                Map<String, Object> result = domService.getSubtree(
                    args.string("snapshot_id"),
                    args.string("element_id"),
                    args.intValue("depth", 2),
                    args.list("fields")
                );
                if (!helper.isSuccess(result)) {
                    return McpResults.error(String.valueOf(result.getOrDefault("reason", "dom_subtree_not_found")), result);
                }
                return McpResults.ok(result);
            }
        ));
    }
}

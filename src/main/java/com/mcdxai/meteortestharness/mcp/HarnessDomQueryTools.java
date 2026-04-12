package com.mcdxai.meteortestharness.mcp;

import com.mcdxai.meteortestharness.services.ScreenDomService;
import com.mcdxai.meteortestharness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;
import java.util.Map;

final class HarnessDomQueryTools {
    private HarnessDomQueryTools() {
    }

    static void register(List<McpServerFeatures.SyncToolSpecification> tools, HarnessRegistryContext context) {
        ScreenDomService domService = context.screenDomService();
        DomToolHelper helper = new DomToolHelper(domService);

        tools.add(context.tool("get_screen_dom", "Get current DOM tree for active screen.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(domService.snapshot())));

        tools.add(context.tool(
            "get_screen_dom_summary",
            "Get a compact summary for the current or latest DOM snapshot.",
            ToolSchemas.object(
                Map.of("refresh", ToolSchemas.boolProperty("Capture a fresh snapshot before summarizing. Default true.")),
                List.of()
            ),
            (exchange, args) -> McpResults.ok(domService.snapshotSummary(args.bool("refresh", true)))
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
                    "snapshot_id", ToolSchemas.stringProperty("Optional snapshot id from get_screen_dom."),
                    "element_id", ToolSchemas.stringProperty("Root element id."),
                    "depth", ToolSchemas.intProperty("Child depth to include. Default 2."),
                    "fields", ToolSchemas.arrayProperty("Optional field whitelist for nodes in the subtree.")
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

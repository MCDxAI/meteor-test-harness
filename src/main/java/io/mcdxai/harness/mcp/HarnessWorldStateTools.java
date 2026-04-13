package io.mcdxai.harness.mcp;

import io.mcdxai.harness.services.GameStateService;
import io.mcdxai.harness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;
import java.util.Map;

final class HarnessWorldStateTools {
    private HarnessWorldStateTools() {
    }

    static void register(List<McpServerFeatures.SyncToolSpecification> tools, HarnessRegistryContext context) {
        GameStateService gameStateService = context.gameStateService();

        tools.add(context.tool("get_player_state", "Get core player state (position, vitals, movement flags, effects).", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(gameStateService.getPlayerState())));

        tools.add(context.tool("get_world_state", "Get current world state stream.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(gameStateService.getWorldState())));

        tools.add(context.tool(
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

        tools.add(context.tool("get_crosshair_target", "Get the current crosshair hit target only.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(gameStateService.getCrosshairTarget())));

        tools.add(context.tool(
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
    }
}

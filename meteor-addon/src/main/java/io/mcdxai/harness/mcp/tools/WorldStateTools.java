package io.mcdxai.harness.mcp.tools;

import io.mcdxai.harness.mcp.RegistryContext;
import io.mcdxai.harness.mcp.ToolSchemas;
import io.mcdxai.harness.services.GameStateService;
import io.mcdxai.harness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;
import java.util.Map;

public final class WorldStateTools {
    private WorldStateTools() {
    }

    public static void register(List<McpServerFeatures.SyncToolSpecification> tools, RegistryContext context) {
        GameStateService gameStateService = context.gameStateService();

        tools.add(context.tool("get_player_state", "Get player state (position, vitals, movement flags, effects).", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(gameStateService.getPlayerState())));

        tools.add(context.tool("get_world_state", "Get current world state snapshot.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(gameStateService.getWorldState())));

        tools.add(context.tool(
            "get_player_inventory",
            "Get player inventory by section.",
            ToolSchemas.object(
                Map.of(
                    "section", ToolSchemas.stringProperty("Section: all, inventory, hotbar, main, row, range, selected, armor, offhand, hands. Default all."),
                    "row", ToolSchemas.intProperty("Main inventory row (0-2). Used when section=row."),
                    "slot_start", ToolSchemas.intProperty("Start slot. Used when section=range."),
                    "slot_end", ToolSchemas.intProperty("End slot. Used when section=range."),
                    "include_empty", ToolSchemas.boolProperty("Include empty slots. Default false.")
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

        tools.add(context.tool("get_crosshair_target", "Get current crosshair target.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(gameStateService.getCrosshairTarget())));

        tools.add(context.tool(
            "get_nearby_entities",
            "Get nearby entities around the player.",
            ToolSchemas.object(
                Map.of(
                    "radius", ToolSchemas.numberProperty("Search radius in blocks. Default 32."),
                    "max_count", ToolSchemas.intProperty("Max entities to return. Default 64.")
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

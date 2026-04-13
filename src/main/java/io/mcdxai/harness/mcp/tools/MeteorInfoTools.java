package io.mcdxai.harness.mcp.tools;

import io.mcdxai.harness.mcp.RegistryContext;
import io.mcdxai.harness.mcp.ToolSchemas;
import io.mcdxai.harness.services.MeteorInfoService;
import io.mcdxai.harness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;
import java.util.Map;

public final class MeteorInfoTools {
    private MeteorInfoTools() {
    }

    public static void register(List<McpServerFeatures.SyncToolSpecification> tools, RegistryContext context) {
        MeteorInfoService meteorInfoService = context.meteorInfoService();

        tools.add(context.tool(
            "get_meteor_info",
            "Get Meteor Client environment: version, build, Baritone status, installed addons, module/HUD counts.",
            ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(meteorInfoService.getMeteorInfo())
        ));

        tools.add(context.tool(
            "list_addon_features",
            "List modules and HUD element types per addon. Omit addon_name for all addons.",
            ToolSchemas.object(
                Map.of("addon_name", ToolSchemas.stringProperty("Addon name to filter. Omit for all.")),
                List.of()
            ),
            (exchange, args) -> McpResults.ok(meteorInfoService.getAddonFeatures(args.string("addon_name")))
        ));

        tools.add(context.tool(
            "get_active_hud",
            "Get active HUD element instances currently shown on screen with positions. Text elements include evaluated value output. Returns disabled message if HUD is off.",
            ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(meteorInfoService.getActiveHud())
        ));
    }
}

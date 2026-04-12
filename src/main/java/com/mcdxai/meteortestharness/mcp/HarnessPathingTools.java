package com.mcdxai.meteortestharness.mcp;

import com.mcdxai.meteortestharness.services.PathingService;
import com.mcdxai.meteortestharness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class HarnessPathingTools {
    private HarnessPathingTools() {
    }

    static void register(List<McpServerFeatures.SyncToolSpecification> tools, HarnessRegistryContext context) {
        PathingService pathingService = context.pathingService();

        tools.add(context.tool("get_pathing_status", "Get Baritone/PathManager status.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(pathingService.getStatus())));

        tools.add(context.tool(
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

        tools.add(context.tool(
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

        tools.add(context.tool("pathing_pause", "Pause current pathing process.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                boolean success = pathingService.pause();
                if (!success) return McpResults.error("Player is not in a world.");
                return McpResults.ok(pathingService.getStatus());
            }
        ));

        tools.add(context.tool("pathing_resume", "Resume paused pathing process.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                boolean success = pathingService.resume();
                if (!success) return McpResults.error("Player is not in a world.");
                return McpResults.ok(pathingService.getStatus());
            }
        ));

        tools.add(context.tool("pathing_stop", "Stop current pathing process.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                boolean success = pathingService.stop();
                if (!success) return McpResults.error("Player is not in a world.");
                return McpResults.ok(pathingService.getStatus());
            }
        ));
    }
}

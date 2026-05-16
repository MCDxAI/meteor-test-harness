package io.mcdxai.harness.mcp.tools;

import io.mcdxai.harness.mcp.RegistryContext;
import io.mcdxai.harness.mcp.ToolSchemas;
import io.mcdxai.harness.services.PathingService;
import io.mcdxai.harness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PathingTools {
    private PathingTools() {
    }

    public static void register(List<McpServerFeatures.SyncToolSpecification> tools, RegistryContext context) {
        PathingService pathingService = context.pathingService();

        tools.add(context.tool("get_pathing_status", "Get Baritone/PathManager status.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(pathingService.getStatus())));

        tools.add(context.tool(
            "pathing_move_to",
            "Move player to coordinates via PathManager/Baritone.",
            ToolSchemas.object(
                Map.of(
                    "x", ToolSchemas.intProperty("Target block X."),
                    "y", ToolSchemas.intProperty("Target block Y."),
                    "z", ToolSchemas.intProperty("Target block Z."),
                    "ignore_y", ToolSchemas.boolProperty("Path in XZ only, ignore Y. Default false.")
                ),
                List.of("x", "y", "z")
            ),
            (exchange, args) -> {
                int x = args.intValue("x", 0);
                int y = args.intValue("y", 0);
                int z = args.intValue("z", 0);
                boolean ignoreY = args.bool("ignore_y", false);

                Map<String, Object> ack = pathingService.startMoveTo(x, y, z, ignoreY);
                if (ack == null) return McpResults.error("Player is not in a world.");
                return McpResults.ok(ack);
            }
        ));

        tools.add(context.tool(
            "pathing_move_in_direction",
            "Move player continuously in a yaw direction.",
            ToolSchemas.object(Map.of("yaw", ToolSchemas.numberProperty("Yaw in degrees (south=0, west=90, north=180, east=270).")), List.of("yaw")),
            (exchange, args) -> {
                float yaw = (float) args.doubleValue("yaw", 0);
                Map<String, Object> ack = pathingService.startMoveInDirection(yaw);
                if (ack == null) return McpResults.error("Player is not in a world.");
                return McpResults.ok(ack);
            }
        ));

        tools.add(context.tool(
            "wait_for_pathing_action",
            "Wait for a pathing action to reach terminal state or paused.",
            ToolSchemas.object(
                Map.of(
                    "action_id", ToolSchemas.stringProperty("Action id from pathing_move_to/pathing_move_in_direction. Omit for current."),
                    "timeout_ms", ToolSchemas.intProperty("Max wait in ms. Default 30000."),
                    "return_on", Map.of(
                        "type", "string",
                        "description", "Return condition: terminal (default) or paused.",
                        "enum", List.of("terminal", "paused")
                    )
                ),
                List.of()
            ),
            false,
            (exchange, args) -> {
                String actionId = args.string("action_id");
                int requestedTimeoutMs = args.intValue("timeout_ms", 30_000);
                String returnOn = args.string("return_on", "terminal");
                boolean returnOnPaused = "paused".equalsIgnoreCase(returnOn);

                int maxWaitMs = Math.max(100, context.requestTimeoutMillis() - 250);
                int timeoutMs = Math.max(100, Math.min(requestedTimeoutMs, maxWaitMs));
                boolean clamped = timeoutMs != requestedTimeoutMs;

                Map<String, Object> result = new LinkedHashMap<>(pathingService.waitForAction(actionId, timeoutMs, returnOnPaused));
                result.put("requested_timeout_ms", requestedTimeoutMs);
                result.put("effective_timeout_ms", timeoutMs);
                result.put("timeout_clamped_by_server", clamped);
                return McpResults.ok(result);
            }
        ));

        tools.add(context.tool("pathing_pause", "Pause current pathing.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                boolean success = pathingService.pause();
                if (!success) return McpResults.error("Player is not in a world.");
                return McpResults.ok(pathingService.getStatus());
            }
        ));

        tools.add(context.tool("pathing_resume", "Resume paused pathing.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                boolean success = pathingService.resume();
                if (!success) return McpResults.error("Player is not in a world.");
                return McpResults.ok(pathingService.getStatus());
            }
        ));

        tools.add(context.tool("pathing_stop", "Stop current pathing.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                boolean success = pathingService.stop();
                if (!success) return McpResults.error("Player is not in a world.");
                return McpResults.ok(pathingService.getStatus());
            }
        ));
    }
}

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

                Map<String, Object> ack = pathingService.startMoveTo(x, y, z, ignoreY);
                if (ack == null) return McpResults.error("Player is not in a world.");
                return McpResults.ok(ack);
            }
        ));

        tools.add(context.tool(
            "pathing_move_in_direction",
            "Move player continuously in a yaw direction.",
            ToolSchemas.object(Map.of("yaw", ToolSchemas.numberProperty("Yaw in degrees.")), List.of("yaw")),
            (exchange, args) -> {
                float yaw = (float) args.doubleValue("yaw", 0);
                Map<String, Object> ack = pathingService.startMoveInDirection(yaw);
                if (ack == null) return McpResults.error("Player is not in a world.");
                return McpResults.ok(ack);
            }
        ));

        tools.add(context.tool(
            "wait_for_pathing_action",
            "Wait for a tracked pathing action to reach terminal state (or paused when requested).",
            ToolSchemas.object(
                Map.of(
                    "action_id", ToolSchemas.stringProperty("Action id returned by pathing_move_to or pathing_move_in_direction. Defaults to current active action."),
                    "timeout_ms", ToolSchemas.intProperty("Maximum wait duration in milliseconds."),
                    "return_on", Map.of(
                        "type", "string",
                        "description", "Condition to return early: terminal (default) or paused.",
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

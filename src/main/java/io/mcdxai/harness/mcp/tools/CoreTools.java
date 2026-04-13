package io.mcdxai.harness.mcp.tools;

import io.mcdxai.harness.mcp.RegistryContext;
import io.mcdxai.harness.mcp.ToolSchemas;
import io.mcdxai.harness.services.HarnessService;
import io.mcdxai.harness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;

public final class CoreTools {
    private CoreTools() {
    }

    public static void register(List<McpServerFeatures.SyncToolSpecification> tools, RegistryContext context) {
        HarnessService harnessService = context.harnessService();

        tools.add(context.tool("get_harness_status", "Get harness runtime/session status.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(harnessService.harnessStatus())));

        tools.add(context.tool("get_harness_debug_info", "Get harness diagnostics.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(harnessService.harnessDebugInfo())));

        tools.add(context.tool("release_session", "Release current session ownership lock.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                context.sessionGate().release(exchange.sessionId());
                return McpResults.ok("Session ownership released.", harnessService.harnessStatus());
            }));
    }
}

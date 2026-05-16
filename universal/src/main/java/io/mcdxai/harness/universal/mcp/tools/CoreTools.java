package io.mcdxai.harness.universal.mcp.tools;

import io.mcdxai.harness.universal.mcp.RegistryContext;
import io.mcdxai.harness.universal.mcp.ToolSchemas;
import io.mcdxai.harness.universal.services.HarnessService;
import io.mcdxai.harness.universal.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;

public final class CoreTools {
    private CoreTools() {
    }

    public static void register(List<McpServerFeatures.SyncToolSpecification> tools, RegistryContext context) {
        HarnessService harnessService = context.harnessService();

        tools.add(context.tool("get_harness_status", "Get harness runtime/session status.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(harnessService.harnessStatus())));

        tools.add(context.tool("get_harness_debug_info", "Get harness diagnostics (loaded mods, registered engines).", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(harnessService.harnessDebugInfo())));

        tools.add(context.tool("list_supported_engines", "List screen engines registered in the adapter registry.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(harnessService.listSupportedEngines())));

        tools.add(context.tool("release_session", "Release current session ownership lock.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                context.sessionGate().release(exchange.sessionId());
                return McpResults.ok("Session ownership released.", harnessService.harnessStatus());
            }));
    }
}

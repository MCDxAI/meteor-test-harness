package com.mcdxai.meteortestharness.mcp;

import com.mcdxai.meteortestharness.services.HarnessService;
import com.mcdxai.meteortestharness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;

final class HarnessCoreTools {
    private HarnessCoreTools() {
    }

    static void register(List<McpServerFeatures.SyncToolSpecification> tools, HarnessRegistryContext context) {
        HarnessService harnessService = context.harnessService();

        tools.add(context.tool("get_harness_status", "Get harness runtime/session status.", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(harnessService.harnessStatus())));

        tools.add(context.tool("get_harness_debug_info", "Get harness diagnostics (mapping/input internals).", ToolSchemas.emptyObject(),
            (exchange, args) -> McpResults.ok(harnessService.harnessDebugInfo())));

        tools.add(context.tool("release_session", "Release current session ownership lock.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                context.sessionGate().release(exchange.sessionId());
                return McpResults.ok("Session ownership released.", harnessService.harnessStatus());
            }));
    }
}

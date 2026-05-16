package io.mcdxai.harness.mcp;

import io.mcdxai.harness.config.HarnessConfig;
import io.mcdxai.harness.mcp.tools.CoreTools;
import io.mcdxai.harness.mcp.tools.DomInputTools;
import io.mcdxai.harness.mcp.tools.DomInteractionTools;
import io.mcdxai.harness.mcp.tools.DomQueryTools;
import io.mcdxai.harness.mcp.tools.MeteorInfoTools;
import io.mcdxai.harness.mcp.tools.ModuleTools;
import io.mcdxai.harness.mcp.tools.PathingTools;
import io.mcdxai.harness.mcp.tools.Resources;
import io.mcdxai.harness.mcp.tools.WorldActionTools;
import io.mcdxai.harness.mcp.tools.WorldStateTools;
import io.mcdxai.harness.services.ChatLogService;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.ArrayList;
import java.util.List;

public final class McpRegistry {
    private final RegistryContext context;

    public McpRegistry(HarnessConfig config, SessionGate sessionGate, ChatLogService chatLogService) {
        this.context = new RegistryContext(config, sessionGate, chatLogService);
    }

    public List<McpServerFeatures.SyncToolSpecification> tools() {
        List<McpServerFeatures.SyncToolSpecification> tools = new ArrayList<>();

        CoreTools.register(tools, context);
        MeteorInfoTools.register(tools, context);
        ModuleTools.register(tools, context);
        WorldStateTools.register(tools, context);
        DomQueryTools.register(tools, context);
        DomInteractionTools.register(tools, context);
        DomInputTools.register(tools, context);
        WorldActionTools.register(tools, context);
        PathingTools.register(tools, context);

        return tools;
    }

    public List<McpServerFeatures.SyncResourceSpecification> resources() {
        List<McpServerFeatures.SyncResourceSpecification> resources = new ArrayList<>();
        Resources.register(resources, context);
        return resources;
    }
}

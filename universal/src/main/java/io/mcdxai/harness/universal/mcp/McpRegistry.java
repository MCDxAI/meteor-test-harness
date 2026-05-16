package io.mcdxai.harness.universal.mcp;

import io.mcdxai.harness.universal.adapter.AdapterRegistry;
import io.mcdxai.harness.universal.config.HarnessConfig;
import io.mcdxai.harness.universal.mcp.tools.CoreTools;
import io.mcdxai.harness.universal.mcp.tools.DomInputTools;
import io.mcdxai.harness.universal.mcp.tools.DomInteractionTools;
import io.mcdxai.harness.universal.mcp.tools.DomQueryTools;
import io.mcdxai.harness.universal.mcp.tools.Resources;
import io.mcdxai.harness.universal.mcp.tools.WorldActionTools;
import io.mcdxai.harness.universal.mcp.tools.WorldStateTools;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.ArrayList;
import java.util.List;

public final class McpRegistry {
    private final RegistryContext context;

    public McpRegistry(HarnessConfig config, SessionGate sessionGate, AdapterRegistry adapterRegistry) {
        this.context = new RegistryContext(config, sessionGate, adapterRegistry);
    }

    public List<McpServerFeatures.SyncToolSpecification> tools() {
        List<McpServerFeatures.SyncToolSpecification> tools = new ArrayList<>();
        CoreTools.register(tools, context);
        WorldStateTools.register(tools, context);
        WorldActionTools.register(tools, context);
        DomQueryTools.register(tools, context);
        DomInteractionTools.register(tools, context);
        DomInputTools.register(tools, context);
        return tools;
    }

    public List<McpServerFeatures.SyncResourceSpecification> resources() {
        List<McpServerFeatures.SyncResourceSpecification> resources = new ArrayList<>();
        Resources.register(resources, context);
        return resources;
    }
}

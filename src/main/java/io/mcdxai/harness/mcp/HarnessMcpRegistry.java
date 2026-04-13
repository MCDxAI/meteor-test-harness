package io.mcdxai.harness.mcp;

import io.mcdxai.harness.config.HarnessConfig;
import io.mcdxai.harness.services.ChatLogService;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.ArrayList;
import java.util.List;

public final class HarnessMcpRegistry {
    private final HarnessRegistryContext context;

    public HarnessMcpRegistry(HarnessConfig config, SessionGate sessionGate, ChatLogService chatLogService) {
        this.context = new HarnessRegistryContext(config, sessionGate, chatLogService);
    }

    public List<McpServerFeatures.SyncToolSpecification> tools() {
        List<McpServerFeatures.SyncToolSpecification> tools = new ArrayList<>();

        HarnessCoreTools.register(tools, context);
        HarnessModuleTools.register(tools, context);
        HarnessWorldStateTools.register(tools, context);
        HarnessDomQueryTools.register(tools, context);
        HarnessDomInteractionTools.register(tools, context);
        HarnessDomInputTools.register(tools, context);
        HarnessWorldActionTools.register(tools, context);
        HarnessPathingTools.register(tools, context);

        return tools;
    }

    public List<McpServerFeatures.SyncResourceSpecification> resources() {
        List<McpServerFeatures.SyncResourceSpecification> resources = new ArrayList<>();
        HarnessResources.register(resources, context);
        return resources;
    }
}

package io.mcdxai.harness.universal;

import io.mcdxai.harness.universal.adapter.AdapterRegistry;
import io.mcdxai.harness.universal.config.HarnessConfig;
import io.mcdxai.harness.universal.mcp.McpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HarnessRuntime {
    private static final Logger LOG = LoggerFactory.getLogger("mc-test-harness-universal");

    private final HarnessConfig config;
    private final McpServer server;

    public HarnessRuntime(HarnessConfig config, AdapterRegistry registry) {
        this.config = config;
        this.server = new McpServer(config, registry);
    }

    public void start() {
        if (!config.autoStart) {
            LOG.info("Universal harness auto-start disabled — MCP server not launched.");
            return;
        }
        if (server.start()) {
            LOG.info("Universal harness ready.");
        }
    }

    public void stop() {
        server.stop();
    }

    public boolean isRunning() {
        return server.isRunning();
    }
}

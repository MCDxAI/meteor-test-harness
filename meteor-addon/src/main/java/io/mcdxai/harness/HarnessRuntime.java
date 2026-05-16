package io.mcdxai.harness;

import io.mcdxai.harness.config.HarnessConfig;
import io.mcdxai.harness.mcp.McpServer;

public final class HarnessRuntime {
    private McpServer server;

    public void initialize() {
        HarnessConfig config = HarnessConfig.get();
        this.server = new McpServer(config);

        if (config.autoStart.get()) {
            startServer();
        }
    }

    public synchronized boolean startServer() {
        if (server == null) {
            server = new McpServer(HarnessConfig.get());
        }

        return server.start();
    }

    public synchronized void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    public synchronized boolean restartServer() {
        stopServer();
        return startServer();
    }

    public synchronized boolean isServerRunning() {
        return server != null && server.isRunning();
    }

    public synchronized McpServer server() {
        return server;
    }
}
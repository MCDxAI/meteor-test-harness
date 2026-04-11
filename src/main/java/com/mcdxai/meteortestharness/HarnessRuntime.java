package com.mcdxai.meteortestharness;

import com.mcdxai.meteortestharness.config.HarnessConfig;
import com.mcdxai.meteortestharness.mcp.McpHarnessServer;

public final class HarnessRuntime {
    private McpHarnessServer server;

    public void initialize() {
        HarnessConfig config = HarnessConfig.get();
        this.server = new McpHarnessServer(config);

        if (config.autoStart.get()) {
            startServer();
        }
    }

    public synchronized boolean startServer() {
        if (server == null) {
            server = new McpHarnessServer(HarnessConfig.get());
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

    public synchronized McpHarnessServer server() {
        return server;
    }
}
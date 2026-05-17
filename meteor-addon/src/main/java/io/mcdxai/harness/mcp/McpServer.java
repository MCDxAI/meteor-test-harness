package io.mcdxai.harness.mcp;

import io.mcdxai.harness.McTestHarnessAddon;
import io.mcdxai.harness.config.HarnessConfig;
import io.mcdxai.harness.services.ChatLogService;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;

import java.time.Duration;

import static meteordevelopment.meteorclient.MeteorClient.EVENT_BUS;

public final class McpServer {
    private final HarnessConfig config;
    private final SessionGate sessionGate;

    private ChatLogService chatLogService;
    private McpRegistry registry;

    private HttpServletStreamableServerTransportProvider transportProvider;
    private McpSyncServer mcpServer;
    private Tomcat tomcat;

    private volatile boolean running;

    public McpServer(HarnessConfig config) {
        this.config = config;
        this.sessionGate = new SessionGate();
    }

    public synchronized boolean start() {
        if (running) {
            return true;
        }

        try {
            sessionGate.clear();

            chatLogService = new ChatLogService(config.chatHistoryLimit.get());
            EVENT_BUS.subscribe(chatLogService);

            transportProvider = HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint(config.mcpEndpoint.get())
                .keepAliveInterval(Duration.ofSeconds(config.keepAliveSeconds.get()))
                .build();

            registry = new McpRegistry(config, sessionGate, chatLogService);

            mcpServer = io.modelcontextprotocol.server.McpServer.sync(transportProvider)
                .serverInfo("mc-test-harness-meteor", "0.1.0")
                .instructions("Local singleplayer Meteor test harness. Use DOM tools for all UI interactions.")
                .tools(registry.tools())
                .resources(registry.resources())
                .requestTimeout(Duration.ofSeconds(config.requestTimeoutSeconds.get()))
                .build();

            tomcat = createEmbeddedTomcat(transportProvider);
            tomcat.start();

            running = true;

            McTestHarnessAddon.LOG.info(
                "MCP server started at http://{}:{}{}",
                config.bindHost.get(),
                config.bindPort.get(),
                config.mcpEndpoint.get()
            );

            return true;
        } catch (Exception e) {
            McTestHarnessAddon.LOG.error("Failed to start MCP harness server.", e);
            stop();
            return false;
        }
    }

    public synchronized void stop() {
        if (!running && tomcat == null && mcpServer == null) {
            return;
        }

        running = false;

        if (chatLogService != null) {
            try {
                EVENT_BUS.unsubscribe(chatLogService);
            } catch (Exception ignored) {
                // No-op.
            }
            chatLogService = null;
        }

        if (mcpServer != null) {
            try {
                mcpServer.closeGracefully();
            } catch (Exception ignored) {
                // No-op.
            }
            mcpServer = null;
        }

        if (tomcat != null) {
            try {
                tomcat.stop();
            } catch (LifecycleException ignored) {
                // No-op.
            }

            try {
                tomcat.destroy();
            } catch (LifecycleException ignored) {
                // No-op.
            }

            tomcat = null;
        }

        transportProvider = null;
        registry = null;
        sessionGate.clear();

        McTestHarnessAddon.LOG.info("MCP server stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    private Tomcat createEmbeddedTomcat(HttpServletStreamableServerTransportProvider provider) {
        Tomcat embeddedTomcat = new Tomcat();
        embeddedTomcat.setPort(config.bindPort.get());

        String baseDir = System.getProperty("java.io.tmpdir") + "/mc-test-harness-meteor-tomcat";
        embeddedTomcat.setBaseDir(baseDir);

        Context context = embeddedTomcat.addContext("", baseDir);

        // Use a classloader that skips leak-detection cleanup (JDBC deregister,
        // ThreadLocal/RMI checks) — those assume a WAR deployment and fail
        // under Fabric's Knot classloader.
        if (context instanceof StandardContext stdCtx) {
            WebappLoader loader = new WebappLoader();
            loader.setLoaderClass(EmbeddedWebappClassLoader.class.getName());
            stdCtx.setLoader(loader);
        }

        Wrapper wrapper = context.createWrapper();
        wrapper.setName("mcpServlet");
        wrapper.setServlet(provider);
        wrapper.setLoadOnStartup(1);
        wrapper.setAsyncSupported(true);
        context.addChild(wrapper);
        context.addServletMappingDecoded("/*", "mcpServlet");

        var connector = embeddedTomcat.getConnector();
        connector.setProperty("address", config.bindHost.get());
        connector.setAsyncTimeout(Duration.ofSeconds(config.requestTimeoutSeconds.get()).toMillis());

        return embeddedTomcat;
    }
}

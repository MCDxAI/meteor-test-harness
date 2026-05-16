package io.mcdxai.harness.universal.mcp;

import io.mcdxai.harness.universal.adapter.AdapterRegistry;
import io.mcdxai.harness.universal.config.HarnessConfig;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public final class McpServer {
    private static final Logger LOG = LoggerFactory.getLogger("universal-harness/mcp");

    private final HarnessConfig config;
    private final SessionGate sessionGate;
    private final AdapterRegistry adapterRegistry;

    private McpRegistry registry;
    private HttpServletStreamableServerTransportProvider transportProvider;
    private McpSyncServer mcpServer;
    private Tomcat tomcat;

    private volatile boolean running;

    public McpServer(HarnessConfig config, AdapterRegistry adapterRegistry) {
        this.config = config;
        this.adapterRegistry = adapterRegistry;
        this.sessionGate = new SessionGate();
    }

    public synchronized boolean start() {
        if (running) return true;

        try {
            sessionGate.clear();

            transportProvider = HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint(config.mcpEndpoint)
                .keepAliveInterval(Duration.ofSeconds(config.keepAliveSeconds))
                .build();

            registry = new McpRegistry(config, sessionGate, adapterRegistry);

            mcpServer = io.modelcontextprotocol.server.McpServer.sync(transportProvider)
                .serverInfo("universal-harness", "0.1.0")
                .instructions("Universal MCP test harness. Use DOM tools to introspect and interact with the active screen across vanilla, owo-lib, and hybrid screens.")
                .tools(registry.tools())
                .resources(registry.resources())
                .requestTimeout(Duration.ofSeconds(config.requestTimeoutSeconds))
                .build();

            tomcat = createEmbeddedTomcat(transportProvider);
            tomcat.start();

            running = true;
            LOG.info("Universal MCP server started at http://{}:{}{}", config.bindHost, config.bindPort, config.mcpEndpoint);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to start universal MCP server.", e);
            stop();
            return false;
        }
    }

    public synchronized void stop() {
        if (!running && tomcat == null && mcpServer == null) return;
        running = false;

        if (mcpServer != null) {
            try {
                mcpServer.closeGracefully();
            } catch (Exception ignored) {
            }
            mcpServer = null;
        }

        if (tomcat != null) {
            try {
                tomcat.stop();
            } catch (LifecycleException ignored) {
            }
            try {
                tomcat.destroy();
            } catch (LifecycleException ignored) {
            }
            tomcat = null;
        }

        transportProvider = null;
        registry = null;
        sessionGate.clear();

        LOG.info("Universal MCP server stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    private Tomcat createEmbeddedTomcat(HttpServletStreamableServerTransportProvider provider) {
        Tomcat embeddedTomcat = new Tomcat();
        embeddedTomcat.setPort(config.bindPort);

        String baseDir = System.getProperty("java.io.tmpdir") + "/universal-harness-tomcat";
        embeddedTomcat.setBaseDir(baseDir);

        Context context = embeddedTomcat.addContext("", baseDir);

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
        connector.setProperty("address", config.bindHost);
        connector.setAsyncTimeout(Duration.ofSeconds(config.requestTimeoutSeconds).toMillis());

        return embeddedTomcat;
    }
}

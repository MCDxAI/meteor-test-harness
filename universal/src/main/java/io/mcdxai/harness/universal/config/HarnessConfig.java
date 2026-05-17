package io.mcdxai.harness.universal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HarnessConfig {
    private static final Logger LOG = LoggerFactory.getLogger("mc-test-harness-universal/config");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String bindHost = "127.0.0.1";
    public int bindPort = 38862;
    public String mcpEndpoint = "/mcp";
    public int keepAliveSeconds = 30;
    public int requestTimeoutSeconds = 30;
    public boolean singleSessionMode = false;
    public boolean autoStart = true;

    public static HarnessConfig load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            HarnessConfig defaults = new HarnessConfig();
            try {
                defaults.save();
            } catch (IOException e) {
                LOG.warn("Failed to write default harness config to {}: {}", path, e.getMessage());
            }
            return defaults;
        }

        try {
            return MAPPER.readValue(path.toFile(), HarnessConfig.class);
        } catch (IOException e) {
            LOG.warn("Failed to read harness config at {} ({}). Using defaults.", path, e.getMessage());
            return new HarnessConfig();
        }
    }

    public void save() throws IOException {
        Path path = configPath();
        Files.createDirectories(path.getParent());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), this);
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("mc-test-harness-universal.json");
    }
}

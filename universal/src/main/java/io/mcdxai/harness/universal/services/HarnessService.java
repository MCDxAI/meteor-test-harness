package io.mcdxai.harness.universal.services;

import io.mcdxai.harness.universal.adapter.AdapterRegistry;
import io.mcdxai.harness.universal.adapter.ScreenEngine;
import io.mcdxai.harness.universal.config.HarnessConfig;
import io.mcdxai.harness.universal.mcp.SessionGate;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HarnessService {
    private final HarnessConfig config;
    private final SessionGate sessionGate;
    private final AdapterRegistry registry;

    public HarnessService(HarnessConfig config, SessionGate sessionGate, AdapterRegistry registry) {
        this.config = config;
        this.sessionGate = sessionGate;
        this.registry = registry;
    }

    public Map<String, Object> harnessStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ownerSession", sessionGate.ownerSessionId());
        status.put("singleSessionMode", config.singleSessionMode);
        status.put("bindHost", config.bindHost);
        status.put("bindPort", config.bindPort);
        status.put("mcpEndpoint", config.mcpEndpoint);
        Minecraft mc = Minecraft.getInstance();
        status.put("inWorld", mc.level != null);
        status.put("hasPlayer", mc.player != null);
        putScreenInfo(status, mc.screen);
        return status;
    }

    public Map<String, Object> harnessDebugInfo() {
        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("inputKeyDispatchMode", "keyboard_keyPress");
        debug.put("loadedMods", listLoadedMods());
        debug.put("registeredEngines", listEngines());
        putScreenInfo(debug, Minecraft.getInstance().screen);
        return debug;
    }

    public Map<String, Object> listSupportedEngines() {
        return Map.of("engines", listEngines());
    }

    public void disconnectToTitle() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;
        if (world != null) {
            try {
                world.disconnect(Component.literal("Disconnected by mc-test-harness"));
            } catch (Exception ignored) {
            }
        }
        mc.disconnect(new TitleScreen(), false, false);
    }

    private List<Map<String, Object>> listEngines() {
        List<Map<String, Object>> engines = new ArrayList<>();
        for (ScreenEngine engine : registry.screenEngines()) {
            engines.add(Map.of("name", engine.engineName(), "priority", engine.priority()));
        }
        return engines;
    }

    private List<String> listLoadedMods() {
        List<String> ids = new ArrayList<>();
        for (var container : FabricLoader.getInstance().getAllMods()) {
            ids.add(container.getMetadata().getId());
        }
        return ids;
    }

    private void putScreenInfo(Map<String, Object> map, Screen screen) {
        if (screen == null) {
            map.put("currentScreen", null);
            map.put("currentScreenType", null);
        } else {
            map.put("currentScreen", screen.getClass().getName());
            map.put("currentScreenType", screen.getClass().getSimpleName());
        }
    }
}

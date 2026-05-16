package io.mcdxai.harness.services;

import io.mcdxai.harness.config.HarnessConfig;
import io.mcdxai.harness.mcp.SessionGate;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class HarnessService {
    private final HarnessConfig config;
    private final SessionGate sessionGate;
    private final NameMappingService nameMappingService;

    public HarnessService(HarnessConfig config, SessionGate sessionGate, NameMappingService nameMappingService) {
        this.config = config;
        this.sessionGate = sessionGate;
        this.nameMappingService = nameMappingService;
    }

    public Map<String, Object> harnessStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ownerSession", sessionGate.ownerSessionId());
        status.put("singleSessionMode", config.singleSessionMode.get());
        status.put("bindHost", config.bindHost.get());
        status.put("bindPort", config.bindPort.get());
        status.put("mcpEndpoint", config.mcpEndpoint.get());
        status.put("inWorld", mc.level != null);
        status.put("hasPlayer", mc.player != null);

        putScreenInfo(status);
        return status;
    }

    public Map<String, Object> harnessDebugInfo() {
        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("mappingRuntimeNamespace", nameMappingService.getRuntimeNamespace());
        debug.put("mappingPreferredNamespace", nameMappingService.getPreferredNamespace());
        debug.put("mappingMode", nameMappingService.getMappingMode());
        debug.put("mappingNamespaces", nameMappingService.getNamespaces());
        debug.put("mappingRuntimeNamedAvailable", nameMappingService.hasRuntimeNamedMappings());
        debug.put("mappingBundledNamedAvailable", nameMappingService.hasBundledNamedMappings());
        debug.put("mappingBundledNamedClassCount", nameMappingService.getBundledNamedClassCount());
        debug.put("mappingBundledSource", nameMappingService.getBundledMappingsSource());
        debug.put("mappingBundledError", nameMappingService.getBundledMappingsError());
        debug.put("inputKeySimulationMode", "screen_and_global");
        debug.put("inputKeyDispatchMode", "keyboard_onKey");

        putScreenInfo(debug);
        return debug;
    }

    public void disconnectToTitle() {
        ClientLevel world = mc.level;
        if (world != null) {
            try {
                world.disconnect(Component.literal("Disconnected by meteor-test-harness"));
            } catch (Exception ignored) {
                // Fall through to other strategies.
            }
        }

        mc.disconnect(new TitleScreen(), false, false);
    }

    private void putScreenInfo(Map<String, Object> map) {
        Screen currentScreen = mc.screen;
        if (currentScreen == null) {
            map.put("currentScreen", null);
            map.put("currentScreenMapped", null);
            map.put("currentScreenType", null);
            map.put("currentScreenTypeMapped", null);
        } else {
            String rawClass = currentScreen.getClass().getName();
            String mappedClass = nameMappingService.mapClassName(rawClass);
            map.put("currentScreen", rawClass);
            map.put("currentScreenMapped", mappedClass);
            map.put("currentScreenType", nameMappingService.simpleName(rawClass));
            map.put("currentScreenTypeMapped", nameMappingService.simpleName(mappedClass));
        }
    }
}

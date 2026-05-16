package io.mcdxai.harness.universal;

import io.mcdxai.harness.universal.adapter.AdapterRegistry;
import io.mcdxai.harness.universal.adapter.hybrid.HybridScreenEngine;
import io.mcdxai.harness.universal.adapter.owo.OwoScreenEngine;
import io.mcdxai.harness.universal.adapter.owo.OwoWidgetAdapters;
import io.mcdxai.harness.universal.adapter.vanilla.VanillaScreenEngine;
import io.mcdxai.harness.universal.adapter.vanilla.VanillaWidgetAdapters;
import io.mcdxai.harness.universal.config.HarnessConfig;
import io.mcdxai.harness.universal.modspec.itemeditor.ItemEditorAdapters;
import io.mcdxai.harness.universal.modspec.itemeditor.ItemEditorScreenDescriptors;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UniversalHarnessMod implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("universal-harness");

    private static HarnessRuntime runtime;

    @Override
    public void onInitializeClient() {
        HarnessConfig config = HarnessConfig.load();
        AdapterRegistry registry = new AdapterRegistry();

        // Always register the vanilla engine + widget adapters.
        VanillaScreenEngine vanillaEngine = new VanillaScreenEngine();
        registry.registerScreenEngine(vanillaEngine);
        VanillaWidgetAdapters.registerAll(registry);

        // owo-lib is recommended; register conditionally so we don't blow up if absent.
        OwoScreenEngine owoEngine = null;
        boolean owoLoaded = FabricLoader.getInstance().isModLoaded("owo");
        if (owoLoaded) {
            owoEngine = new OwoScreenEngine();
            registry.registerScreenEngine(owoEngine);
            OwoWidgetAdapters.registerAll(registry);
            registry.registerScreenEngine(new HybridScreenEngine(vanillaEngine, owoEngine));
            LOG.info("owo-lib detected — Owo + Hybrid engines registered.");
        } else {
            LOG.info("owo-lib not present — only the vanilla engine is registered.");
        }

        if (FabricLoader.getInstance().isModLoaded("itemeditor")) {
            ItemEditorAdapters.registerAll(registry);
            ItemEditorScreenDescriptors.registerAll(registry);
            LOG.info("item-editor detected — registered custom decorators and screen descriptors.");
        }

        runtime = new HarnessRuntime(config, registry);
        runtime.start();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (runtime != null) runtime.stop();
        });
    }

    public static HarnessRuntime runtime() {
        return runtime;
    }
}

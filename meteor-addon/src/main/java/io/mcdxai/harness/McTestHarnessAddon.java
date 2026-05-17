package io.mcdxai.harness;

import io.mcdxai.harness.config.HarnessConfig;
import io.mcdxai.harness.gui.HarnessTab;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McTestHarnessAddon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("MC Test Harness");

    private static HarnessRuntime runtime;

    @Override
    public void onInitialize() {
        LOG.info("Initializing MC Test Harness addon.");

        Systems.add(new HarnessConfig());
        Tabs.add(new HarnessTab());

        runtime = new HarnessRuntime();
        runtime.initialize();

        LOG.info("MC Test Harness addon initialized.");
    }

    @Override
    public void onRegisterCategories() {
        // No custom module categories.
    }

    @Override
    public String getPackage() {
        return "io.mcdxai.harness";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MCDxAI", "mc-test-harness");
    }

    public static HarnessRuntime runtime() {
        return runtime;
    }
}

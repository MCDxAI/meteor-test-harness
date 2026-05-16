package io.mcdxai.harness;

import io.mcdxai.harness.config.HarnessConfig;
import io.mcdxai.harness.gui.HarnessTab;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeteorTestHarnessAddon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Meteor Test Harness");

    private static HarnessRuntime runtime;

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Test Harness addon.");

        Systems.add(new HarnessConfig());
        Tabs.add(new HarnessTab());

        runtime = new HarnessRuntime();
        runtime.initialize();

        LOG.info("Meteor Test Harness addon initialized.");
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
        return new GithubRepo("MCDxAI", "meteor-test-harness");
    }

    public static HarnessRuntime runtime() {
        return runtime;
    }
}

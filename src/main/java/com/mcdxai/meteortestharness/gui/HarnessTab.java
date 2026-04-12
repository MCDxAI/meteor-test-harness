package com.mcdxai.meteortestharness.gui;

import com.mcdxai.meteortestharness.HarnessRuntime;
import com.mcdxai.meteortestharness.MeteorTestHarnessAddon;
import com.mcdxai.meteortestharness.config.HarnessConfig;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import net.minecraft.client.gui.screen.Screen;

public final class HarnessTab extends Tab {
    public HarnessTab() {
        super("Harness");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new HarnessTabScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof HarnessTabScreen;
    }

    private static final class HarnessTabScreen extends WindowTabScreen {
        private HarnessTabScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
            window.padding = 6;
            window.spacing = 4;
            window.id = "meteor-test-harness-config";
        }

        @Override
        public void initWidgets() {
            HarnessConfig config = HarnessConfig.get();
            boolean serverRunning = isServerRunning();

            add(theme.settings(config.settings)).expandX();

            add(theme.horizontalSeparator()).expandX();

            var statusRow = add(theme.horizontalList()).expandX().widget();
            statusRow.add(theme.label("Server:"));
            statusRow.add(theme.label(serverRunning ? "Running" : "Stopped"));

            var endpointRow = add(theme.horizontalList()).expandX().widget();
            endpointRow.add(theme.label("Endpoint:"));
            endpointRow.add(theme.label(buildEndpoint(config))).expandX();

            var buttonRow = add(theme.horizontalList()).expandX().widget();

            if (serverRunning) {
                buttonRow.add(theme.button("Restart Server")).expandX().widget().action = () -> {
                    HarnessRuntime runtime = MeteorTestHarnessAddon.runtime();
                    if (runtime != null) runtime.restartServer();
                    reload();
                };

                buttonRow.add(theme.button("Stop Server")).expandX().widget().action = () -> {
                    HarnessRuntime runtime = MeteorTestHarnessAddon.runtime();
                    if (runtime != null) runtime.stopServer();
                    reload();
                };
            } else {
                buttonRow.add(theme.button("Start Server")).expandX().widget().action = () -> {
                    HarnessRuntime runtime = MeteorTestHarnessAddon.runtime();
                    if (runtime != null) runtime.startServer();
                    reload();
                };
            }

            buttonRow.add(theme.button("Refresh")).expandX().widget().action = this::reload;

            add(theme.horizontalSeparator()).expandX();
            add(theme.label("Settings save and apply immediately. Network and chat history changes restart the server when running.")).expandX();
        }

        private static boolean isServerRunning() {
            HarnessRuntime runtime = MeteorTestHarnessAddon.runtime();
            return runtime != null && runtime.isServerRunning();
        }

        private static String buildEndpoint(HarnessConfig config) {
            String endpoint = config.mcpEndpoint.get();
            if (!endpoint.startsWith("/")) endpoint = "/" + endpoint;
            return "http://" + config.bindHost.get() + ":" + config.bindPort.get() + endpoint;
        }
    }
}

package io.mcdxai.harness.config;

import io.mcdxai.harness.HarnessRuntime;
import io.mcdxai.harness.McTestHarnessAddon;
import meteordevelopment.meteorclient.systems.Systems;

final class HarnessConfigRuntimeApplier {
    private HarnessConfigRuntimeApplier() {
    }

    static void onSaveOnlySettingChanged() {
        if (!isRuntimeReady()) return;
        Systems.save();
    }

    static void onAutoStartChanged(boolean autoStartEnabled) {
        HarnessRuntime runtime = McTestHarnessAddon.runtime();
        if (runtime == null) return;

        Systems.save();

        if (autoStartEnabled) runtime.startServer();
        else runtime.stopServer();
    }

    static void onServerRestartSettingChanged() {
        HarnessRuntime runtime = McTestHarnessAddon.runtime();
        if (runtime == null) return;

        Systems.save();

        if (runtime.isServerRunning()) {
            runtime.restartServer();
        }
    }

    private static boolean isRuntimeReady() {
        return McTestHarnessAddon.runtime() != null;
    }
}

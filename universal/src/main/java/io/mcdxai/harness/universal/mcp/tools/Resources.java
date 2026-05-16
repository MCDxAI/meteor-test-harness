package io.mcdxai.harness.universal.mcp.tools;

import io.mcdxai.harness.universal.mcp.RegistryContext;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;

public final class Resources {
    private Resources() {
    }

    public static void register(List<McpServerFeatures.SyncResourceSpecification> resources, RegistryContext context) {
        resources.add(context.resource(
            "harness://state/player",
            "Player State",
            "Latest player state snapshot.",
            () -> context.gameStateService().getPlayerState()
        ));

        resources.add(context.resource(
            "harness://state/world",
            "World State",
            "Latest world state snapshot.",
            () -> context.gameStateService().getWorldState()
        ));

        resources.add(context.resource(
            "harness://state/crosshair",
            "Crosshair Target",
            "Latest crosshair target snapshot.",
            () -> context.gameStateService().getCrosshairTarget()
        ));

        resources.add(context.resource(
            "harness://state/entities",
            "Nearby Entities",
            "Nearby entities around the player.",
            () -> context.gameStateService().getNearbyEntities(32D, 64)
        ));

        resources.add(context.resource(
            "harness://state/screen-dom",
            "Screen DOM",
            "DOM snapshot of the active screen.",
            () -> context.screenDomService().snapshot()
        ));
    }
}

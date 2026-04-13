package io.mcdxai.harness.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;

final class HarnessResources {
    private HarnessResources() {
    }

    static void register(List<McpServerFeatures.SyncResourceSpecification> resources, HarnessRegistryContext context) {
        resources.add(context.resource(
            "meteor://modules",
            "Meteor Module Schema",
            "All modules with setting schema and current values.",
            () -> context.moduleService().listModules(true)
        ));

        resources.add(context.resource(
            "meteor://state/player",
            "Player State",
            "Latest player state snapshot.",
            () -> context.gameStateService().getPlayerState()
        ));

        resources.add(context.resource(
            "meteor://state/world",
            "World State",
            "Latest world state snapshot.",
            () -> context.gameStateService().getWorldState()
        ));

        resources.add(context.resource(
            "meteor://state/crosshair",
            "Crosshair Target",
            "Latest crosshair target snapshot.",
            () -> context.gameStateService().getCrosshairTarget()
        ));

        resources.add(context.resource(
            "meteor://state/entities",
            "Nearby Entities",
            "Nearby entities around the player.",
            () -> context.gameStateService().getNearbyEntities(32D, 64)
        ));

        resources.add(context.resource(
            "meteor://state/pathing",
            "Pathing Status",
            "Current pathing manager status.",
            () -> context.pathingService().getStatus()
        ));

        resources.add(context.resource(
            "meteor://state/screen-dom",
            "Screen DOM",
            "DOM snapshot of the active screen.",
            () -> context.screenDomService().snapshot()
        ));

        resources.add(context.resource(
            "meteor://chat/history",
            "Chat History",
            "Buffered incoming/outgoing chat lines.",
            () -> context.chatLogService().snapshot(200)
        ));
    }
}

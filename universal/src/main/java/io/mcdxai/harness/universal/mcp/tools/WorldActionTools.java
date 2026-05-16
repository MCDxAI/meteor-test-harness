package io.mcdxai.harness.universal.mcp.tools;

import io.mcdxai.harness.universal.mcp.RegistryContext;
import io.mcdxai.harness.universal.mcp.ToolSchemas;
import io.mcdxai.harness.universal.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;
import java.util.Map;

public final class WorldActionTools {
    private WorldActionTools() {
    }

    public static void register(List<McpServerFeatures.SyncToolSpecification> tools, RegistryContext context) {
        tools.add(context.tool(
            "send_chat",
            "Send chat message as player.",
            ToolSchemas.object(Map.of("message", ToolSchemas.stringProperty("Chat message text.")), List.of("message")),
            (exchange, args) -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) return McpResults.error("No local player.");

                ClientPacketListener networkHandler = player.connection;
                if (networkHandler == null) return McpResults.error("Network handler unavailable.");

                String message = args.string("message", "");
                if (message.isBlank()) return McpResults.error("Message cannot be empty.");

                networkHandler.sendChat(message);
                return McpResults.ok("Message sent.");
            }
        ));

        tools.add(context.tool(
            "send_command",
            "Send command as player.",
            ToolSchemas.object(Map.of("command", ToolSchemas.stringProperty("Command with or without leading slash.")), List.of("command")),
            (exchange, args) -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null) return McpResults.error("No local player.");

                ClientPacketListener networkHandler = player.connection;
                if (networkHandler == null) return McpResults.error("Network handler unavailable.");

                String command = args.string("command", "").trim();
                if (command.isEmpty()) return McpResults.error("Command cannot be empty.");
                if (command.startsWith("/")) command = command.substring(1);
                if (command.isEmpty()) return McpResults.error("Command cannot be empty.");

                networkHandler.sendCommand(command);
                return McpResults.ok("Command sent.");
            }
        ));

        tools.add(context.tool("disconnect_world", "Disconnect from current world/server.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                context.harnessService().disconnectToTitle();
                return McpResults.ok(Map.of("inWorld", Minecraft.getInstance().level != null));
            }
        ));
    }
}

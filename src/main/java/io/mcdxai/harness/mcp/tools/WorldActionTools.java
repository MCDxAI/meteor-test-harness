package io.mcdxai.harness.mcp.tools;

import io.mcdxai.harness.mcp.RegistryContext;
import io.mcdxai.harness.mcp.ToolSchemas;
import io.mcdxai.harness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class WorldActionTools {
    private WorldActionTools() {
    }

    public static void register(List<McpServerFeatures.SyncToolSpecification> tools, RegistryContext context) {
        tools.add(context.tool(
            "send_chat",
            "Send chat message as player.",
            ToolSchemas.object(Map.of("message", ToolSchemas.stringProperty("Chat message text.")), List.of("message")),
            (exchange, args) -> {
                ClientPlayerEntity player = mc.player;
                if (player == null) return McpResults.error("No local player.");

                ClientPlayNetworkHandler networkHandler = player.networkHandler;
                if (networkHandler == null) return McpResults.error("Network handler unavailable.");

                String message = args.string("message", "");
                if (message.isBlank()) return McpResults.error("Message cannot be empty.");

                networkHandler.sendChatMessage(message);
                return McpResults.ok("Message sent.");
            }
        ));

        tools.add(context.tool(
            "send_command",
            "Send command as player.",
            ToolSchemas.object(Map.of("command", ToolSchemas.stringProperty("Command with or without leading slash.")), List.of("command")),
            (exchange, args) -> {
                ClientPlayerEntity player = mc.player;
                if (player == null) return McpResults.error("No local player.");

                ClientPlayNetworkHandler networkHandler = player.networkHandler;
                if (networkHandler == null) return McpResults.error("Network handler unavailable.");

                String command = args.string("command", "").trim();
                if (command.isEmpty()) return McpResults.error("Command cannot be empty.");
                if (command.startsWith("/")) command = command.substring(1);
                if (command.isEmpty()) return McpResults.error("Command cannot be empty.");

                networkHandler.sendChatCommand(command);
                return McpResults.ok("Command sent.");
            }
        ));

        tools.add(context.tool("disconnect_world", "Disconnect from current world/server.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                context.harnessService().disconnectToTitle();
                return McpResults.ok(Map.of("inWorld", mc.world != null));
            }
        ));

        tools.add(context.tool(
            "get_chat_history",
            "Get captured chat history.",
            ToolSchemas.object(Map.of("count", ToolSchemas.intProperty("Max lines to return. Default 100.")), List.of()),
            (exchange, args) -> McpResults.ok(Map.of("messages", context.chatLogService().snapshot(args.intValue("count", 100))))
        ));

        tools.add(context.tool("clear_chat_history", "Clear captured chat history buffer.", ToolSchemas.emptyObject(),
            (exchange, args) -> {
                context.chatLogService().clear();
                return McpResults.ok("Chat history cleared.");
            }
        ));
    }
}

package io.mcdxai.harness.services;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.orbit.EventHandler;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChatLogService {
    private final Deque<Map<String, Object>> lines = new ArrayDeque<>();
    private int maxLines;

    public ChatLogService(int maxLines) {
        this.maxLines = Math.max(20, maxLines);
    }

    public synchronized void updateLimit(Setting<Integer> setting) {
        this.maxLines = Math.max(20, setting.get());
        trim();
    }

    public synchronized List<Map<String, Object>> snapshot(int count) {
        int limit = Math.max(1, count);

        List<Map<String, Object>> all = new ArrayList<>(lines);
        int from = Math.max(0, all.size() - limit);

        return new ArrayList<>(all.subList(from, all.size()));
    }

    public synchronized void clear() {
        lines.clear();
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        addLine("incoming", event.getMessage().getString());
    }

    @EventHandler
    private void onSendMessage(SendMessageEvent event) {
        addLine("outgoing", event.message);
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        addLine("system", "Joined world.");
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        addLine("system", "Left world.");
    }

    private synchronized void addLine(String direction, String text) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("timestamp", Instant.now().toString());
        entry.put("direction", direction);
        entry.put("text", text);

        lines.addLast(entry);
        trim();
    }

    private void trim() {
        while (lines.size() > maxLines) {
            lines.removeFirst();
        }
    }
}
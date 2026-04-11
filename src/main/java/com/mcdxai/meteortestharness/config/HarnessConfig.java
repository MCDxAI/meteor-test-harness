package com.mcdxai.meteortestharness.config;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.nbt.NbtCompound;

public final class HarnessConfig extends System<HarnessConfig> {
    public final Settings settings = new Settings();

    private final SettingGroup sgNetwork = settings.createGroup("Network");
    private final SettingGroup sgBehavior = settings.createGroup("Behavior");

    public final Setting<Boolean> autoStart = sgNetwork.add(new BoolSetting.Builder()
        .name("auto-start")
        .description("Automatically start the MCP HTTP server when Meteor initializes.")
        .defaultValue(true)
        .build());

    public final Setting<String> bindHost = sgNetwork.add(new StringSetting.Builder()
        .name("bind-host")
        .description("Host/IP to bind the embedded MCP HTTP server to.")
        .defaultValue("127.0.0.1")
        .build());

    public final Setting<Integer> bindPort = sgNetwork.add(new IntSetting.Builder()
        .name("bind-port")
        .description("Port for the MCP HTTP endpoint.")
        .defaultValue(38861)
        .min(1024)
        .max(65535)
        .sliderRange(1024, 65535)
        .build());

    public final Setting<String> mcpEndpoint = sgNetwork.add(new StringSetting.Builder()
        .name("mcp-endpoint")
        .description("HTTP endpoint path exposed by the MCP server.")
        .defaultValue("/mcp")
        .build());

    public final Setting<Integer> keepAliveSeconds = sgNetwork.add(new IntSetting.Builder()
        .name("keep-alive-seconds")
        .description("SSE keep-alive interval in seconds.")
        .defaultValue(30)
        .min(5)
        .max(300)
        .sliderRange(5, 120)
        .build());

    public final Setting<Integer> requestTimeoutSeconds = sgNetwork.add(new IntSetting.Builder()
        .name("request-timeout-seconds")
        .description("Maximum time for a single MCP tool call execution.")
        .defaultValue(30)
        .min(5)
        .max(300)
        .sliderRange(5, 120)
        .build());

    public final Setting<Boolean> singleSessionMode = sgBehavior.add(new BoolSetting.Builder()
        .name("single-session-mode")
        .description("Allow one active MCP session owner at a time.")
        .defaultValue(true)
        .build());

    public final Setting<Integer> chatHistoryLimit = sgBehavior.add(new IntSetting.Builder()
        .name("chat-history-limit")
        .description("Maximum number of captured chat lines retained in memory.")
        .defaultValue(200)
        .min(20)
        .max(2000)
        .sliderRange(50, 500)
        .build());

    public HarnessConfig() {
        super("meteor-test-harness-config");
    }

    public static HarnessConfig get() {
        return Systems.get(HarnessConfig.class);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("settings", settings.toTag());
        return tag;
    }

    @Override
    public HarnessConfig fromTag(NbtCompound tag) {
        if (tag.contains("settings")) {
            settings.fromTag(tag.getCompoundOrEmpty("settings"));
        }
        return this;
    }
}
package com.mcdxai.meteortestharness.services;

import meteordevelopment.meteorclient.pathing.IPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class PathingService {
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        boolean inWorld = mc.player != null && mc.world != null;
        status.put("inWorld", inWorld);

        if (!inWorld) {
            status.put("manager", null);
            status.put("isPathing", false);
            return status;
        }

        IPathManager pathManager;
        try {
            pathManager = PathManagers.get();
        } catch (Exception ignored) {
            status.put("manager", null);
            status.put("isPathing", false);
            status.put("error", "path_manager_unavailable");
            return status;
        }

        if (pathManager == null) {
            status.put("manager", null);
            status.put("isPathing", false);
            status.put("error", "path_manager_null");
            return status;
        }

        status.put("manager", pathManager.getName());
        status.put("isPathing", pathManager.isPathing());
        status.put("targetYaw", pathManager.getTargetYaw());
        status.put("targetPitch", pathManager.getTargetPitch());

        IPathManager.ISettings settings = pathManager.getSettings();
        if (settings != null) {
            status.put("walkOnWater", settings.getWalkOnWater().get());
            status.put("walkOnLava", settings.getWalkOnLava().get());
            status.put("step", settings.getStep().get());
            status.put("noFall", settings.getNoFall().get());
        }

        return status;
    }

    public boolean moveTo(int x, int y, int z, boolean ignoreY) {
        if (!canPath()) return false;
        PathManagers.get().moveTo(new BlockPos(x, y, z), ignoreY);
        return true;
    }

    public boolean moveInDirection(float yaw) {
        if (!canPath()) return false;
        PathManagers.get().moveInDirection(yaw);
        return true;
    }

    public boolean pause() {
        if (!canPath()) return false;
        PathManagers.get().pause();
        return true;
    }

    public boolean resume() {
        if (!canPath()) return false;
        PathManagers.get().resume();
        return true;
    }

    public boolean stop() {
        if (!canPath()) return false;
        PathManagers.get().stop();
        return true;
    }

    private boolean canPath() {
        return mc.player != null && mc.world != null;
    }
}

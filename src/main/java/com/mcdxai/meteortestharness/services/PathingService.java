package com.mcdxai.meteortestharness.services;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.event.events.PathEvent;
import baritone.api.event.events.TickEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.pathing.goals.Goal;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import com.mcdxai.meteortestharness.util.MainThreadInvoker;
import meteordevelopment.meteorclient.pathing.IPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import net.minecraft.util.math.BlockPos;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class PathingService {
    private static final long WAIT_POLL_INTERVAL_MS = 50L;
    private static final long STARTUP_GRACE_MS = 250L;
    private static final int ACTION_HISTORY_LIMIT = 128;
    private static final Duration SNAPSHOT_TIMEOUT = Duration.ofSeconds(3);

    private final Object actionLock = new Object();
    private final LinkedHashMap<String, PathingAction> actionsById = new LinkedHashMap<>();
    private final AbstractGameEventListener baritoneListener;

    private long nextActionNumber = 1L;
    private PathingAction activeAction;
    private boolean baritoneListenerRegistered;

    public PathingService() {
        this.baritoneListener = new AbstractGameEventListener() {
            @Override
            public void onPathEvent(PathEvent event) {
                handlePathEvent(event);
            }

            @Override
            public void onTick(TickEvent event) {
                if (event.getType() == TickEvent.Type.OUT) {
                    handleWorldUnload();
                }
            }
        };

        tryRegisterBaritoneListener();
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        boolean inWorld = mc.player != null && mc.world != null;
        status.put("inWorld", inWorld);

        if (!inWorld) {
            status.put("manager", null);
            status.put("isPathing", false);
            status.put("baritoneListenerRegistered", baritoneListenerRegistered);
            synchronized (actionLock) {
                if (activeAction != null) status.put("activeAction", toActionMapLocked(activeAction));
            }
            return status;
        }

        IPathManager pathManager = safePathManager();
        if (pathManager == null) {
            status.put("manager", null);
            status.put("isPathing", false);
            status.put("error", "path_manager_unavailable");
            status.put("baritoneListenerRegistered", baritoneListenerRegistered);
            synchronized (actionLock) {
                if (activeAction != null) status.put("activeAction", toActionMapLocked(activeAction));
            }
            return status;
        }

        status.put("manager", pathManager.getName());
        status.put("isPathing", pathManager.isPathing());
        status.put("targetYaw", pathManager.getTargetYaw());
        status.put("targetPitch", pathManager.getTargetPitch());
        status.put("baritoneListenerRegistered", baritoneListenerRegistered);

        IPathManager.ISettings settings = pathManager.getSettings();
        if (settings != null) {
            status.put("walkOnWater", settings.getWalkOnWater().get());
            status.put("walkOnLava", settings.getWalkOnLava().get());
            status.put("step", settings.getStep().get());
            status.put("noFall", settings.getNoFall().get());
        }

        synchronized (actionLock) {
            if (activeAction != null) status.put("activeAction", toActionMapLocked(activeAction));
        }

        return status;
    }

    public Map<String, Object> startMoveTo(int x, int y, int z, boolean ignoreY) {
        if (!canPath()) return null;

        IPathManager pathManager = safePathManager();
        if (pathManager == null) return null;

        PathingAction action = beginAction("move_to", "finite", pathManager.getName());
        synchronized (actionLock) {
            action.targetX = x;
            action.targetY = y;
            action.targetZ = z;
            action.ignoreY = ignoreY;
        }

        pathManager.moveTo(new BlockPos(x, y, z), ignoreY);
        refreshActionState(action);

        Map<String, Object> ack = new LinkedHashMap<>();
        ack.put("success", true);
        ack.put("action_id", action.actionId);
        ack.put("target", Map.of("x", x, "y", y, "z", z));
        ack.put("ignoreY", ignoreY);
        ack.put("mode", "finite");
        ack.put("message", "Pathing request submitted");
        return ack;
    }

    public Map<String, Object> startMoveInDirection(float yaw) {
        if (!canPath()) return null;

        IPathManager pathManager = safePathManager();
        if (pathManager == null) return null;

        PathingAction action = beginAction("move_in_direction", "continuous", pathManager.getName());
        synchronized (actionLock) {
            action.yaw = yaw;
        }

        pathManager.moveInDirection(yaw);
        refreshActionState(action);

        Map<String, Object> ack = new LinkedHashMap<>();
        ack.put("success", true);
        ack.put("action_id", action.actionId);
        ack.put("yaw", yaw);
        ack.put("mode", "continuous");
        ack.put("message", "Direction pathing request submitted");
        return ack;
    }

    public boolean pause() {
        if (!canPath()) return false;
        IPathManager pathManager = safePathManager();
        if (pathManager == null) return false;

        pathManager.pause();
        synchronized (actionLock) {
            if (activeAction != null && !activeAction.terminal) {
                activeAction.phase = "paused";
                actionLock.notifyAll();
            }
        }
        return true;
    }

    public boolean resume() {
        if (!canPath()) return false;
        IPathManager pathManager = safePathManager();
        if (pathManager == null) return false;

        pathManager.resume();
        synchronized (actionLock) {
            if (activeAction != null && !activeAction.terminal) {
                activeAction.phase = "executing";
                actionLock.notifyAll();
            }
        }
        return true;
    }

    public boolean stop() {
        if (!canPath()) return false;
        IPathManager pathManager = safePathManager();
        if (pathManager == null) return false;

        pathManager.stop();
        synchronized (actionLock) {
            terminateLocked(activeAction, "canceled", "manual_stop");
        }
        return true;
    }

    public Map<String, Object> waitForAction(String actionId, int timeoutMs, boolean returnOnPaused) {
        long startedAt = System.currentTimeMillis();
        String condition = "terminal";

        PathingAction action;
        synchronized (actionLock) {
            if (actionId == null || actionId.isBlank()) {
                action = activeAction;
                if (action == null) return waitResult("no_active_action", 0, null, condition);
            } else {
                action = actionsById.get(actionId);
                if (action == null) return waitResult("invalid_action_id", 0, null, condition);
            }
        }

        if (!isBaritoneManager(action.manager)) {
            refreshActionState(action);
            return waitResult("unsupported_manager", 0, action, condition);
        }

        while (true) {
            refreshActionState(action);

            synchronized (actionLock) {
                long waitedMs = Math.max(0L, System.currentTimeMillis() - startedAt);
                if (action.terminal) {
                    return waitResult("completed", waitedMs, action, "terminal");
                }

                if (returnOnPaused && "paused".equals(action.phase)) {
                    return waitResult("completed", waitedMs, action, "paused");
                }

                long remainingMs = timeoutMs - waitedMs;
                if (remainingMs <= 0) {
                    return waitResult("timeout", waitedMs, action, condition);
                }

                try {
                    actionLock.wait(Math.min(WAIT_POLL_INTERVAL_MS, remainingMs));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return waitResult("timeout", waitedMs, action, condition);
                }
            }
        }
    }

    private void tryRegisterBaritoneListener() {
        try {
            IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            baritone.getGameEventHandler().registerEventListener(baritoneListener);
            baritoneListenerRegistered = true;
        } catch (Throwable ignored) {
            baritoneListenerRegistered = false;
        }
    }

    private PathingAction beginAction(String kind, String mode, String manager) {
        synchronized (actionLock) {
            if (activeAction != null && !activeAction.terminal) {
                terminateLocked(activeAction, "superseded", "new_action_started");
            }

            String actionId = "path-" + nextActionNumber++;
            PathingAction action = new PathingAction(actionId, kind, mode, manager);
            actionsById.put(action.actionId, action);
            trimHistoryLocked();
            activeAction = action;
            actionLock.notifyAll();
            return action;
        }
    }

    private void trimHistoryLocked() {
        while (actionsById.size() > ACTION_HISTORY_LIMIT) {
            String firstKey = actionsById.keySet().iterator().next();
            if (activeAction != null && firstKey.equals(activeAction.actionId)) break;
            actionsById.remove(firstKey);
        }
    }

    private void handleWorldUnload() {
        synchronized (actionLock) {
            terminateLocked(activeAction, "interrupted", "world_unloaded");
        }
    }

    private void handlePathEvent(PathEvent event) {
        synchronized (actionLock) {
            if (activeAction == null || activeAction.terminal) return;

            activeAction.lastPathEvent = event.name();
            switch (event) {
                case CALC_STARTED:
                case NEXT_SEGMENT_CALC_STARTED:
                case PATH_FINISHED_NEXT_STILL_CALCULATING:
                    activeAction.phase = "calculating";
                    actionLock.notifyAll();
                    break;
                case CALC_FINISHED_NOW_EXECUTING:
                case NEXT_SEGMENT_CALC_FINISHED:
                case CONTINUING_ONTO_PLANNED_NEXT:
                case SPLICING_ONTO_NEXT_EARLY:
                    activeAction.phase = "executing";
                    actionLock.notifyAll();
                    break;
                case CALC_FAILED:
                    terminateLocked(activeAction, "failed", "calc_failed");
                    break;
                case AT_GOAL:
                    if ("finite".equals(activeAction.mode)) {
                        terminateLocked(activeAction, "succeeded", "at_goal");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void refreshActionState(PathingAction action) {
        if (action == null || action.terminal) return;

        try {
            if (mc.isOnThread()) {
                refreshActionStateOnMainThread(action);
            } else {
                MainThreadInvoker.run(() -> refreshActionStateOnMainThread(action), SNAPSHOT_TIMEOUT);
            }
        } catch (Exception ignored) {
            synchronized (actionLock) {
                if (!action.terminal) {
                    terminateLocked(action, "interrupted", "snapshot_failed");
                }
            }
        }
    }

    private void refreshActionStateOnMainThread(PathingAction action) {
        if (action == null || action.terminal) return;

        if (!canPath()) {
            synchronized (actionLock) {
                terminateLocked(action, "interrupted", "world_unloaded");
            }
            return;
        }

        IPathManager manager = safePathManager();
        if (manager == null) {
            synchronized (actionLock) {
                terminateLocked(action, "interrupted", "path_manager_unavailable");
            }
            return;
        }

        String managerName = manager.getName();
        if (!managerName.equalsIgnoreCase(action.manager)) {
            synchronized (actionLock) {
                terminateLocked(action, "interrupted", "manager_changed");
            }
            return;
        }

        if (!isBaritoneManager(managerName)) {
            synchronized (actionLock) {
                if (action.terminal) return;

                action.isPathing = manager.isPathing();
                action.hasPath = action.isPathing;
                action.calcInProgress = false;
                action.processName = null;
                action.processTemporary = null;
                action.commandType = null;
                action.phase = action.isPathing ? "executing" : "submitted";
                actionLock.notifyAll();
            }
            return;
        }

        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        IPathingBehavior pathingBehavior = baritone.getPathingBehavior();
        boolean isPathing = pathingBehavior.isPathing();
        boolean hasPath = pathingBehavior.hasPath();
        boolean calcInProgress = pathingBehavior.getInProgress().isPresent();

        Optional<IBaritoneProcess> processOptional = baritone.getPathingControlManager().mostRecentInControl();
        IBaritoneProcess process = processOptional.orElse(null);
        String processName = process == null ? null : process.getClass().getSimpleName();
        Boolean processTemporary = process == null ? null : process.isTemporary();

        Optional<PathingCommand> commandOptional = baritone.getPathingControlManager().mostRecentCommand();
        PathingCommand command = commandOptional.orElse(null);
        String commandType = command == null ? null : command.commandType.name();

        Goal customGoal = baritone.getCustomGoalProcess().getGoal();
        boolean goalReached = isActionGoalReached(action);
        long ageMs = Math.max(0L, System.currentTimeMillis() - action.startedAtMillis);

        synchronized (actionLock) {
            if (action.terminal) return;

            action.isPathing = isPathing;
            action.hasPath = hasPath;
            action.calcInProgress = calcInProgress;
            action.processName = processName;
            action.processTemporary = processTemporary;
            action.commandType = commandType;

            if ("finite".equals(action.mode) && goalReached) {
                terminateLocked(action, "succeeded", "at_goal");
                return;
            }

            if (hasPath && !isPathing) {
                action.phase = "paused";
            } else if (isPathing) {
                action.phase = "executing";
            } else if (calcInProgress) {
                action.phase = "calculating";
            } else {
                action.phase = "submitted";
            }

            boolean preempted = process != null && !process.isTemporary() && !"CustomGoalProcess".equals(processName);
            if (preempted) {
                terminateLocked(action, "interrupted", "process_preempted");
                return;
            }

            if ("finite".equals(action.mode)
                && ageMs >= STARTUP_GRACE_MS
                && !isPathing
                && !hasPath
                && !calcInProgress
                && customGoal == null) {
                terminateLocked(action, "failed", "pathing_stopped_before_goal");
                return;
            }

            actionLock.notifyAll();
        }
    }

    private boolean isActionGoalReached(PathingAction action) {
        if (action == null || !"move_to".equals(action.kind) || mc.player == null) return false;
        if (action.targetX == null || action.targetY == null || action.targetZ == null) return false;

        BlockPos playerPos = mc.player.getBlockPos();
        if (action.ignoreY) {
            return playerPos.getX() == action.targetX && playerPos.getZ() == action.targetZ;
        }

        return playerPos.getX() == action.targetX
            && playerPos.getY() == action.targetY
            && playerPos.getZ() == action.targetZ;
    }

    private void terminateLocked(PathingAction action, String finalState, String reasonCode) {
        if (action == null || action.terminal) return;

        action.terminal = true;
        action.phase = "terminal";
        action.finalState = finalState;
        action.reasonCode = reasonCode;
        action.endedAt = Instant.now();
        action.endedAtMillis = System.currentTimeMillis();

        if (activeAction == action) activeAction = null;
        actionLock.notifyAll();
    }

    private Map<String, Object> waitResult(String status, long waitedMs, PathingAction action, String completionCondition) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", status);
        result.put("waited_ms", waitedMs);
        result.put("completion_condition", completionCondition);

        if (action != null) {
            synchronized (actionLock) {
                result.put("action", toActionMapLocked(action));
            }
        }

        return result;
    }

    private Map<String, Object> toActionMapLocked(PathingAction action) {
        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("action_id", action.actionId);
        mapped.put("kind", action.kind);
        mapped.put("mode", action.mode);
        mapped.put("phase", action.phase);
        mapped.put("terminal", action.terminal);
        mapped.put("final_state", action.finalState);
        mapped.put("reason_code", action.reasonCode);
        mapped.put("last_path_event", action.lastPathEvent);
        mapped.put("manager", action.manager);
        mapped.put("is_pathing", action.isPathing);
        mapped.put("has_path", action.hasPath);
        mapped.put("calc_in_progress", action.calcInProgress);
        mapped.put("process_name", action.processName);
        mapped.put("process_temporary", action.processTemporary);
        mapped.put("command_type", action.commandType);
        mapped.put("started_at", action.startedAt.toString());
        mapped.put("ended_at", action.endedAt == null ? null : action.endedAt.toString());

        if ("move_to".equals(action.kind)
            && action.targetX != null
            && action.targetY != null
            && action.targetZ != null) {
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("x", action.targetX);
            target.put("y", action.targetY);
            target.put("z", action.targetZ);
            mapped.put("target", target);
            mapped.put("ignoreY", action.ignoreY);
        }

        if ("move_in_direction".equals(action.kind) && action.yaw != null) {
            mapped.put("yaw", action.yaw);
        }

        return mapped;
    }

    private IPathManager safePathManager() {
        try {
            return PathManagers.get();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean canPath() {
        return mc.player != null && mc.world != null;
    }

    private static boolean isBaritoneManager(IPathManager manager) {
        return manager != null && isBaritoneManager(manager.getName());
    }

    private static boolean isBaritoneManager(String managerName) {
        return managerName != null && "Baritone".equalsIgnoreCase(managerName);
    }

    private static final class PathingAction {
        private final String actionId;
        private final String kind;
        private final String mode;
        private final String manager;
        private final Instant startedAt;
        private final long startedAtMillis;

        private Integer targetX;
        private Integer targetY;
        private Integer targetZ;
        private boolean ignoreY;
        private Float yaw;

        private String phase = "submitted";
        private boolean terminal;
        private String finalState;
        private String reasonCode;
        private String lastPathEvent;
        private Instant endedAt;
        private long endedAtMillis;

        private boolean isPathing;
        private boolean hasPath;
        private boolean calcInProgress;
        private String processName;
        private Boolean processTemporary;
        private String commandType;

        private PathingAction(String actionId, String kind, String mode, String manager) {
            this.actionId = actionId;
            this.kind = kind;
            this.mode = mode;
            this.manager = manager;
            this.startedAt = Instant.now();
            this.startedAtMillis = System.currentTimeMillis();
        }
    }
}

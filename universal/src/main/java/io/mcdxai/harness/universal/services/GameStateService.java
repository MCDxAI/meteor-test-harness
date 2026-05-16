package io.mcdxai.harness.universal.services;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GameStateService {

    public Map<String, Object> getPlayerState() {
        Map<String, Object> state = new LinkedHashMap<>();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel world = mc.level;

        state.put("inWorld", player != null && world != null);
        if (player == null || world == null) return state;

        state.put("name", player.getName().getString());
        state.put("uuid", player.getStringUUID());
        state.put("position", vec(player.getX(), player.getY(), player.getZ()));
        state.put("blockPosition", blockPos(player.blockPosition()));
        state.put("velocity", vec(player.getDeltaMovement()));
        state.put("yaw", player.getYRot());
        state.put("pitch", player.getXRot());
        state.put("health", player.getHealth());
        state.put("maxHealth", player.getMaxHealth());
        state.put("absorption", player.getAbsorptionAmount());
        state.put("hunger", player.getFoodData().getFoodLevel());
        state.put("saturation", player.getFoodData().getSaturationLevel());
        state.put("experienceLevel", player.experienceLevel);
        state.put("experienceProgress", player.experienceProgress);
        state.put("air", player.getAirSupply());
        state.put("maxAir", player.getMaxAirSupply());
        state.put("onGround", player.onGround());
        state.put("sneaking", player.isShiftKeyDown());
        state.put("sprinting", player.isSprinting());
        state.put("swimming", player.isSwimming());
        state.put("fallFlying", player.isFallFlying());
        state.put("flying", player.getAbilities().flying);
        state.put("mayFly", player.getAbilities().mayfly);
        state.put("usingItem", player.isUsingItem());
        state.put("gameMode", resolveGameType(player));
        state.put("effects", effects(player));
        return state;
    }

    public Map<String, Object> getWorldState() {
        Map<String, Object> state = new LinkedHashMap<>();
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;
        LocalPlayer player = mc.player;

        state.put("inWorld", world != null);
        state.put("singleplayer", mc.hasSingleplayerServer());

        if (world == null) return state;

        ResourceKey<?> key = world.dimension();
        Identifier dimensionId = key == null ? null : key.identifier();

        state.put("dimension", dimensionId == null ? "unknown" : dimensionId.toString());
        state.put("time", world.getGameTime());
        state.put("timeOfDay", world.getGameTime() % 24000L);
        state.put("raining", world.isRaining());
        state.put("thundering", world.isThundering());
        state.put("rainGradient", world.getRainLevel(1.0F));
        state.put("thunderGradient", world.getThunderLevel(1.0F));
        state.put("difficulty", world.getDifficulty().name());
        state.put("players", world.players().size());

        if (player != null) {
            BlockPos pos = player.blockPosition();
            BlockState blockState = world.getBlockState(pos);
            state.put("playerBlock", blockState.getBlock().getName().getString());
            state.put("playerBiome", world.getBiome(pos).unwrapKey().map(k -> k.identifier().toString()).orElse("unknown"));
        }

        return state;
    }

    public Map<String, Object> getPlayerInventory(String section, int row, int slotStart, int slotEnd, boolean includeEmpty) {
        Map<String, Object> state = new LinkedHashMap<>();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            state.put("inWorld", false);
            return state;
        }

        var inventory = player.getInventory();
        int selectedSlot = player.getInventory().getSelectedSlot();
        String normalizedSection = normalizeInventorySection(section);

        state.put("inWorld", true);
        state.put("section", normalizedSection);
        state.put("includeEmpty", includeEmpty);
        state.put("selectedSlot", selectedSlot);
        state.put("inventorySize", inventory.getContainerSize());

        List<Map<String, Object>> slots = new ArrayList<>();

        if (normalizedSection.equals("offhand")) {
            int index = 40;
            if (index < inventory.getContainerSize()) appendInventorySlot(slots, inventory.getItem(index), index, selectedSlot, includeEmpty);
        } else if (normalizedSection.equals("armor")) {
            for (int i = 36; i <= 39 && i < inventory.getContainerSize(); i++) {
                appendInventorySlot(slots, inventory.getItem(i), i, selectedSlot, includeEmpty);
            }
        } else if (normalizedSection.equals("hands")) {
            state.put("mainHand", itemStack(player.getMainHandItem()));
            state.put("offHand", itemStack(player.getOffhandItem()));
        } else {
            int[] range = resolveInventoryRange(normalizedSection, row, slotStart, slotEnd, selectedSlot, inventory.getContainerSize());
            state.put("slotRange", Map.of("start", range[0], "end", range[1]));
            if (normalizedSection.equals("row")) state.put("row", Math.max(0, Math.min(2, row)));

            for (int i = range[0]; i <= range[1] && i < inventory.getContainerSize(); i++) {
                appendInventorySlot(slots, inventory.getItem(i), i, selectedSlot, includeEmpty);
            }
        }

        state.put("count", slots.size());
        state.put("slots", slots);
        return state;
    }

    public Map<String, Object> getCrosshairTarget() {
        Minecraft mc = Minecraft.getInstance();
        HitResult target = mc.hitResult;
        if (target == null) return Map.of("type", "none");

        Map<String, Object> map = new LinkedHashMap<>();
        HitResult.Type type = target.getType();
        map.put("type", type.name().toLowerCase());
        map.put("position", vec(target.getLocation()));

        if (type == HitResult.Type.BLOCK && target instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            map.put("blockPos", blockPos(pos));
            if (mc.level != null) {
                BlockState state = mc.level.getBlockState(pos);
                map.put("block", state.getBlock().getName().getString());
            }
        } else if (type == HitResult.Type.ENTITY && target instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            map.put("entityType", String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())));
            map.put("entityName", entity.getName().getString());
            map.put("entityId", entity.getId());
            map.put("entityPos", vec(entity.getX(), entity.getY(), entity.getZ()));
        }

        return map;
    }

    public Map<String, Object> getNearbyEntities(double radius, int maxCount) {
        Map<String, Object> state = new LinkedHashMap<>();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel world = mc.level;
        state.put("inWorld", player != null && world != null);

        if (player == null || world == null) {
            state.put("entities", List.of());
            return state;
        }

        double clampedRadius = Math.max(1D, Math.min(256D, radius));
        int clampedMaxCount = Math.max(1, Math.min(512, maxCount));

        List<Map<String, Object>> entities = new ArrayList<>();
        double originX = player.getX();
        double originY = player.getY();
        double originZ = player.getZ();

        for (Entity entity : world.entitiesForRendering()) {
            if (entity == player) continue;
            double dx = entity.getX() - originX;
            double dy = entity.getY() - originY;
            double dz = entity.getZ() - originZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > clampedRadius) continue;

            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("id", entity.getId());
            mapped.put("uuid", entity.getStringUUID());
            mapped.put("name", entity.getName().getString());
            mapped.put("type", String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())));
            mapped.put("position", vec(entity.getX(), entity.getY(), entity.getZ()));
            mapped.put("blockPosition", blockPos(entity.blockPosition()));
            mapped.put("velocity", vec(entity.getDeltaMovement()));
            mapped.put("distance", distance);
            mapped.put("onGround", entity.onGround());
            mapped.put("submergedInWater", entity.isUnderWater());

            if (entity instanceof LivingEntity living) {
                mapped.put("health", living.getHealth());
                mapped.put("maxHealth", living.getMaxHealth());
            }

            entities.add(mapped);
        }

        entities.sort(Comparator.comparingDouble(entry -> ((Number) entry.get("distance")).doubleValue()));
        if (entities.size() > clampedMaxCount) entities = new ArrayList<>(entities.subList(0, clampedMaxCount));

        state.put("radius", clampedRadius);
        state.put("count", entities.size());
        state.put("entities", entities);
        return state;
    }

    private List<Map<String, Object>> effects(LocalPlayer player) {
        List<Map<String, Object>> effects = new ArrayList<>();
        for (MobEffectInstance instance : player.getActiveEffects()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("effect", instance.getEffect().unwrapKey().map(k -> k.identifier().toString()).orElse("unknown"));
            data.put("amplifier", instance.getAmplifier());
            data.put("duration", instance.getDuration());
            data.put("ambient", instance.isAmbient());
            data.put("showParticles", instance.isVisible());
            effects.add(data);
        }
        return effects;
    }

    private Map<String, Object> itemStack(ItemStack stack) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("itemId", itemId == null ? "unknown" : itemId.toString());
        data.put("count", stack.getCount());
        data.put("empty", stack.isEmpty());
        data.put("name", stack.getHoverName().getString());
        data.put("damage", stack.getDamageValue());
        data.put("maxDamage", stack.getMaxDamage());
        data.put("damageable", stack.isDamageableItem());
        data.put("enchanted", stack.isEnchanted());
        return data;
    }

    private Map<String, Object> vec(Vec3 vec) {
        return Map.of("x", vec.x, "y", vec.y, "z", vec.z);
    }

    private Map<String, Object> vec(double x, double y, double z) {
        return Map.of("x", x, "y", y, "z", z);
    }

    private Map<String, Object> blockPos(BlockPos pos) {
        return Map.of("x", pos.getX(), "y", pos.getY(), "z", pos.getZ());
    }

    private void appendInventorySlot(List<Map<String, Object>> slots, ItemStack stack, int slot, int selectedSlot, boolean includeEmpty) {
        if (!includeEmpty && stack.isEmpty()) return;

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("slot", slot);
        entry.put("container", inventoryContainer(slot));
        entry.put("selected", slot == selectedSlot);

        if (slot >= 0 && slot <= 8) {
            entry.put("hotbarIndex", slot);
        } else if (slot >= 9 && slot <= 35) {
            int local = slot - 9;
            entry.put("row", local / 9);
            entry.put("column", local % 9);
        }

        entry.put("item", itemStack(stack));
        slots.add(entry);
    }

    private String inventoryContainer(int slot) {
        if (slot >= 0 && slot <= 8) return "hotbar";
        if (slot >= 9 && slot <= 35) return "main";
        if (slot >= 36 && slot <= 39) return "armor";
        if (slot == 40) return "offhand";
        return "unknown";
    }

    private String normalizeInventorySection(String section) {
        if (section == null || section.isBlank()) return "all";
        return switch (section.trim().toLowerCase()) {
            case "all", "hotbar", "main", "inventory", "armor", "offhand", "hands", "row", "range", "selected" -> section.trim().toLowerCase();
            case "storage" -> "main";
            default -> "all";
        };
    }

    private int[] resolveInventoryRange(String section, int row, int slotStart, int slotEnd, int selectedSlot, int inventorySize) {
        int maxSlot = Math.max(0, inventorySize - 1);
        return switch (section) {
            case "hotbar" -> new int[]{0, Math.min(8, maxSlot)};
            case "main" -> new int[]{Math.min(9, maxSlot), Math.min(35, maxSlot)};
            case "inventory" -> new int[]{0, Math.min(35, maxSlot)};
            case "selected" -> new int[]{Math.max(0, Math.min(selectedSlot, maxSlot)), Math.max(0, Math.min(selectedSlot, maxSlot))};
            case "row" -> {
                int clampedRow = Math.max(0, Math.min(2, row));
                int start = 9 + clampedRow * 9;
                int end = start + 8;
                yield new int[]{Math.min(start, maxSlot), Math.min(end, maxSlot)};
            }
            case "range" -> {
                int start = Math.max(0, Math.min(slotStart, maxSlot));
                int end = Math.max(0, Math.min(slotEnd, maxSlot));
                if (slotStart < 0 && slotEnd < 0) yield new int[]{0, maxSlot};
                if (slotStart < 0) start = 0;
                if (slotEnd < 0) end = maxSlot;
                if (start > end) {
                    int temp = start;
                    start = end;
                    end = temp;
                }
                yield new int[]{start, end};
            }
            default -> new int[]{0, maxSlot};
        };
    }

    private String resolveGameType(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return "unknown";
        PlayerInfo entry = mc.getConnection().getPlayerInfo(player.getUUID());
        if (entry == null) return "unknown";
        GameType gameMode = entry.getGameMode();
        if (gameMode == null) return "unknown";
        return gameMode.getName();
    }
    @SuppressWarnings("unused")
    private List<Object> armor(LocalPlayer player) {
        List<Object> armor = new ArrayList<>(4);
        armor.add(itemStack(player.getItemBySlot(EquipmentSlot.HEAD)));
        armor.add(itemStack(player.getItemBySlot(EquipmentSlot.CHEST)));
        armor.add(itemStack(player.getItemBySlot(EquipmentSlot.LEGS)));
        armor.add(itemStack(player.getItemBySlot(EquipmentSlot.FEET)));
        return armor;
    }
}

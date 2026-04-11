package com.mcdxai.meteortestharness.services;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class GameStateService {
    public Map<String, Object> getPlayerState() {
        Map<String, Object> state = new LinkedHashMap<>();

        ClientPlayerEntity player = mc.player;
        ClientWorld world = mc.world;

        state.put("inWorld", player != null && world != null);
        if (player == null || world == null) {
            return state;
        }

        state.put("name", player.getName().getString());
        state.put("uuid", player.getUuidAsString());
        state.put("position", vec(player.getX(), player.getY(), player.getZ()));
        state.put("blockPosition", blockPos(player.getBlockPos()));
        state.put("velocity", vec(player.getVelocity()));
        state.put("yaw", player.getYaw());
        state.put("pitch", player.getPitch());
        state.put("health", player.getHealth());
        state.put("maxHealth", player.getMaxHealth());
        state.put("absorption", player.getAbsorptionAmount());
        state.put("hunger", player.getHungerManager().getFoodLevel());
        state.put("saturation", player.getHungerManager().getSaturationLevel());
        state.put("experienceLevel", player.experienceLevel);
        state.put("experienceProgress", player.experienceProgress);
        state.put("air", player.getAir());
        state.put("maxAir", player.getMaxAir());
        state.put("onGround", player.isOnGround());
        state.put("sneaking", player.isSneaking());
        state.put("sprinting", player.isSprinting());
        state.put("swimming", player.isSwimming());
        state.put("fallFlying", player.isGliding());
        state.put("usingItem", player.isUsingItem());

        state.put("mainHand", itemStack(player.getMainHandStack()));
        state.put("offHand", itemStack(player.getOffHandStack()));
        state.put("armor", armor(player));
        state.put("inventory", inventory(player));
        state.put("effects", effects(player));
        state.put("crosshairTarget", crosshairTarget());

        return state;
    }

    public Map<String, Object> getWorldState() {
        Map<String, Object> state = new LinkedHashMap<>();

        ClientWorld world = mc.world;
        ClientPlayerEntity player = mc.player;

        state.put("inWorld", world != null);
        state.put("singleplayer", mc.getServer() != null);

        if (world == null) {
            return state;
        }

        RegistryKey<?> key = world.getRegistryKey();
        Identifier dimensionId = key == null ? null : key.getValue();

        state.put("dimension", dimensionId == null ? "unknown" : dimensionId.toString());
        state.put("time", world.getTime());
        state.put("timeOfDay", world.getTimeOfDay());
        state.put("raining", world.isRaining());
        state.put("thundering", world.isThundering());
        state.put("rainGradient", world.getRainGradient(1.0F));
        state.put("thunderGradient", world.getThunderGradient(1.0F));
        state.put("difficulty", world.getDifficulty().name());
        state.put("players", world.getPlayers().size());

        if (player != null) {
            BlockPos pos = player.getBlockPos();
            BlockState blockState = world.getBlockState(pos);
            state.put("playerBlock", blockState.getBlock().getName().getString());
            state.put("playerBiome", world.getBiome(pos).getKey().map(k -> k.getValue().toString()).orElse("unknown"));
        }

        return state;
    }

    public Map<String, Object> getInventoryState() {
        Map<String, Object> state = new LinkedHashMap<>();

        ClientPlayerEntity player = mc.player;
        if (player == null) {
            state.put("inWorld", false);
            return state;
        }

        state.put("inWorld", true);
        state.put("selectedSlot", resolveSelectedSlot(player));
        state.put("main", inventory(player));
        state.put("mainHand", itemStack(player.getMainHandStack()));
        state.put("offHand", itemStack(player.getOffHandStack()));
        state.put("armor", armor(player));

        return state;
    }

    public Map<String, Object> getNearbyEntities(double radius, int maxCount) {
        Map<String, Object> state = new LinkedHashMap<>();

        ClientPlayerEntity player = mc.player;
        ClientWorld world = mc.world;
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

        for (Entity entity : world.getEntities()) {
            if (entity == player) continue;

            double dx = entity.getX() - originX;
            double dy = entity.getY() - originY;
            double dz = entity.getZ() - originZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > clampedRadius) continue;

            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("id", entity.getId());
            mapped.put("uuid", entity.getUuidAsString());
            mapped.put("name", entity.getName().getString());
            mapped.put("type", String.valueOf(Registries.ENTITY_TYPE.getId(entity.getType())));
            mapped.put("position", vec(entity.getX(), entity.getY(), entity.getZ()));
            mapped.put("blockPosition", blockPos(entity.getBlockPos()));
            mapped.put("velocity", vec(entity.getVelocity()));
            mapped.put("distance", distance);
            mapped.put("onGround", entity.isOnGround());
            mapped.put("submergedInWater", entity.isSubmergedInWater());

            if (entity instanceof LivingEntity livingEntity) {
                mapped.put("health", livingEntity.getHealth());
                mapped.put("maxHealth", livingEntity.getMaxHealth());
            }

            entities.add(mapped);
        }

        entities.sort(Comparator.comparingDouble(entry -> ((Number) entry.get("distance")).doubleValue()));
        if (entities.size() > clampedMaxCount) {
            entities = new ArrayList<>(entities.subList(0, clampedMaxCount));
        }

        state.put("radius", clampedRadius);
        state.put("count", entities.size());
        state.put("entities", entities);
        return state;
    }

    private Map<String, Object> crosshairTarget() {
        HitResult target = mc.crosshairTarget;
        if (target == null) return Map.of("type", "none");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", target.getType().name().toLowerCase());
        map.put("position", vec(target.getPos()));

        if (target instanceof BlockHitResult blockHitResult) {
            BlockPos pos = blockHitResult.getBlockPos();
            map.put("blockPos", blockPos(pos));
            if (mc.world != null) {
                BlockState blockState = mc.world.getBlockState(pos);
                map.put("block", blockState.getBlock().getName().getString());
            }
        } else if (target instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            map.put("entityType", String.valueOf(Registries.ENTITY_TYPE.getId(entity.getType())));
            map.put("entityName", entity.getName().getString());
            map.put("entityId", entity.getId());
            map.put("entityPos", vec(entity.getX(), entity.getY(), entity.getZ()));
        }

        return map;
    }

    private List<Map<String, Object>> effects(ClientPlayerEntity player) {
        List<Map<String, Object>> effects = new ArrayList<>();
        for (StatusEffectInstance instance : player.getStatusEffects()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("effect", String.valueOf(instance.getEffectType().getKey().map(k -> k.getValue()).orElse(Identifier.of("minecraft:unknown"))));
            data.put("amplifier", instance.getAmplifier());
            data.put("duration", instance.getDuration());
            data.put("ambient", instance.isAmbient());
            data.put("showParticles", instance.shouldShowParticles());
            effects.add(data);
        }
        return effects;
    }

    private List<Object> inventory(ClientPlayerEntity player) {
        var inventory = player.getInventory();
        List<Object> slots = new ArrayList<>(inventory.size());

        for (int i = 0; i < inventory.size(); i++) {
            slots.add(itemStack(inventory.getStack(i)));
        }

        return slots;
    }

    private List<Object> armor(ClientPlayerEntity player) {
        List<Object> armor = new ArrayList<>(4);
        armor.add(itemStack(player.getEquippedStack(EquipmentSlot.HEAD)));
        armor.add(itemStack(player.getEquippedStack(EquipmentSlot.CHEST)));
        armor.add(itemStack(player.getEquippedStack(EquipmentSlot.LEGS)));
        armor.add(itemStack(player.getEquippedStack(EquipmentSlot.FEET)));
        return armor;
    }

    private Map<String, Object> itemStack(ItemStack stack) {
        Identifier itemId = Registries.ITEM.getId(stack.getItem());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("itemId", itemId == null ? "unknown" : itemId.toString());
        data.put("count", stack.getCount());
        data.put("empty", stack.isEmpty());
        data.put("name", stack.getName().getString());
        data.put("damage", stack.getDamage());
        data.put("maxDamage", stack.getMaxDamage());
        data.put("damageable", stack.isDamageable());
        data.put("enchanted", stack.hasEnchantments());

        return data;
    }

    private Map<String, Object> vec(Vec3d vec) {
        return Map.of("x", vec.x, "y", vec.y, "z", vec.z);
    }

    private Map<String, Object> vec(double x, double y, double z) {
        return Map.of("x", x, "y", y, "z", z);
    }

    private Map<String, Object> blockPos(BlockPos pos) {
        return Map.of("x", pos.getX(), "y", pos.getY(), "z", pos.getZ());
    }

    private int resolveSelectedSlot(ClientPlayerEntity player) {
        try {
            return (int) player.getInventory().getClass().getMethod("getSelectedSlot").invoke(player.getInventory());
        } catch (Exception ignored) {
            return -1;
        }
    }
}

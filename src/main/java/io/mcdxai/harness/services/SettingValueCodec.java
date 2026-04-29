package io.mcdxai.harness.services;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SettingValueCodec {
    public Map<String, Object> describeSetting(Setting<?> setting) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", setting.name);
        map.put("title", setting.title);
        map.put("description", setting.description);
        map.put("settingClass", setting.getClass().getSimpleName());
        map.put("visible", setting.isVisible());
        map.put("value", serializeValue(safeGet(setting)));
        map.put("defaultValue", serializeValue(safeDefault(setting)));

        List<String> suggestions = setting.getSuggestions();
        if (suggestions != null && !suggestions.isEmpty()) {
            map.put("suggestions", new ArrayList<>(suggestions));
        }

        return map;
    }

    public boolean applySettingValue(Setting<?> setting, Object value) {
        if (value == null) {
            return false;
        }

        Object current = safeGet(setting);

        try {
            if (current instanceof Boolean) {
                return setTyped(setting, toBoolean(value));
            }
            if (current instanceof Integer) {
                return setTyped(setting, toInteger(value));
            }
            if (current instanceof Double) {
                return setTyped(setting, toDouble(value));
            }
            if (current instanceof Float) {
                return setTyped(setting, toFloat(value));
            }
            if (current instanceof String) {
                return setTyped(setting, String.valueOf(value));
            }
            if (current instanceof Enum<?>) {
                return setting.parse(String.valueOf(value));
            }
            if (current instanceof SettingColor) {
                return applyColor(setting, value);
            }
            if (current instanceof Keybind) {
                return applyKeybind(setting, value);
            }
            if (current instanceof BlockPos) {
                return applyBlockPos(setting, value);
            }
            if (current instanceof Vec3) {
                return applyVec3d(setting, value);
            }
            if (current instanceof Block) {
                return applyRegistryObject(setting, value, RegistryKind.BLOCK);
            }
            if (current instanceof Item) {
                return applyRegistryObject(setting, value, RegistryKind.ITEM);
            }
            if (current instanceof EntityType<?>) {
                return applyRegistryObject(setting, value, RegistryKind.ENTITY_TYPE);
            }
            if (current instanceof MobEffect) {
                return applyRegistryObject(setting, value, RegistryKind.STATUS_EFFECT);
            }
            if (current instanceof Potion) {
                return applyRegistryObject(setting, value, RegistryKind.POTION);
            }
            if (current instanceof SoundEvent) {
                return applyRegistryObject(setting, value, RegistryKind.SOUND_EVENT);
            }
            if (current instanceof ParticleType<?>) {
                return applyRegistryObject(setting, value, RegistryKind.PARTICLE_TYPE);
            }
            if (current instanceof BlockEntityType<?>) {
                return applyRegistryObject(setting, value, RegistryKind.BLOCK_ENTITY_TYPE);
            }
            if (current instanceof MenuType<?>) {
                return applyRegistryObject(setting, value, RegistryKind.SCREEN_HANDLER_TYPE);
            }
            if (current instanceof Module) {
                return applyModule(setting, value);
            }
            if (current instanceof List<?> || current instanceof Set<?>) {
                return applyCollection(setting, value);
            }
            if (current instanceof Map<?, ?>) {
                return applyMap(setting, value);
            }

            return setting.parse(String.valueOf(value));
        } catch (Exception ignored) {
            return false;
        }
    }

    private Object safeGet(Setting<?> setting) {
        try {
            return setting.get();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object safeDefault(Setting<?> setting) {
        try {
            return setting.getDefaultValue();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object serializeValue(Object value) {
        if (value == null) return null;

        if (value instanceof String || value instanceof Boolean || value instanceof Number) {
            return value;
        }
        if (value instanceof Module module) {
            return module.name;
        }
        if (value instanceof BlockPos pos) {
            return Map.of("x", pos.getX(), "y", pos.getY(), "z", pos.getZ());
        }
        if (value instanceof Vec3 vec) {
            return Map.of("x", vec.x, "y", vec.y, "z", vec.z);
        }
        if (value instanceof SettingColor color) {
            return Map.of(
                "r", color.r,
                "g", color.g,
                "b", color.b,
                "a", color.a,
                "rainbow", color.rainbow
            );
        }
        if (value instanceof Keybind keybind) {
            var tag = keybind.toTag();
            return Map.of(
                "isKey", tag.contains("isKey") ? tag.getBoolean("isKey") : true,
                "value", tag.contains("value") ? tag.getInt("value") : -1,
                "modifiers", tag.contains("modifiers") ? tag.getInt("modifiers") : 0,
                "label", keybind.toString()
            );
        }
        if (value instanceof ItemStack stack) {
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return Map.of(
                "itemId", id == null ? "unknown" : id.toString(),
                "count", stack.getCount(),
                "empty", stack.isEmpty(),
                "name", stack.getHoverName().getString()
            );
        }

        String registryId = tryResolveRegistryId(value);
        if (registryId != null) {
            return registryId;
        }

        if (value instanceof Collection<?> collection) {
            List<Object> list = new ArrayList<>(collection.size());
            for (Object o : collection) {
                list.add(serializeValue(o));
            }
            return list;
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> serialized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                serialized.put(String.valueOf(entry.getKey()), serializeValue(entry.getValue()));
            }
            return serialized;
        }

        return String.valueOf(value);
    }

    private String tryResolveRegistryId(Object value) {
        Identifier id;

        if (value instanceof Block block) {
            id = BuiltInRegistries.BLOCK.getKey(block);
            return id == null ? null : id.toString();
        }
        if (value instanceof Item item) {
            id = BuiltInRegistries.ITEM.getKey(item);
            return id == null ? null : id.toString();
        }
        if (value instanceof EntityType<?> entityType) {
            id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            return id == null ? null : id.toString();
        }
        if (value instanceof MobEffect statusEffect) {
            id = BuiltInRegistries.MOB_EFFECT.getKey(statusEffect);
            return id == null ? null : id.toString();
        }
        if (value instanceof Potion potion) {
            id = BuiltInRegistries.POTION.getKey(potion);
            return id == null ? null : id.toString();
        }
        if (value instanceof SoundEvent soundEvent) {
            id = BuiltInRegistries.SOUND_EVENT.getKey(soundEvent);
            return id == null ? null : id.toString();
        }
        if (value instanceof ParticleType<?> particleType) {
            id = BuiltInRegistries.PARTICLE_TYPE.getKey(particleType);
            return id == null ? null : id.toString();
        }
        if (value instanceof BlockEntityType<?> blockEntityType) {
            id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntityType);
            return id == null ? null : id.toString();
        }
        if (value instanceof MenuType<?> screenHandlerType) {
            id = BuiltInRegistries.MENU.getKey(screenHandlerType);
            return id == null ? null : id.toString();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> boolean setTyped(Setting<?> setting, T value) {
        return ((Setting<T>) setting).set(value);
    }

    private boolean applyColor(Setting<?> setting, Object value) {
        if (!(value instanceof Map<?, ?> map)) return false;

        @SuppressWarnings("unchecked")
        Setting<SettingColor> colorSetting = (Setting<SettingColor>) setting;

        SettingColor color = colorSetting.get();
        if (color == null) color = new SettingColor();

        color.r = toInteger(map.get("r"));
        color.g = toInteger(map.get("g"));
        color.b = toInteger(map.get("b"));
        Object alpha = map.containsKey("a") ? map.get("a") : Integer.valueOf(255);
        Object rainbow = map.containsKey("rainbow") ? map.get("rainbow") : Boolean.FALSE;
        color.a = toInteger(alpha);
        color.rainbow = toBoolean(rainbow);
        color.validate();

        return colorSetting.set(color);
    }

    private boolean applyKeybind(Setting<?> setting, Object value) {
        if (!(value instanceof Map<?, ?> map)) return false;

        @SuppressWarnings("unchecked")
        Setting<Keybind> keybindSetting = (Setting<Keybind>) setting;

        Keybind keybind = keybindSetting.get();
        if (keybind == null) keybind = Keybind.none();

        Object rawIsKey = map.containsKey("isKey") ? map.get("isKey") : Boolean.TRUE;
        Object rawValue = map.containsKey("value") ? map.get("value") : Integer.valueOf(-1);
        Object rawModifiers = map.containsKey("modifiers") ? map.get("modifiers") : Integer.valueOf(0);

        boolean isKey = toBoolean(rawIsKey);
        int inputValue = toInteger(rawValue);
        int modifiers = toInteger(rawModifiers);

        keybind.set(isKey, inputValue, modifiers);
        return keybindSetting.set(keybind);
    }

    private boolean applyBlockPos(Setting<?> setting, Object value) {
        if (!(value instanceof Map<?, ?> map)) return false;

        int x = toInteger(map.get("x"));
        int y = toInteger(map.get("y"));
        int z = toInteger(map.get("z"));

        return setTyped(setting, new BlockPos(x, y, z));
    }

    private boolean applyVec3d(Setting<?> setting, Object value) {
        if (!(value instanceof Map<?, ?> map)) return false;

        double x = toDouble(map.get("x"));
        double y = toDouble(map.get("y"));
        double z = toDouble(map.get("z"));

        return setTyped(setting, new Vec3(x, y, z));
    }

    private boolean applyRegistryObject(Setting<?> setting, Object value, RegistryKind kind) {
        Identifier id = toIdentifier(value);
        if (id == null) return false;

        Object resolved = switch (kind) {
            case BLOCK -> BuiltInRegistries.BLOCK.getValue(id);
            case ITEM -> BuiltInRegistries.ITEM.getValue(id);
            case ENTITY_TYPE -> BuiltInRegistries.ENTITY_TYPE.getValue(id);
            case STATUS_EFFECT -> BuiltInRegistries.MOB_EFFECT.getValue(id);
            case POTION -> BuiltInRegistries.POTION.getValue(id);
            case SOUND_EVENT -> BuiltInRegistries.SOUND_EVENT.getValue(id);
            case PARTICLE_TYPE -> BuiltInRegistries.PARTICLE_TYPE.getValue(id);
            case BLOCK_ENTITY_TYPE -> BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(id);
            case SCREEN_HANDLER_TYPE -> BuiltInRegistries.MENU.getValue(id);
        };

        return resolved != null && setTyped(setting, resolved);
    }

    private boolean applyModule(Setting<?> setting, Object value) {
        String moduleName = String.valueOf(value);
        Module module = meteordevelopment.meteorclient.systems.modules.Modules.get().get(moduleName);
        return module != null && setTyped(setting, module);
    }

    @SuppressWarnings("unchecked")
    private boolean applyCollection(Setting<?> setting, Object value) {
        if (!(value instanceof List<?> list)) {
            return setting.parse(String.valueOf(value));
        }

        String className = setting.getClass().getSimpleName();

        if (className.equals("StringListSetting")) {
            List<String> strings = new ArrayList<>(list.size());
            for (Object item : list) strings.add(String.valueOf(item));
            return ((Setting<List<String>>) setting).set(strings);
        }
        if (className.equals("ModuleListSetting")) {
            List<Module> modules = new ArrayList<>();
            for (Object item : list) {
                Module module = meteordevelopment.meteorclient.systems.modules.Modules.get().get(String.valueOf(item));
                if (module != null) modules.add(module);
            }
            return ((Setting<List<Module>>) setting).set(modules);
        }
        if (className.equals("ItemListSetting")) {
            List<Item> items = new ArrayList<>();
            for (Object item : list) {
                Identifier id = toIdentifier(item);
                if (id == null) continue;
                Item resolved = BuiltInRegistries.ITEM.getValue(id);
                if (resolved != null) items.add(resolved);
            }
            return ((Setting<List<Item>>) setting).set(items);
        }
        if (className.equals("BlockListSetting")) {
            List<Block> blocks = new ArrayList<>();
            for (Object item : list) {
                Identifier id = toIdentifier(item);
                if (id == null) continue;
                Block resolved = BuiltInRegistries.BLOCK.getValue(id);
                if (resolved != null) blocks.add(resolved);
            }
            return ((Setting<List<Block>>) setting).set(blocks);
        }
        if (className.equals("EntityTypeListSetting")) {
            Set<EntityType<?>> entities = new java.util.HashSet<>();
            for (Object item : list) {
                Identifier id = toIdentifier(item);
                if (id == null) continue;
                EntityType<?> resolved = BuiltInRegistries.ENTITY_TYPE.getValue(id);
                if (resolved != null) entities.add(resolved);
            }
            return ((Setting<Set<EntityType<?>>>) setting).set(entities);
        }
        if (className.equals("StatusEffectListSetting")) {
            List<MobEffect> effects = new ArrayList<>();
            for (Object item : list) {
                Identifier id = toIdentifier(item);
                if (id == null) continue;
                MobEffect resolved = BuiltInRegistries.MOB_EFFECT.getValue(id);
                if (resolved != null) effects.add(resolved);
            }
            return ((Setting<List<MobEffect>>) setting).set(effects);
        }

        return setting.parse(String.valueOf(list));
    }

    @SuppressWarnings("unchecked")
    private boolean applyMap(Setting<?> setting, Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return setting.parse(String.valueOf(value));
        }

        if (setting.getClass().getSimpleName().equals("StatusEffectAmplifierMapSetting")) {
            Map<MobEffect, Integer> parsed = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Identifier id = toIdentifier(entry.getKey());
                if (id == null) continue;
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(id);
                if (effect == null) continue;
                parsed.put(effect, toInteger(entry.getValue()));
            }
            return ((Setting<Map<MobEffect, Integer>>) setting).set(parsed);
        }

        return setting.parse(String.valueOf(map));
    }

    private Identifier toIdentifier(Object raw) {
        if (raw == null) return null;
        try {
            return Identifier.parse(String.valueOf(raw));
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) return booleanValue;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private int toInteger(Object value) {
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0D;
        }
    }

    private float toFloat(Object value) {
        if (value instanceof Number number) return number.floatValue();
        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (Exception ignored) {
            return 0F;
        }
    }

    private enum RegistryKind {
        BLOCK,
        ITEM,
        ENTITY_TYPE,
        STATUS_EFFECT,
        POTION,
        SOUND_EVENT,
        PARTICLE_TYPE,
        BLOCK_ENTITY_TYPE,
        SCREEN_HANDLER_TYPE
    }
}

package io.mcdxai.harness.services;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModuleService {
    private final SettingValueCodec settingValueCodec;

    public ModuleService(SettingValueCodec settingValueCodec) {
        this.settingValueCodec = settingValueCodec;
    }

    public List<Map<String, Object>> listModules(boolean includeSettings) {
        List<Module> modules = new ArrayList<>(Modules.get().getAll());
        modules.sort(Comparator.comparing(module -> module.name, String.CASE_INSENSITIVE_ORDER));

        List<Map<String, Object>> mapped = new ArrayList<>(modules.size());
        for (Module module : modules) {
            mapped.add(describeModule(module, includeSettings));
        }

        return mapped;
    }

    public Optional<Module> findModule(String moduleName) {
        if (moduleName == null || moduleName.isBlank()) return Optional.empty();

        for (Module module : Modules.get().getAll()) {
            if (module.name.equalsIgnoreCase(moduleName) || module.title.equalsIgnoreCase(moduleName)) {
                return Optional.of(module);
            }
        }

        return Optional.empty();
    }

    public Map<String, Object> describeModule(Module module, boolean includeSettings) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", module.name);
        map.put("title", module.title);
        map.put("description", module.description);
        map.put("category", module.category.name);
        map.put("active", module.isActive());
        map.put("favorite", module.favorite);
        map.put("addon", module.addon != null ? module.addon.name : "Meteor Client");

        if (includeSettings) {
            map.put("settings", listModuleSettings(module));
        }

        return map;
    }

    public boolean setModuleState(Module module, boolean active) {
        if (module.isActive() == active) return true;
        module.toggle();
        return module.isActive() == active;
    }

    public List<Map<String, Object>> listModuleSettings(Module module) {
        List<Map<String, Object>> groups = new ArrayList<>();

        for (SettingGroup group : module.settings) {
            Map<String, Object> groupData = new LinkedHashMap<>();
            groupData.put("name", group.name);

            List<Map<String, Object>> settings = new ArrayList<>();
            for (Setting<?> setting : group) {
                settings.add(settingValueCodec.describeSetting(setting));
            }

            groupData.put("settings", settings);
            groups.add(groupData);
        }

        return groups;
    }

    public Optional<Setting<?>> findSetting(Module module, String settingName) {
        if (settingName == null || settingName.isBlank()) {
            return Optional.empty();
        }

        for (SettingGroup group : module.settings) {
            for (Setting<?> setting : group) {
                if (setting.name.equalsIgnoreCase(settingName) || setting.title.equalsIgnoreCase(settingName)) {
                    return Optional.of(setting);
                }
            }
        }

        return Optional.empty();
    }

    public boolean setSetting(Setting<?> setting, Object value) {
        return settingValueCodec.applySettingValue(setting, value);
    }
}
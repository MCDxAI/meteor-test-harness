package io.mcdxai.harness.services;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.elements.ActiveModulesHud;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import net.fabricmc.loader.api.FabricLoader;
import org.meteordev.starscript.Script;
import org.meteordev.starscript.compiler.Compiler;
import org.meteordev.starscript.compiler.Parser;
import org.meteordev.starscript.utils.StarscriptError;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MeteorInfoService {

    public Map<String, Object> getMeteorInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        // Meteor Client
        info.put("name", MeteorClient.NAME);
        info.put("version", MeteorClient.VERSION.toString());
        info.put("buildNumber", MeteorClient.BUILD_NUMBER);
        info.put("devEnvironment", FabricLoader.getInstance().isDevelopmentEnvironment());

        String meteorCommit = MeteorClient.ADDON.getCommit();
        info.put("commit", meteorCommit);

        // Minecraft version
        String mcVersion = FabricLoader.getInstance()
            .getModContainer("minecraft")
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
        info.put("minecraftVersion", mcVersion);

        // Baritone / pathing
        Map<String, Object> pathing = new LinkedHashMap<>();
        pathing.put("baritoneAvailable", BaritoneUtils.IS_AVAILABLE);
        pathing.put("pathManager", PathManagers.get().getName());
        if (BaritoneUtils.IS_AVAILABLE) {
            pathing.put("baritonePrefix", BaritoneUtils.getPrefix());
        }
        info.put("pathing", pathing);

        // Addons
        List<Map<String, Object>> addons = new ArrayList<>();
        for (MeteorAddon addon : AddonManager.ADDONS) {
            addons.add(describeAddon(addon));
        }
        info.put("addons", addons);

        // Counts
        info.put("moduleCount", Modules.get().getCount());
        info.put("hudTypeCount", Hud.get().infos.size());

        return info;
    }

    public Map<String, Object> getAddonFeatures(String addonName) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (addonName == null || addonName.isBlank()) {
            // All addons: modules grouped by addon, HUD types grouped by group
            result.put("modules", buildModulesByAddon(null));
            result.put("hudTypes", buildHudTypesByGroup(null));
        } else {
            // Find the addon
            MeteorAddon matchedAddon = findAddon(addonName);
            if (matchedAddon == null) {
                result.put("error", "Addon not found: " + addonName);
                result.put("availableAddons", AddonManager.ADDONS.stream().map(a -> a.name).toList());
                return result;
            }

            result.put("addon", describeAddon(matchedAddon));
            result.put("modules", buildModulesByAddon(matchedAddon));
            result.put("hudTypes", buildHudTypesByGroup(matchedAddon));
        }

        return result;
    }

    public Map<String, Object> getActiveHud() {
        Map<String, Object> result = new LinkedHashMap<>();
        Hud hud = Hud.get();

        result.put("enabled", hud.active);
        if (!hud.active) {
            result.put("message", "HUD is disabled. Enable it in Meteor settings to see active elements.");
            return result;
        }

        List<Map<String, Object>> elements = new ArrayList<>();
        for (HudElement element : hud) {
            if (!element.isActive()) continue;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", element.info.name);
            Object value = resolveHudValue(element);
            if (value != null) entry.put("value", value);
            entry.put("x", element.x);
            entry.put("y", element.y);
            entry.put("width", element.getWidth());
            entry.put("height", element.getHeight());
            elements.add(entry);
        }

        result.put("elementCount", elements.size());
        result.put("elements", elements);
        return result;
    }

    private Object resolveHudValue(HudElement element) {
        if (element instanceof TextHud textHud) {
            return resolveTextHudValue(textHud);
        }

        if (element instanceof ActiveModulesHud) {
            List<String> activeModules = new ArrayList<>();
            for (Module module : Modules.get().getActive()) {
                String info = module.getInfoString();
                if (info == null || info.isBlank()) activeModules.add(module.title);
                else activeModules.add(module.title + " " + info);
            }

            return activeModules;
        }

        return null;
    }

    private String resolveTextHudValue(TextHud textHud) {
        String source = textHud.text.get();
        if (source == null || source.isBlank()) return "";

        Parser.Result parseResult = Parser.parse(source);
        if (parseResult.hasErrors()) {
            return parseResult.errors.getFirst().toString();
        }

        Script script = Compiler.compile(parseResult);
        try {
            String value = MeteorStarscript.run(script);
            return value != null ? value : "";
        } catch (StarscriptError error) {
            return error.getMessage();
        }
    }

    private Map<String, Object> describeAddon(MeteorAddon addon) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", addon.name);
        info.put("authors", addon.authors);
        info.put("package", addon.getPackage());

        String website = addon.getWebsite();
        if (website != null) info.put("website", website);

        String commit = addon.getCommit();
        if (commit != null) info.put("commit", commit);

        GithubRepo repo = addon.getRepo();
        if (repo != null) info.put("repo", repo.getOwnerName());

        return info;
    }

    private Map<String, List<Map<String, Object>>> buildModulesByAddon(MeteorAddon filterAddon) {
        Map<String, List<Map<String, Object>>> grouped = new TreeMap<>();

        for (Module module : Modules.get().getAll()) {
            String ownerName = module.addon != null ? module.addon.name : "Unknown";

            if (filterAddon != null && module.addon != filterAddon) {
                continue;
            }

            grouped.computeIfAbsent(ownerName, k -> new ArrayList<>()).add(describeModuleBrief(module));
        }

        return grouped;
    }

    private Map<String, List<Map<String, Object>>> buildHudTypesByGroup(MeteorAddon filterAddon) {
        Map<String, List<Map<String, Object>>> grouped = new TreeMap<>();
        Hud hud = Hud.get();

        for (HudElementInfo<?> info : hud.infos.values()) {
            String groupTitle = info.group.title();

            if (filterAddon != null) {
                // Best-effort match: group title against addon name
                if (!matchesAddon(groupTitle, filterAddon)) continue;
            }

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", info.name);
            entry.put("title", info.title);
            entry.put("description", info.description);

            if (info.hasPresets()) {
                entry.put("presets", info.presets.stream().map(p -> p.title).toList());
            }

            grouped.computeIfAbsent(groupTitle, k -> new ArrayList<>()).add(entry);
        }

        return grouped;
    }

    private boolean matchesAddon(String groupTitle, MeteorAddon addon) {
        if (groupTitle.equalsIgnoreCase(addon.name)) return true;
        // Meteor Client's built-in group title is "Meteor", addon name is "Meteor Client"
        if ("Meteor".equalsIgnoreCase(groupTitle) && "Meteor Client".equalsIgnoreCase(addon.name)) return true;
        return false;
    }

    private Map<String, Object> describeModuleBrief(Module module) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", module.name);
        entry.put("title", module.title);
        entry.put("category", module.category.name);
        entry.put("active", module.isActive());
        entry.put("description", module.description);
        return entry;
    }

    private MeteorAddon findAddon(String name) {
        for (MeteorAddon addon : AddonManager.ADDONS) {
            if (addon.name.equalsIgnoreCase(name)) return addon;
        }
        return null;
    }
}

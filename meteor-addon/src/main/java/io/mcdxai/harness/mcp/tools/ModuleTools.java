package io.mcdxai.harness.mcp.tools;

import io.mcdxai.harness.mcp.RegistryContext;
import io.mcdxai.harness.mcp.ToolSchemas;
import io.mcdxai.harness.services.ModuleService;
import io.mcdxai.harness.util.McpResults;
import io.modelcontextprotocol.server.McpServerFeatures;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModuleTools {
    private ModuleTools() {
    }

    public static void register(List<McpServerFeatures.SyncToolSpecification> tools, RegistryContext context) {
        ModuleService moduleService = context.moduleService();

        tools.add(context.tool(
            "list_modules",
            "List all Meteor and addon modules.",
            ToolSchemas.object(
                Map.of("include_settings", ToolSchemas.boolProperty("Include full settings tree per module. Default false.")),
                List.of()
            ),
            (exchange, args) -> McpResults.ok(Map.of("modules", moduleService.listModules(args.bool("include_settings", false))))
        ));

        tools.add(context.tool(
            "get_module",
            "Get one module by name.",
            ToolSchemas.object(
                Map.of(
                    "module_name", ToolSchemas.stringProperty("Module name or title."),
                    "include_settings", ToolSchemas.boolProperty("Include settings tree. Default true.")
                ),
                List.of("module_name")
            ),
            (exchange, args) -> {
                Optional<meteordevelopment.meteorclient.systems.modules.Module> module = moduleService.findModule(args.string("module_name"));
                if (module.isEmpty()) return McpResults.error("Module not found.");
                return McpResults.ok(moduleService.describeModule(module.get(), args.bool("include_settings", true)));
            }
        ));

        tools.add(context.tool(
            "set_module_state",
            "Enable or disable a module.",
            ToolSchemas.object(
                Map.of(
                    "module_name", ToolSchemas.stringProperty("Module name or title."),
                    "active", ToolSchemas.boolProperty("Desired active state.")
                ),
                List.of("module_name", "active")
            ),
            (exchange, args) -> {
                Optional<meteordevelopment.meteorclient.systems.modules.Module> module = moduleService.findModule(args.string("module_name"));
                if (module.isEmpty()) return McpResults.error("Module not found.");

                boolean success = moduleService.setModuleState(module.get(), args.bool("active", false));
                if (!success) return McpResults.error("Failed to set module state.");

                return McpResults.ok(moduleService.describeModule(module.get(), false));
            }
        ));

        tools.add(context.tool(
            "list_module_settings",
            "List settings for a module.",
            ToolSchemas.object(
                Map.of("module_name", ToolSchemas.stringProperty("Module name or title.")),
                List.of("module_name")
            ),
            (exchange, args) -> {
                Optional<meteordevelopment.meteorclient.systems.modules.Module> module = moduleService.findModule(args.string("module_name"));
                if (module.isEmpty()) return McpResults.error("Module not found.");
                return McpResults.ok(Map.of("settings", moduleService.listModuleSettings(module.get())));
            }
        ));

        tools.add(context.tool(
            "get_module_setting",
            "Get one setting from a module.",
            ToolSchemas.object(
                Map.of(
                    "module_name", ToolSchemas.stringProperty("Module name or title."),
                    "setting_name", ToolSchemas.stringProperty("Setting name or title.")
                ),
                List.of("module_name", "setting_name")
            ),
            (exchange, args) -> {
                Optional<meteordevelopment.meteorclient.systems.modules.Module> module = moduleService.findModule(args.string("module_name"));
                if (module.isEmpty()) return McpResults.error("Module not found.");

                Optional<meteordevelopment.meteorclient.settings.Setting<?>> setting = moduleService.findSetting(module.get(), args.string("setting_name"));
                if (setting.isEmpty()) return McpResults.error("Setting not found.");

                return McpResults.ok(context.settingValueCodec().describeSetting(setting.get()));
            }
        ));

        tools.add(context.tool(
            "set_module_setting",
            "Set one module setting value.",
            ToolSchemas.object(
                Map.of(
                    "module_name", ToolSchemas.stringProperty("Module name or title."),
                    "setting_name", ToolSchemas.stringProperty("Setting name or title."),
                    "value", Map.of("description", "New value. Type depends on setting (string/number/boolean/list/map).")
                ),
                List.of("module_name", "setting_name", "value")
            ),
            (exchange, args) -> {
                Optional<meteordevelopment.meteorclient.systems.modules.Module> module = moduleService.findModule(args.string("module_name"));
                if (module.isEmpty()) return McpResults.error("Module not found.");

                Optional<meteordevelopment.meteorclient.settings.Setting<?>> setting = moduleService.findSetting(module.get(), args.string("setting_name"));
                if (setting.isEmpty()) return McpResults.error("Setting not found.");

                Object value = args.raw("value");
                boolean success = moduleService.setSetting(setting.get(), value);
                if (!success) return McpResults.error("Failed to apply setting value.");

                return McpResults.ok(context.settingValueCodec().describeSetting(setting.get()));
            }
        ));
    }
}

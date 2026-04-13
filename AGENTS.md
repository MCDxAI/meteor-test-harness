# Repository Guidelines

## What This Is

A Meteor Client addon (Fabric mod) that embeds an MCP (Model Context Protocol) HTTP server inside Minecraft. LLM agents connect via MCP Streamable HTTP transport to automate the game - click UI elements, read screen DOM trees, manage Meteor modules, send commands, control pathing - enabling automated testing of Meteor Client and Meteor addons.

## Tech Stack

- Minecraft 1.21.11, Yarn mappings 1.21.11+build.3
- Fabric Loader 0.18.2, Loom 1.14-SNAPSHOT
- Meteor Client 1.21.11-SNAPSHOT
- Java 21 (source/target/release 21)
- MCP SDK Java 1.1.1 (`mcp-core` + `mcp-json-jackson2`)
- Embedded Tomcat 11.0.13 (servlet container for MCP HTTP transport)
- Gradle with Kotlin DSL, version catalog at `gradle/libs.versions.toml`

## Build

```bash
./gradlew build
```

Output JAR goes to `build/libs/`. Drop it into a Fabric loader mods folder alongside Meteor Client.

## Runtime

The addon starts an embedded Tomcat server at `127.0.0.1:38861` with MCP endpoint at `/mcp`. Single-session mode by default. All config is in `HarnessConfig` (bind host, port, endpoint path, single-session mode, auto-start).

## Key Architectural Constraints

- All tool handlers must run on Minecraft's render thread. `MainThreadInvoker` dispatches via `CompletableFuture` to the client thread. Never call Minecraft APIs from the MCP servlet thread directly.
- No string-based reflection. Fabric uses intermediary names at runtime (for example, `class_442` not `TitleScreen`). String method-name literals are not remapped - use direct typed method calls only.
- DOM clicking must route through the screen, not elements directly. Many list widgets (world list, server list) have entries whose `mouseClicked()` just returns `true` - selection happens in the parent widget dispatch chain. Coordinate-based `screen.mouseClicked(x, y)` must run before direct element clicks.
- `Widget` (non-`ClickableWidget`) elements get coordinate data from their parent container. Their x/y may be parent-relative, not screen-relative - account for this when computing click coordinates.

## Source Layout

```text
src/main/java/io/mcdxai/harness/
  MeteorTestHarnessAddon.java      - addon entry point (MeteorAddon subclass)
  HarnessRuntime.java              - MCP server lifecycle (start/stop)
  config/HarnessConfig.java        - settings (bind host/port, session mode)
  mcp/
    McpServer.java                 - Tomcat + MCP server bootstrap
    McpRegistry.java               - wires up all tools and resources
    RegistryContext.java           - shared context (services, tool factory, session gate)
    SessionGate.java               - single-session ownership lock
    ToolSchemas.java               - JSON schema definitions for tools
    EmbeddedWebappClassLoader.java - Tomcat classloader hack
    tools/
      CoreTools.java               - harness status/debug/release tools
      ModuleTools.java             - module CRUD tools
      WorldStateTools.java         - player/world/inventory query tools
      WorldActionTools.java        - chat/command/attack action tools
      PathingTools.java            - Baritone pathing tools
      DomQueryTools.java           - DOM snapshot/query tools
      DomInteractionTools.java     - DOM click/scroll/drag tools
      DomInputTools.java           - DOM text input tools
      DomToolHelper.java           - shared DOM result-wrapping logic
      Resources.java               - MCP resource registrations
  services/
    ScreenDomService.java          - DOM engine: snapshot, click, setText, setValue
    ModuleService.java             - Meteor module CRUD
    GameStateService.java          - player/world/inventory/entity state
    PathingService.java            - Baritone/Meteor PathManager integration
    ChatLogService.java            - chat capture and history
    SettingValueCodec.java         - serialize/deserialize Meteor setting values
  util/
    MainThreadInvoker.java         - dispatch to render thread
    McpResults.java                - tool result construction (text + structuredContent)
    ArgReader.java                 - MCP tool argument parsing
src/main/resources/
  fabric.mod.json                  - Fabric mod metadata (filtering: ${version}, ${mc_version})
```

## External Reference Folder

Important: use the external reference folder at `C:\Users\coper\Documents\AI-Workspace\meteor-test-harness-references` when you need upstream context.

It contains Meteor Client source code, Baritone source code, addon template/reference material, and related docs:

- `meteor-client/` - cloned source of Meteor Client (upstream target for this addon)
- `baritone/` - cloned source of Meteor's Baritone fork (pathing engine)
- `meteor-addon-template/` - Meteor's official addon template project
- `meteor-addon-development-reference.md` - addon development documentation

When behavior is unclear (Meteor internals, screen/widget hierarchies, addon integration details), consult this folder instead of guessing. Cross-reference Yarn/intermediary class names (for example, `class_442` = `SelectWorldScreen`) against the source.

## Minecraft Dev MCP Tooling

The `minecraft_dev_mcp` toolset is configured and should be a first-choice workflow for Minecraft internals, mapping lookups, decompilation, mixin/access-widener validation, and mod inspection.

Use this before guessing about class/method names, registry contents, or cross-version behavior changes.

### Tool Inventory (Configured)

- Version and cache management:
  - `list_minecraft_versions`
  - `decompile_minecraft_version`
  - `index_minecraft_version`
- Source, symbols, and docs:
  - `get_minecraft_source`
  - `search_minecraft_code`
  - `search_indexed`
  - `find_mapping`
  - `get_documentation`
  - `search_documentation`
- Version comparison:
  - `compare_versions`
  - `compare_versions_detailed`
- Registry inspection:
  - `get_registry_data`
- Mixin and access-widener validation:
  - `analyze_mixin`
  - `validate_access_widener`
- Mod JAR analysis/decompile/remap:
  - `analyze_mod_jar`
  - `remap_mod_jar`
  - `decompile_mod_jar`
  - `index_mod`
  - `search_mod_code`
  - `search_mod_indexed`

Notes:
- `analyze_mixin` and `validate_access_widener` can validate content or file paths.
- Mod JAR tooling supports both Windows paths (for example, `C:\...`) and WSL paths (for example, `/mnt/c/...`).
- Prefer indexed search (`search_indexed`, `search_mod_indexed`) after indexing when doing broad lookups.

## Additional Docs

- `HANDOFF.md` - detailed session handoff notes including current bugs, in-progress fixes, and action items
- `docs/v0-spec.md` - v0 scope and tool surface specification

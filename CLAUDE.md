# Repository Guidelines

## What This Is

A **Meteor Client addon** (Fabric mod) that embeds an MCP (Model Context Protocol) HTTP server inside Minecraft. LLM agents connect via MCP Streamable HTTP transport to automate the game — click UI elements, read screen DOM trees, manage Meteor modules, send commands, control pathing — enabling automated testing of Meteor Client and Meteor addons.

## Tech Stack

- **Minecraft** 1.21.11, **Yarn mappings** 1.21.11+build.3
- **Fabric** Loader 0.18.2, Loom 1.14-SNAPSHOT
- **Meteor Client** 1.21.11-SNAPSHOT
- **Java 21** (source/target/release 21)
- **MCP SDK** Java 1.1.1 (`mcp-core` + `mcp-json-jackson2`)
- **Embedded Tomcat** 11.0.13 (servlet container for MCP HTTP transport)
- **Gradle** with Kotlin DSL, version catalog at `gradle/libs.versions.toml`

## Build

```
./gradlew build
```

Output JAR goes to `build/libs/`. Drop into a Fabric loader mods folder alongside Meteor Client.

## Runtime

The addon starts an embedded Tomcat server at `127.0.0.1:38861` with MCP endpoint at `/mcp`. Single-session mode by default. All config in `HarnessConfig` (bind host, port, endpoint path, single-session mode, auto-start).

## Key Architectural Constraints

- **All tool handlers must run on Minecraft's render thread.** `MainThreadInvoker` dispatches via `CompletableFuture` to the client thread. Never call Minecraft APIs from the MCP servlet thread directly.
- **No string-based reflection.** Fabric uses intermediary names at runtime (e.g., `class_442` not `TitleScreen`). String method name literals are NOT remapped — direct typed method calls only.
- **DOM clicking must route through the screen, not elements directly.** Many list widgets (world list, server list) have entries whose `mouseClicked()` just returns `true` — selection happens in the parent widget's dispatch chain. Coordinate-based `screen.mouseClicked(x, y)` must run before direct element clicks.
- **`Widget` (non-`ClickableWidget`) elements** get coordinate data from their parent container. Their x/y may be parent-relative, not screen-relative — account for this when computing click coordinates.

## Source Layout

```
src/main/java/io/mcdxai/harness/
  MeteorTestHarnessAddon.java    — addon entry point (MeteorAddon subclass)
  HarnessRuntime.java            — MCP server lifecycle (start/stop)
  config/HarnessConfig.java      — settings (bind host/port, session mode)
  mcp/
    McpServer.java               — Tomcat + MCP server bootstrap
    McpRegistry.java             — wires up all tools and resources
    RegistryContext.java         — shared context (services, tool factory, session gate)
    SessionGate.java             — single-session ownership lock
    ToolSchemas.java             — JSON schema definitions for tools
    EmbeddedWebappClassLoader.java — Tomcat classloader hack
    tools/
      CoreTools.java             — harness status/debug/release tools
      ModuleTools.java           — module CRUD tools
      WorldStateTools.java       — player/world/inventory query tools
      WorldActionTools.java      — chat/command/attack action tools
      PathingTools.java          — Baritone pathing tools
      DomQueryTools.java         — DOM snapshot/query tools
      DomInteractionTools.java   — DOM click/scroll/drag tools
      DomInputTools.java         — DOM text input tools
      DomToolHelper.java         — shared DOM result-wrapping logic
      Resources.java             — MCP resource registrations
  services/
    ScreenDomService.java        — DOM engine: snapshot, click, setText, setValue
    ModuleService.java           — Meteor module CRUD
    GameStateService.java        — player/world/inventory/entity state
    PathingService.java          — Baritone/Meteor PathManager integration
    ChatLogService.java          — chat capture and history
    SettingValueCodec.java       — serialize/deserialize Meteor setting values
  util/
    MainThreadInvoker.java       — dispatch to render thread
    McpResults.java              — tool result construction (text + structuredContent)
    ArgReader.java               — MCP tool argument parsing
src/main/resources/
  fabric.mod.json                — Fabric mod metadata (filtering: ${version}, ${mc_version})
```

## External Knowledge Base

Located at **`C:\Users\coper\Documents\AI-Workspace\meteor-test-harness-references`**:

- **`meteor-client/`** — cloned source of Meteor Client (the upstream this addon targets)
- **`baritone/`** — cloned source of Meteor's Baritone fork (pathing engine)
- **`meteor-addon-template/`** — Meteor's official addon template project
- **`meteor-addon-development-reference.md`** — addon development documentation

When you need to understand Meteor internals, Minecraft screen/widget hierarchies, or how addons interact with Meteor systems, consult this knowledge base rather than guessing. Cross-reference Yarn-mapped class names (e.g., `class_442` = `SelectWorldScreen`) against the source.

## Additional Docs

- `HANDOFF.md` — detailed session handoff notes including current bugs, in-progress fixes, and action items
- `docs/v0-spec.md` — v0 scope and tool surface specification

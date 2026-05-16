<div align="center">

# Meteor Test Harness

![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-00800f?style=flat)
![Fabric](https://img.shields.io/badge/Fabric_Loader-0.19.2-3d5dff?style=flat)
![Meteor Client](https://img.shields.io/badge/Meteor_Client-26.1.2-8a11b6?style=flat)
![Java](https://img.shields.io/badge/Java-25-e28655?style=flat)
![MCP SDK](https://img.shields.io/badge/MCP_SDK-1.1.1-1a9f5c?style=flat)

**LLM-driven test harness for Meteor Client — expose Minecraft as an MCP server and automate the game via DOM-first screen interaction, module control, pathing, and game-state queries.**

</div>

<div align="center">

## Projects In This Repo

| Module | Mod ID | Port | What it targets |
| --- | --- | --- | --- |
| [`meteor-addon/`](meteor-addon/) | `meteor-test-harness` | `38861` | Meteor Client — module CRUD, HUD, Baritone pathing, DOM over vanilla screens |
| [`universal/`](universal/) | `universal-harness` | `38862` | Engine-agnostic Fabric mod — DOM over vanilla + owo-lib + hybrid screens (no Meteor required) |

The two are independent Fabric mods sharing a Gradle multi-project. Pick `meteor-addon` for Meteor-specific automation, `universal-harness` for general Fabric-mod GUI testing (e.g. owo-lib mods like [item-editor](https://github.com/noramibu/Item-Editor)). See [`universal/README.md`](universal/README.md) for the universal variant's scope and tool surface.

</div>

<div align="center">

## Features

| Capability | Details |
| --- | --- |
| **DOM-first screen interaction** | Snapshot any screen into a structured DOM tree, query elements by label/role/type, click, scroll, drag, and type text — all from an LLM agent. |
| **Module & setting control** | List, inspect, toggle, and configure every Meteor module and addon module with full settings CRUD. |
| **Structured game-state queries** | Player position/vitals/effects, world info, inventory, crosshair target, nearby entities — all returned as structured JSON. |
| **Baritone pathing integration** | Move to coordinates, move in cardinal directions, pause/resume/stop, and wait for pathing completion. |
| **Chat & command execution** | Send chat messages and slash commands, capture and query chat history. |
| **Meteor info & addon introspection** | Query Meteor version, installed addons, module/HUD counts, and active HUD elements. |
| **Single-session ownership** | Configurable session gate ensures one agent owns the harness at a time. |
| **Embedded HTTP server** | Tomcat 11.0.13 serves MCP via Streamable HTTP — no external process needed. |

</div>

<div align="center">

## Quick Start

| Step | Instructions |
| --- | --- |
| **Requirements** | • Java 25 or higher<br>• Minecraft 26.1.2<br>• Fabric Loader 0.19.2+<br>• Meteor Client 26.1.2-SNAPSHOT |
| **Installation** | 1. Download the latest `.jar` from [releases](https://github.com/MCDxAI/meteor-test-harness/releases)<br>2. Place in `.minecraft/mods/` alongside Meteor Client<br>3. Launch Minecraft with Fabric profile |
| **Usage** | The MCP server starts automatically on launch (configurable). Connect an MCP client to `http://127.0.0.1:38861/mcp` using Streamable HTTP transport. |

</div>

<div align="center">

## Configuration

All settings are available in the **Meteor GUI → Test Harness** tab.

| Setting | Default | Description |
| --- | --- | --- |
| `bind-host` | `127.0.0.1` | Host/IP the MCP HTTP server binds to |
| `bind-port` | `38861` | Port for the MCP HTTP endpoint (1024–65535) |
| `mcp-endpoint` | `/mcp` | HTTP endpoint path exposed by the MCP server |
| `auto-start` | `true` | Start the MCP server automatically when Meteor initializes |
| `single-session-mode` | `false` | Restrict to one active MCP session owner at a time |
| `keep-alive-seconds` | `30` | Connection keep-alive interval in seconds |
| `request-timeout-seconds` | `30` | Maximum execution time for a single MCP tool call |
| `chat-history-limit` | `200` | Maximum captured chat lines retained in memory |

</div>

<div align="center">

## MCP Tools

### Session & Runtime

| Tool | Description |
| --- | --- |
| `get_harness_status` | Get harness runtime/session status |
| `get_harness_debug_info` | Get harness diagnostics |
| `release_session` | Release current session ownership lock |

### Meteor Info

| Tool | Description |
| --- | --- |
| `get_meteor_info` | Get Meteor Client version, build, Baritone status, installed addons, module/HUD counts |
| `list_addon_features` | List modules and HUD element types per addon |
| `get_active_hud` | Get active HUD element instances with positions and evaluated text |

### Modules & Settings

| Tool | Description |
| --- | --- |
| `list_modules` | List all modules with optional settings tree |
| `get_module` | Get single module details with optional settings |
| `set_module_state` | Toggle a module on or off |
| `list_module_settings` | List all settings for a module |
| `get_module_setting` | Get a single setting value |
| `set_module_setting` | Update a setting value |

### World State

| Tool | Description |
| --- | --- |
| `get_player_state` | Get player position, vitals, movement flags, effects |
| `get_world_state` | Get current world state snapshot |
| `get_player_inventory` | Query inventory by section (all, hotbar, main, row, range, selected, armor, offhand, hands) |
| `get_crosshair_target` | Get current crosshair target |
| `get_nearby_entities` | Get nearby entities within a configurable radius |

### DOM Engine — Queries

| Tool | Description |
| --- | --- |
| `get_screen_dom` | Get full DOM tree for the active screen |
| `get_screen_dom_summary` | Get a compact DOM summary |
| `find_dom_elements` | Search DOM elements by label, text, role, actions, module name, or type |
| `get_dom_element` | Get a single DOM element by ID |
| `get_dom_subtree` | Get a DOM subtree rooted at a given element |

### DOM Engine — Interaction

| Tool | Description |
| --- | --- |
| `click_dom_element` | Click a DOM element by ID |
| `click_dom_query` | Click a DOM element matching filters |
| `scroll_dom_element` | Scroll a DOM element (vertical/horizontal) |
| `drag_dom_element` | Drag a DOM element by offset |
| `navigate_back` | Close current screen / go back |

### DOM Engine — Input

| Tool | Description |
| --- | --- |
| `set_dom_text` | Set text on a DOM element by ID |
| `set_dom_text_query` | Set text on a DOM element matching filters |
| `type_dom_text` | Type text character-by-character on a DOM element |
| `set_dom_value` | Set a value (boolean for toggles, number for sliders) |
| `press_screen_key` | Press a key on the active screen (ENTER, ESCAPE, TAB, arrows, etc.) |

### Chat & Commands

| Tool | Description |
| --- | --- |
| `send_chat` | Send a chat message as the player |
| `send_command` | Send a slash command as the player |
| `disconnect_world` | Disconnect from the current world/server |
| `get_chat_history` | Get captured chat history |
| `clear_chat_history` | Clear the chat history buffer |

### Pathing

| Tool | Description |
| --- | --- |
| `get_pathing_status` | Get Baritone/PathManager status |
| `pathing_move_to` | Path to target coordinates (X, Y, Z) |
| `pathing_move_in_direction` | Path in a cardinal direction (north, south, east, west) |
| `pathing_pause` | Pause current pathing |
| `pathing_resume` | Resume paused pathing |
| `pathing_stop` | Stop current pathing |
| `wait_for_pathing_action` | Wait for a pathing action to reach a terminal or paused state |

</div>

<div align="center">

## MCP Resources

| URI | Description |
| --- | --- |
| `meteor://modules` | All modules with setting schema and current values |
| `meteor://state/player` | Latest player state snapshot |
| `meteor://state/world` | Latest world state snapshot |
| `meteor://state/crosshair` | Latest crosshair target snapshot |
| `meteor://state/entities` | Nearby entities around the player |
| `meteor://state/pathing` | Current pathing status |
| `meteor://state/screen-dom` | DOM snapshot of the active screen |
| `meteor://chat/history` | Buffered incoming/outgoing chat lines |

</div>

<div align="center">

## Development

| Task | Command |
| --- | --- |
| **Build all** | `./gradlew build` — Builds both `meteor-addon` and `universal` |
| **Build one** | `./gradlew :meteor-addon:build` or `./gradlew :universal:build` — JARs land in each module's `build/libs/` |
| **Clean Build** | `./gradlew clean build` — Removes old artifacts and rebuilds |
| **Run Client** | `./gradlew :meteor-addon:runClient` — Launches a Fabric dev client with the addon loaded |
| **Dependencies** | Bundled: MCP SDK 1.1.1, Embedded Tomcat 11.0.13 • Provided: Meteor Client, Fabric Loader |

</div>

<div align="center">

## Project Structure (meteor-addon)

For the universal-harness layout, see [`universal/README.md`](universal/README.md).

</div>

```
meteor-addon/src/main/java/io/mcdxai/harness/
├── MeteorTestHarnessAddon.java     # Addon entry point (MeteorAddon subclass)
├── HarnessRuntime.java             # MCP server lifecycle (start/stop)
├── config/
│   ├── HarnessConfig.java          # Settings (host, port, session mode)
│   └── HarnessConfigRuntimeApplier.java  # Config change handlers
├── dom/
│   ├── DomSnapshot.java            # DOM snapshot model
│   ├── DomSnapshotBuilder.java     # Builds DOM from screen widget tree
│   ├── DomQueryEngine.java         # Filter/search DOM elements
│   ├── DomInteractor.java          # Click, scroll, drag dispatch
│   ├── DomActionHints.java         # Action metadata for DOM elements
│   ├── DomMetadataHelper.java      # Widget metadata extraction
│   ├── DomEntryListHelper.java     # Entry list widget support
│   ├── DomKeyCodec.java            # Key name ↔ GLFW keycode mapping
│   ├── DomValueUtils.java          # Value coercion for settings
│   ├── ElementRef.java             # Typed element reference
│   └── MeteorModuleMetadata.java   # Module metadata extraction
├── gui/
│   └── HarnessTab.java             # Meteor GUI tab for config
├── mcp/
│   ├── McpServer.java              # Tomcat + MCP server bootstrap
│   ├── McpRegistry.java            # Wires up all tools and resources
│   ├── RegistryContext.java        # Shared context (services, session gate)
│   ├── SessionGate.java            # Single-session ownership lock
│   ├── ToolSchemas.java            # JSON schema definitions
│   ├── EmbeddedWebappClassLoader.java  # Tomcat classloader bridge
│   └── tools/
│       ├── CoreTools.java          # Harness status/debug/release
│       ├── MeteorInfoTools.java    # Meteor info/addon/HUD queries
│       ├── ModuleTools.java        # Module CRUD
│       ├── WorldStateTools.java    # Player/world/inventory queries
│       ├── WorldActionTools.java   # Chat/command/disconnect actions
│       ├── PathingTools.java       # Baritone pathing control
│       ├── DomQueryTools.java      # DOM snapshot/query tools
│       ├── DomInteractionTools.java  # DOM click/scroll/drag
│       ├── DomInputTools.java      # DOM text input/keypress
│       ├── DomToolHelper.java      # Shared DOM result wrapping
│       └── Resources.java          # MCP resource registrations
├── services/
│   ├── ScreenDomService.java       # DOM engine: snapshot, click, setText, setValue
│   ├── ModuleService.java          # Meteor module CRUD
│   ├── GameStateService.java       # Player/world/inventory/entity state
│   ├── PathingService.java         # Baritone/Meteor PathManager integration
│   ├── ChatLogService.java         # Chat capture and history
│   ├── MeteorInfoService.java      # Meteor environment introspection
│   ├── HarnessService.java         # Harness lifecycle operations
│   ├── NameMappingService.java     # Yarn ↔ intermediary name resolution
│   └── SettingValueCodec.java      # Serialize/deserialize Meteor settings
├── mixin/
│   └── KeyboardInvoker.java        # Accessor mixin for key dispatch
└── util/
    ├── MainThreadInvoker.java      # Dispatch to Minecraft render thread
    ├── McpResults.java             # Tool result construction
    └── ArgReader.java              # MCP tool argument parsing
```

<div align="center">

## Architecture

- **Thread safety** — All tool handlers run on Minecraft's render thread via `MainThreadInvoker`. Never call Minecraft APIs from the MCP servlet thread directly.
- **DOM-first interaction** — Screens are snapshotted into a structured DOM tree. Agents query and interact with elements by ID or filters rather than raw pixel coordinates.
- **Streamable HTTP transport** — The MCP server uses the Streamable HTTP protocol over an embedded Tomcat servlet container. No stdio, no WebSocket — just HTTP POST with optional streaming responses.

## License

This project is licensed under the [CC0-1.0 license](LICENSE).

</div>

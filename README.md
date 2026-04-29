# meteor-test-harness

Meteor addon that exposes a local MCP HTTP server for automated, DOM-first testing of Meteor Client and Meteor addons.

Target stack for v0:
- Minecraft `26.1.2`
- Meteor Client `26.1.2-SNAPSHOT`
- Fabric Loader `0.19.2`
- Java `25`
- MCP SDK `1.1.1` using HTTP servlet transport

## v0 Scope

- Local singleplayer test harness.
- Single active MCP session ownership by default.
- No screenshot pipeline in v0.
- UI interaction is DOM-first via screen tree tools (`get_screen_dom`, `click_dom_element`, etc.).
- Movement/pathing delegated to Meteor PathManager/Baritone integration.
- Module and setting control for Meteor + addon modules.
- Structured game-state streams for player/world/inventory/entities/pathing/chat.

## Build

```bash
./gradlew build
```

## Runtime Notes

- The addon starts an embedded Tomcat server and mounts MCP endpoint at `bindHost:bindPort + mcpEndpoint`.
- Default config values:
- `bind-host`: `127.0.0.1`
- `bind-port`: `38861`
- `mcp-endpoint`: `/mcp`
- `single-session-mode`: `true`
- `auto-start`: `true`

## MCP Tools (v0)

Session/runtime:
- `get_harness_status`
- `release_session`

Modules/settings:
- `list_modules`
- `get_module`
- `set_module_state`
- `list_module_settings`
- `get_module_setting`
- `set_module_setting`

State streams:
- `get_player_state`
- `get_world_state`
- `get_player_inventory`
- `get_crosshair_target`
- `get_nearby_entities`

DOM engine:
- `get_screen_dom`
- `click_dom_element`
- `set_dom_text`
- `set_dom_value`
- `navigate_back`

Chat/commands:
- `send_chat`
- `send_command`
- `disconnect_world`
- `get_chat_history`
- `clear_chat_history`

Pathing:
- `get_pathing_status`
- `pathing_move_to`
- `pathing_move_in_direction`
- `pathing_pause`
- `pathing_resume`
- `pathing_stop`

## MCP Resources (v0)

- `meteor://modules`
- `meteor://state/player`
- `meteor://state/world`
- `meteor://state/crosshair`
- `meteor://state/entities`
- `meteor://state/pathing`
- `meteor://state/screen-dom`
- `meteor://chat/history`

## Project Layout

- `src/main/java/com/mcdxai/meteortestharness/mcp` MCP server bootstrap, tool/resource registry, session gate.
- `src/main/java/com/mcdxai/meteortestharness/services` domain services for modules, DOM mapping, pathing, game state, chat.
- `src/main/java/com/mcdxai/meteortestharness/config` addon config system.
- `src/main/resources/fabric.mod.json` Fabric/Meteor addon metadata.

# Meteor Test Harness v0 Spec

## Locked Decisions

- Addon name: `meteor-test-harness`
- Target versions: Minecraft `1.21.11`, Meteor Client `1.21.11-SNAPSHOT`
- Transport: MCP SDK HTTP servlet transport (embedded Tomcat)
- Environment: local singleplayer test environment
- Session policy: single active MCP session by default
- UI policy: DOM-only interaction for model actions
- Screenshot delivery: out of scope for v0

## Tool Surface (v0)

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

DOM tools:
- `get_screen_dom`
- `click_dom_element`
- `set_dom_text`
- `set_dom_value`
- `navigate_back`

Chat + control:
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

## Resources (v0)

- `meteor://modules`
- `meteor://state/player`
- `meteor://state/world`
- `meteor://state/crosshair`
- `meteor://state/entities`
- `meteor://state/pathing`
- `meteor://state/screen-dom`
- `meteor://chat/history`

## Open Questions for Next Phase

- Should we expose a dedicated action tool for hotbar slot selection and item use, or keep this through module/state automation only?
- Should entity streams include optional richer filters (hostile/passive/player/projectile) in v0.1?
- Should we add a tick-level `wait_for` primitive (conditions + timeout) in v0.1 for deterministic test sequencing?
- Should we include a lightweight test-run annotation stream (step markers, assertions) as first-party MCP tools?

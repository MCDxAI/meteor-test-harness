# MCP Tool Registry

> Complete reference for all tools exposed by the Meteor Test Harness MCP server.
>
> **40 tools** across 6 registration files. Organized by functional domain.

---

## Core & Session

> Harness runtime status, diagnostics, and session lock management.
>
> **Source:** `src/main/java/com/mcdxai/meteortestharness/mcp/HarnessCoreTools.java`

### `get_harness_status`

Get harness runtime/session status.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| — | — | — | No arguments. |

**Source:** `HarnessCoreTools.java:13-14`

---

### `get_harness_debug_info`

Get harness diagnostics (mapping/input internals).

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| — | — | — | No arguments. |

**Source:** `HarnessCoreTools.java:16-17`

---

### `release_session`

Release current session ownership lock.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| — | — | — | No arguments. |

**Source:** `HarnessCoreTools.java:19-23`

---

## Screen DOM

> Read, query, and interact with the current Minecraft screen's widget tree.
> DOM tools operate on snapshot-based element trees with id-based targeting or query-based filters.
>
> **Source:** `src/main/java/com/mcdxai/meteortestharness/mcp/HarnessDomTools.java`

### `get_screen_dom`

Get current DOM tree for active screen.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| — | — | — | No arguments. |

**Source:** `HarnessDomTools.java:17-18`

---

### `get_screen_dom_summary`

Get a compact summary for the current or latest DOM snapshot.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `refresh` | boolean | No | Capture a fresh snapshot before summarizing. Default true. |

**Source:** `HarnessDomTools.java:20-28`

---

### `find_dom_elements`

Query DOM elements server-side using filters and return only matched records.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `snapshot_id` | string | No | Optional snapshot id from `get_screen_dom`. |
| `filters` | object | No | Filter object (label/moduleName/role/actions/text/etc). |
| `limit` | integer | No | Maximum matched elements to return. Default 32. |
| `fields` | array | No | Optional field whitelist for each returned element. |
| `include_children` | boolean | No | Include children/subtrees for each result. |

**Source:** `HarnessDomTools.java:30-57`

---

### `get_dom_element`

Get one DOM element by id from a snapshot (or latest snapshot).

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `snapshot_id` | string | No | Optional snapshot id from `get_screen_dom`. |
| `element_id` | string | **Yes** | Element id. |
| `fields` | array | No | Optional field whitelist for returned element. |
| `include_children` | boolean | No | Include nested children for this element. |

**Source:** `HarnessDomTools.java:59-84`

---

### `get_dom_subtree`

Get an element subtree by id with bounded depth.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `snapshot_id` | string | No | Optional snapshot id from `get_screen_dom`. |
| `element_id` | string | **Yes** | Root element id. |
| `depth` | integer | No | Child depth to include. Default 2. |
| `fields` | array | No | Optional field whitelist for nodes in the subtree. |

**Source:** `HarnessDomTools.java:86-111`

---

### `click_dom_query`

Find a DOM element with filters and click it atomically.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `filters` | object | **Yes** | Filter object used to select element(s). |
| `index` | integer | No | Match index to click. Default 0. |
| `button` | integer | No | Mouse button code. 0=left, 1=right, 2=middle. Default 0. |
| `double_click` | boolean | No | Whether to send click as double-click. |

**Source:** `HarnessDomTools.java:113-138`

---

### `set_dom_text_query`

Find a DOM text-capable element with filters and set text atomically.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `filters` | object | **Yes** | Filter object used to select element(s). |
| `text` | string | **Yes** | Text to apply. |
| `index` | integer | No | Match index. Default 0. |
| `submit` | boolean | No | Press Enter after setting text. |
| `type_characters` | boolean | No | Type through char events instead of direct assignment. |
| `clear_first` | boolean | No | Clear current text before typing. |

**Source:** `HarnessDomTools.java:140-169`

---

### `click_dom_element`

Click a DOM element by id.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `element_id` | string | **Yes** | Element id from `get_screen_dom`. |
| `button` | integer | No | Mouse button code. 0=left, 1=right, 2=middle. Default 0. |
| `double_click` | boolean | No | Whether to send click as double-click. |

**Source:** `HarnessDomTools.java:171-193`

---

### `set_dom_text`

Set text content on a DOM text input by id.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `element_id` | string | **Yes** | Element id from `get_screen_dom`. |
| `text` | string | **Yes** | Text to set. |
| `submit` | boolean | No | Press Enter after setting text. |
| `type_characters` | boolean | No | Type through char events instead of direct assignment. |
| `clear_first` | boolean | No | Clear current text before typing. |

**Source:** `HarnessDomTools.java:195-221`

---

### `type_dom_text`

Type text into a DOM element through keyboard char events.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `element_id` | string | **Yes** | Element id from `get_screen_dom`. |
| `text` | string | **Yes** | Text to type. |
| `clear_first` | boolean | No | Clear existing text first. Default true. |
| `submit` | boolean | No | Press Enter after typing. |

**Source:** `HarnessDomTools.java:223-247`

---

### `scroll_dom_element`

Scroll at a DOM element location (or screen center if no element id is provided).

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `element_id` | string | No | Optional element id from `get_screen_dom`. |
| `vertical` | number | No | Vertical scroll amount. Positive/negative follows Minecraft screen semantics. |
| `horizontal` | number | No | Horizontal scroll amount. |

**Source:** `HarnessDomTools.java:249-271`

---

### `drag_dom_element`

Drag from the center of a DOM element by offsets.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `element_id` | string | **Yes** | Element id from `get_screen_dom`. |
| `offset_x` | number | **Yes** | Drag offset on X axis in screen pixels. |
| `offset_y` | number | **Yes** | Drag offset on Y axis in screen pixels. |
| `steps` | integer | No | Number of drag interpolation steps. Default 8. |
| `button` | integer | No | Mouse button code. Default 0 (left). |

**Source:** `HarnessDomTools.java:273-299`

---

### `press_screen_key`

Send a key press/release. Targets active screen when present, otherwise uses global in-game key handling.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `key` | string | **Yes** | Key name (e.g. ENTER, ESCAPE, TAB, UP, A, F5). |
| `modifiers` | integer | No | Modifier bitmask. Default 0. |
| `repeat` | integer | No | Number of keyPressed repeats. Default 1. |
| `release` | boolean | No | Whether to send keyReleased after presses. Default true. |

**Source:** `HarnessDomTools.java:301-325`

---

### `set_dom_value`

Set value on a DOM control (checkbox/slider) by id.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `element_id` | string | **Yes** | Element id from `get_screen_dom`. |
| `value` | object | **Yes** | Value payload. |

**Source:** `HarnessDomTools.java:327-344`

---

### `navigate_back`

Close current screen/go back.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| — | — | — | No arguments. |

**Source:** `HarnessDomTools.java:346-347`

---

## Module Management

> List, inspect, toggle, and configure Meteor Client modules and their settings.
>
> **Source:** `src/main/java/com/mcdxai/meteortestharness/mcp/HarnessModuleTools.java`

### `list_modules`

List all Meteor and addon modules.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `include_settings` | boolean | No | Include each module's full settings tree. |

**Source:** `HarnessModuleTools.java:18-26`

---

### `get_module`

Get one module and optionally its settings.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `module_name` | string | **Yes** | Module name/title. |
| `include_settings` | boolean | No | Include settings tree. |

**Source:** `HarnessModuleTools.java:28-43`

---

### `set_module_state`

Enable or disable a module.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `module_name` | string | **Yes** | Module name/title. |
| `active` | boolean | **Yes** | Desired active state. |

**Source:** `HarnessModuleTools.java:45-64`

---

### `list_module_settings`

List settings for a module.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `module_name` | string | **Yes** | Module name/title. |

**Source:** `HarnessModuleTools.java:66-78`

---

### `get_module_setting`

Get one setting from a module.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `module_name` | string | **Yes** | Module name/title. |
| `setting_name` | string | **Yes** | Setting name/title. |

**Source:** `HarnessModuleTools.java:80-99`

---

### `set_module_setting`

Set one module setting value.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `module_name` | string | **Yes** | Module name/title. |
| `setting_name` | string | **Yes** | Setting name/title. |
| `value` | any | **Yes** | New value. Scalars/maps/lists supported depending on setting type. |

**Source:** `HarnessModuleTools.java:101-125`

---

## World State

> Read-only queries about the player, world, inventory, and entities.
>
> **Source:** `src/main/java/com/mcdxai/meteortestharness/mcp/HarnessWorldStateTools.java`

### `get_player_state`

Get core player state (position, vitals, movement flags, effects).

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| — | — | — | No arguments. |

**Source:** `HarnessWorldStateTools.java:17-18`

---

### `get_world_state`

Get current world state stream.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| — | — | — | No arguments. |

**Source:** `HarnessWorldStateTools.java:20-21`

---

### `get_player_inventory`

Get granular player inventory slices (hotbar/main/row/range/armor/offhand/hands/selected/all).

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `section` | string | No | Inventory section: all, inventory, hotbar, main, row, range, selected, armor, offhand, hands. |
| `row` | integer | No | Main inventory row index (0-2). Used when section=row. |
| `slot_start` | integer | No | Start slot index. Used when section=range. |
| `slot_end` | integer | No | End slot index. Used when section=range. |
| `include_empty` | boolean | No | Include empty slots in slot results. Default false. |

**Source:** `HarnessWorldStateTools.java:23-43`

---

### `get_crosshair_target`

Get the current crosshair hit target only.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| — | — | — | No arguments. |

**Source:** `HarnessWorldStateTools.java:45-46`

---

### `get_nearby_entities`

Get nearby entities around the player.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `radius` | number | No | Search radius in blocks. Default 32. |
| `max_count` | integer | No | Maximum entities to return. Default 64. |

**Source:** `HarnessWorldStateTools.java:48-64`

---

## World Actions

> Send chat, commands, and manage chat history. Disconnect from world/server.
>
> **Source:** `src/main/java/com/mcdxai/meteortestharness/mcp/HarnessWorldActionTools.java`

### `send_chat`

Send chat message as player.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `message` | string | **Yes** | Chat message text. |

**Source:** `HarnessWorldActionTools.java:18-35`

---

### `send_command`

Send command as player.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `command` | string | **Yes** | Command with or without leading slash. |

**Source:** `HarnessWorldActionTools.java:37-56`

---

### `disconnect_world`

Disconnect from current world/server.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| — | — | — | No arguments. |

**Source:** `HarnessWorldActionTools.java:58-63`

---

### `get_chat_history`

Get captured chat history.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `count` | integer | No | Number of newest lines to return. |

**Source:** `HarnessWorldActionTools.java:65-70`

---

### `clear_chat_history`

Clear captured chat history buffer.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| — | — | — | No arguments. |

**Source:** `HarnessWorldActionTools.java:72-77`

---

## Pathing

> Baritone/Meteor PathManager integration — move to coordinates, move in a direction, pause/resume/stop.
>
> **Source:** `src/main/java/com/mcdxai/meteortestharness/mcp/HarnessPathingTools.java`

### `get_pathing_status`

Get Baritone/PathManager status.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| — | — | — | No arguments. |

**Source:** `HarnessPathingTools.java:18-19`

---

### `pathing_move_to`

Move player to target coordinates using PathManager/Baritone.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `x` | integer | **Yes** | Target block X. |
| `y` | integer | **Yes** | Target block Y. |
| `z` | integer | **Yes** | Target block Z. |
| `ignore_y` | boolean | No | Ignore Y and path in XZ only. |

**Source:** `HarnessPathingTools.java:21-49`

---

### `pathing_move_in_direction`

Move player continuously in a yaw direction.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `yaw` | number | **Yes** | Yaw in degrees. |

**Source:** `HarnessPathingTools.java:51-66`

---

### `pathing_pause`

Pause current pathing process.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| — | — | — | No arguments. |

**Source:** `HarnessPathingTools.java:68-74`

---

### `pathing_resume`

Resume paused pathing process.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| — | — | — | No arguments. |

**Source:** `HarnessPathingTools.java:76-82`

---

### `pathing_stop`

Stop current pathing process.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| — | — | — | No arguments. |

**Source:** `HarnessPathingTools.java:84-90`

---

## Summary

| Group | Tools | Source File |
|-------|-------|-------------|
| Core & Session | 3 | `HarnessCoreTools.java` |
| Screen DOM | 15 | `HarnessDomTools.java` |
| Module Management | 6 | `HarnessModuleTools.java` |
| World State | 5 | `HarnessWorldStateTools.java` |
| World Actions | 5 | `HarnessWorldActionTools.java` |
| Pathing | 6 | `HarnessPathingTools.java` |
| **Total** | **40** | — |

All source files are under `src/main/java/com/mcdxai/meteortestharness/mcp/`.

All tools are registered via `HarnessMcpRegistry.java` which delegates to each group's `register()` static method.
Tool schemas are built using helpers from `ToolSchemas.java`.
All handlers run on Minecraft's render thread via `MainThreadInvoker`.

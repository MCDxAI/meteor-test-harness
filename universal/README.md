<div align="center">

# Universal Test Harness

![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-00800f?style=flat)
![Fabric](https://img.shields.io/badge/Fabric_Loader-0.19.2-3d5dff?style=flat)
![owo-lib](https://img.shields.io/badge/owo--lib-0.13.0%2B26.1-c08bd6?style=flat)
![Java](https://img.shields.io/badge/Java-25-e28655?style=flat)
![MCP SDK](https://img.shields.io/badge/MCP_SDK-1.1.1-1a9f5c?style=flat)

**Engine-agnostic MCP HTTP server for automated GUI testing of Fabric mods. Vanilla + owo-lib + hybrid screen support, with name-based decorators for soft mod dependencies.**

</div>

## What This Is

A standalone Fabric client mod (sibling of [`meteor-addon`](../meteor-addon/)) that embeds an MCP Streamable HTTP server at `127.0.0.1:38862/mcp`. Where the Meteor variant targets Meteor Client internals, this one targets the broader Fabric ecosystem: an LLM agent can introspect and drive any screen built on vanilla Minecraft widgets, owo-lib components, or a mix of the two.

The driving use case is automated regression and exploration testing of arbitrary Fabric mods that ship GUIs — for example [item-editor](https://github.com/Glisco/owo-item-editor) — without writing per-mod glue.

## Scope (v0)

- Vanilla widgets: `Button`, `Checkbox`, `Slider`, `CycleButton`, `EditBox`, `AbstractContainerScreen` slots
- owo-lib components: `ButtonComponent`, `CheckboxComponent`, `TextBoxComponent`, `RichTextAreaComponent`, `DropdownComponent`, `SliderComponent`, `ModalOverlayLayout`, `StackLayout`/`FlowLayout` containers, `InputSafeScrollContainer`
- Hybrid screens: vanilla container + owo overlay, owo UIAdapter mounted on a vanilla `Screen`
- Overlay handling: dropdowns and modals are reported as topmost; the rest of the tree is marked `occludedByOverlay: true`
- Container-slot enumeration with `modOrigin` decorator (e.g. `itemeditor`) for soft mod identification
- 3-phase settle pattern (dispatch → off-main settle → finalize) so `screenChanged` reflects real post-click state

## Quick Start

| Step | Instructions |
| --- | --- |
| **Requirements** | Java 25, Minecraft 26.1.2, Fabric Loader 0.16.0+, Fabric API. owo-lib 0.13.0+26.1 recommended (any owo screen support disables silently if missing). |
| **Build** | From repo root: `./gradlew :universal:build` — JAR lands in `universal/build/libs/`. |
| **Install** | Drop the JAR into `.minecraft/mods/`. No Meteor Client required. |
| **Connect** | MCP Streamable HTTP at `http://127.0.0.1:38862/mcp`. |

## Architecture

```
universal/src/main/java/io/mcdxai/harness/universal/
  UniversalHarnessMod.java         — Fabric ClientModInitializer entry point
  HarnessRuntime.java              — MCP server lifecycle
  config/                          — bind host/port, single-session, auto-start
  adapter/
    ScreenEngine.java              — engine interface (vanilla / owo / hybrid)
    WidgetAdapter.java             — per-widget metadata + setValue/setText handlers
    AdapterRegistry.java           — priority-ordered engine + widget adapter lookup
    vanilla/                       — VanillaScreenEngine, VanillaWidgetAdapters
    owo/                           — OwoScreenEngine, OwoWidgetAdapters (soft-dep)
    hybrid/                        — HybridScreenEngine (vanilla shell + owo overlay)
  dom/                             — DomSnapshot, DomQueryEngine, DomInteractor
  modspec/                         — name-based decorators (modOrigin tagging)
  mcp/                             — McpServer (Tomcat), McpRegistry, tools/
  mixin/                           — @Pseudo accessors for soft-dep classes
  services/                        — ScreenDomService, GameStateService, ChatLogService
  util/                            — MainThreadInvoker, ArgReader, McpResults
```

### Engine priorities

| Engine | Priority | Matches |
| --- | --- | --- |
| `vanilla` | 0 | Any `Screen` subclass not handled by a higher-priority engine |
| `hybrid` | 5 | Vanilla `Screen` with an attached owo `UIAdapter` overlay |
| `owo` | 10 | `BaseOwoScreen` and subclasses |

Element IDs are engine-prefixed: `v-` (vanilla), `o-` (owo), `s-` (snapshot-only/structural).

### Soft-dep handling

owo-lib is a `compileOnly` dependency and `recommends` in `fabric.mod.json`. All owo class references in the adapter and mixin layers are isolated behind `@Pseudo` mixins and lazy lookups so the mod loads cleanly when owo is absent — the owo engine simply unregisters itself.

## MCP Tools (v0)

The tool surface mirrors `meteor-addon` minus Meteor-specific tools (no module CRUD, no Baritone pathing).

| Category | Tools |
| --- | --- |
| Session/runtime | `get_harness_status`, `get_harness_debug_info`, `release_session`, `list_supported_engines` |
| World state | `get_player_state`, `get_world_state`, `get_player_inventory`, `get_crosshair_target`, `get_nearby_entities` |
| Chat/commands | `send_chat`, `send_command`, `disconnect_world` |
| DOM queries | `get_screen_dom`, `get_screen_dom_summary`, `find_dom_elements`, `get_dom_element`, `get_dom_subtree` |
| DOM interaction | `click_dom_element`, `click_dom_query`, `scroll_dom_element`, `drag_dom_element`, `navigate_back` |
| DOM input | `set_dom_text`, `set_dom_text_query`, `type_dom_text`, `set_dom_value`, `press_screen_key` |

## Known Limitations (v0)

- **No Meteor Client integration.** Use [`meteor-addon`](../meteor-addon/) for module/setting CRUD, HUD introspection, and Baritone pathing.
- **owo `RichTextAreaComponent` is not an `EditBox`** — `set_dom_text` will return `element_does_not_accept_text`. Click to focus, then use `type_dom_text` (charTyped path).
- **Tiny owo checkboxes** (width ~16) inside nested `StackLayout`/`ScrollContainer` trees may not route the screen-level press through OwoUIAdapter hit-testing. The adapter falls back to a direct `widget.mouseClicked` dispatch — verified working on item-editor's Stack Count / Unbreakable / custom-model toggles.
- **No mixin-level input injection.** Key events are delivered via `Screen.keyPressed`/`charTyped`, not the Keyboard manager — global hotkeys outside the focused screen are out of scope for v0.

## Configuration

Defaults are baked into `HarnessConfig`. There is no in-game settings UI yet (no Meteor tab to host one); change defaults at build time or extend `HarnessConfig` to read from a file.

| Setting | Default |
| --- | --- |
| `bind-host` | `127.0.0.1` |
| `bind-port` | `38862` |
| `mcp-endpoint` | `/mcp` |
| `auto-start` | `true` |
| `single-session-mode` | `true` |

## See Also

- [Root README](../README.md)
- [`meteor-addon/`](../meteor-addon/) — the Meteor Client variant
- [`docs/v0-research.md`](../docs/v0-research.md) — design notes and engine-survey research that shaped v0

<div align="center">

# MC Test Harness

![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-00800f?style=flat)
![Fabric](https://img.shields.io/badge/Fabric_Loader-0.19.2-3d5dff?style=flat)
![owo-lib](https://img.shields.io/badge/owo--lib-0.13.0%2B26.1-c08bd6?style=flat)
![Java](https://img.shields.io/badge/Java-25-e28655?style=flat)
![MCP SDK](https://img.shields.io/badge/MCP_SDK-1.1.1-1a9f5c?style=flat)

**MCP server embedded in Minecraft for LLM-driven automation. Drives screens as a DOM tree, queries game state, and runs commands over an HTTP endpoint. Ships two Fabric mods: a Meteor variant and an engine-agnostic Universal variant.**

</div>

<div align="center">

## Variants

| Module | Mod ID | Port | Targets |
| --- | --- | --- | --- |
| [`meteor-addon/`](meteor-addon/) | `mc-test-harness-meteor` | `38861` | Meteor Client — module CRUD, HUD, Baritone pathing, DOM over vanilla screens |
| [`universal/`](universal/) | `mc-test-harness-universal` | `38862` | Engine-agnostic Fabric mod — DOM over vanilla + owo-lib + hybrid screens, no Meteor required |

</div>

<div align="center">

## Features

### Meteor variant (`mc-test-harness-meteor`)

| Capability | Details |
| --- | --- |
| **DOM-first screen interaction** | Snapshot any screen into a DOM tree, query by label/role/type, click/scroll/drag/type. |
| **Module & setting control** | List, inspect, toggle, and configure every Meteor and addon module with full settings CRUD. |
| **Baritone pathing** | Move to coordinates or cardinal directions; pause/resume/stop; wait for completion. |
| **Meteor introspection** | Version, build, installed addons, module/HUD counts, active HUD elements. |
| **Game-state queries** | Player position/vitals/effects, world info, inventory, crosshair target, nearby entities. |
| **Chat & commands** | Send chat or slash commands; capture and query chat history. |
| **In-game config tab** | Settings live in Meteor GUI → Test Harness. |

### Universal variant (`mc-test-harness-universal`)

| Capability | Details |
| --- | --- |
| **DOM-first screen interaction** | Same DOM model as the Meteor variant, plus owo-lib component support and hybrid screen handling. |
| **Engine-agnostic** | Vanilla, owo-lib, and hybrid screens via priority-ordered engine adapters (`v-` / `o-` / `s-` element prefixes). |
| **Soft mod-aware** | Container slots and screens decorate elements with `modOrigin` for soft mod identification (e.g. `itemeditor`). |
| **Overlay handling** | Dropdowns and modals report as topmost; everything else marked `occludedByOverlay`. |
| **Game-state queries** | Player position/vitals, world info, inventory, crosshair target, nearby entities. |
| **Chat & commands** | Send chat or slash commands. |
| **No Meteor required** | Standalone Fabric mod; owo-lib is `recommends` (Universal degrades cleanly if absent). |

### Shared infrastructure

| Capability | Details |
| --- | --- |
| **Single-session ownership** | Configurable session gate ensures one agent owns the harness at a time. |
| **Embedded HTTP server** | Tomcat 11.0.13 serves MCP via Streamable HTTP — no external process needed. |
| **Render-thread safety** | All tool handlers dispatch to Minecraft's main thread automatically. |

</div>

<div align="center">

## Quick Start

| Requirement | Version |
| --- | --- |
| Java | 25+ |
| Minecraft | 26.1.2 |
| Fabric Loader | 0.19.2+ |
| Meteor Client | 26.1.2-SNAPSHOT (Meteor variant only) |
| owo-lib | 0.13.0+26.1 (Universal variant, recommended) |

### Install

1. Download the appropriate `.jar` from [releases](https://github.com/MCDxAI/mc-test-harness/releases) — pick **`mc-test-harness-meteor`** for Meteor integration or **`mc-test-harness-universal`** for general Fabric-mod GUI testing.
2. Drop into `.minecraft/mods/` (alongside Meteor Client if using the Meteor variant).
3. Launch the Fabric profile.

### Connect

| Variant | MCP endpoint |
| --- | --- |
| Meteor | `http://127.0.0.1:38861/mcp` |
| Universal | `http://127.0.0.1:38862/mcp` |

Both speak MCP Streamable HTTP transport and auto-start on launch (configurable). The two can run side-by-side.

</div>

<div align="center">

## Configuration

### Meteor variant

Settings live in **Meteor GUI → Test Harness**.

| Setting | Default | Description |
| --- | --- | --- |
| `bind-host` | `127.0.0.1` | Host/IP for the MCP HTTP server |
| `bind-port` | `38861` | Port for the MCP HTTP endpoint |
| `mcp-endpoint` | `/mcp` | HTTP endpoint path |
| `auto-start` | `true` | Start the server on Meteor init |
| `single-session-mode` | `false` | Restrict to one active MCP session at a time |
| `keep-alive-seconds` | `30` | Connection keep-alive interval |
| `request-timeout-seconds` | `30` | Max execution time per MCP tool call |
| `chat-history-limit` | `200` | Captured chat lines retained in memory |

### Universal variant

JSON file at `config/mc-test-harness-universal.json` (auto-created on first run). Same field set as the Meteor variant minus `chat-history-limit`, default port `38862`. See [`universal/README.md`](universal/README.md#configuration).

</div>

<div align="center">

## MCP Tools and Resources

Tool surfaces differ between variants. Full references:

| Variant | Tool reference | Resource scheme |
| --- | --- | --- |
| Meteor (`mc-test-harness-meteor`) | [`docs/TOOLS.md`](docs/TOOLS.md) | `meteor://` (modules, state, chat) |
| Universal (`mc-test-harness-universal`) | [`universal/README.md`](universal/README.md#mcp-tools-v0) | `harness://` (state only) |

Shared baseline both variants expose: harness session/runtime, world state, chat/commands, DOM queries, DOM interaction, DOM input.

</div>

<div align="center">

## Development

| Task | Command |
| --- | --- |
| **Build all** | `./gradlew build` — both subprojects |
| **Build one** | `./gradlew :meteor-addon:build` or `./gradlew :universal:build` |
| **Clean build** | `./gradlew clean build` |
| **Run Meteor client** | `./gradlew :meteor-addon:runClient` |
| **Run Universal client** | `./gradlew :universal:runClient` |

JARs land in each subproject's `build/libs/`. Dependencies: MCP SDK 1.1.1 + Embedded Tomcat 11.0.13 (bundled); Meteor Client / owo-lib (provided per variant).

</div>

<div align="center">

## License

This project is licensed under the [CC0-1.0 license](LICENSE).

</div>

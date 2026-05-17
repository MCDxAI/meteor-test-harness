# MC Test Harness (Universal) — Live Dogfood Test Suite

This document is the **complete brief** for a fresh Claude Code session whose job is to dogfood the `mc-test-harness-universal` v0 against a running Minecraft client. Read it top-to-bottom before invoking any tool.

You are continuing work on a Fabric mod that just shipped v0. The point of this session is **not** to add features — it is to find real bugs and rough edges by driving the harness through end-to-end flows that nobody has scripted yet.

---

## 1. What the harness is

`mc-test-harness-universal` is a Fabric client mod that embeds an MCP Streamable HTTP server inside Minecraft. An LLM agent (you) connects via MCP and drives the game: introspect screens as DOM trees, click widgets, type into fields, run commands, read world state.

Three screen engines, priority-ordered:

| Engine  | Priority | Element prefix | Matches                                           |
| ------- | -------- | -------------- | ------------------------------------------------- |
| vanilla | 0        | `v-`           | any vanilla `Screen` not handled by a higher one  |
| hybrid  | 5        | `s-` / mixed   | vanilla shell + owo `UIAdapter` overlay           |
| owo     | 10       | `o-`           | `BaseOwoScreen` and subclasses                    |

You will run against:
- **Minecraft 26.1.2** (unobfuscated), Fabric Loader 0.19.2, owo-lib 0.13.0+26.1
- The harness mod itself (you are testing it)
- **item-editor** (owo-lib mod by Glisco) — the primary GUI-under-test
- Possibly other Fabric mods loaded in the dev profile — discover via `get_screen_dom` and act accordingly

## 2. Environment when you start

The user will have:
- Booted Minecraft to the **main menu** (Title Screen).
- Done nothing else.

The harness's MCP endpoint is already configured in this repo's `.mcp.json` (`http://127.0.0.1:38862/mcp`). It should appear as the `mc-test-harness-universal` server with tools prefixed `mcp__mc-test-harness-universal__…`. If those tools are not visible in your tool list, **stop and tell the user** — the mod did not load, or the port is wrong.

You are responsible for everything else: navigating menus, joining the world, opening GUIs, executing the scenarios below, and writing up findings.

## 3. Hard constraints

- **Single-session mode is on.** Once you claim the session, only you can drive it. If you ever see `session_locked` errors, call `release_session` and re-acquire.
- **Render-thread safety is handled for you** by the harness — every tool dispatches to the client thread. You do not need to worry about timing inside a single tool call, but back-to-back calls may race against animation/transition state. When a click "looks like it should have worked but didn't," re-snapshot before retrying — the screen probably changed under you.
- **Element IDs are snapshot-scoped.** A `snapshotId` is returned with every DOM read; element IDs from snapshot N may not exist in snapshot N+1 if the screen recomposed. Always re-query after a screen change.
- **Do NOT modify code.** This session is read-only against the codebase except for writing a findings file at the end. If you discover a bug, write it down — do not fix it.
- **Do NOT push, commit, or run gradle.** Just test.

## 4. Tool primer (the ones you will actually use)

Discovery:
- `list_supported_engines` — confirm vanilla/owo/hybrid all registered
- `get_harness_status` / `get_harness_debug_info` — health + diagnostics
- `get_screen_dom` — full DOM of active screen (verbose; use sparingly on deep screens)
- `get_screen_dom_summary` — compact summary; preferred first read on an unfamiliar screen
- `find_dom_elements` — **always pass a `filter` object** (e.g. `{"role": "checkbox"}`, `{"labelContains": "Play"}`, `{"actionsIncludes": "set_value"}`). The filter going missing is a known prior bug that was fixed — verifying it still works is part of the test.
- `get_dom_element` / `get_dom_subtree` — drill into one element / its descendants

Interaction:
- `click_dom_element` / `click_dom_query` — primary click path
- `scroll_dom_element` — for list widgets and scroll containers
- `drag_dom_element` — sliders
- `set_dom_value` — booleans for checkboxes, numbers for sliders, strings for cycle buttons. Returns post-state truth.
- `set_dom_text` — for `EditBox`/owo `TextBoxComponent` (sets text in one shot)
- `type_dom_text` — for things that aren't real EditBoxes (e.g. owo `RichTextAreaComponent`): click to focus first, then type
- `press_screen_key` — ENTER/ESCAPE/TAB/arrows on the focused screen
- `navigate_back` — closes the active screen

World/state:
- `get_player_state`, `get_world_state`, `get_player_inventory`, `get_crosshair_target`, `get_nearby_entities`
- `send_chat`, `send_command`, `disconnect_world`

## 5. Known gotchas from the v0 build (do not retest these as bugs — verify they still work)

- **`RichTextAreaComponent` is not an `EditBox`.** `set_dom_text` returns `element_does_not_accept_text`. Workaround: `click_dom_element` to focus, then `type_dom_text` with the desired string. Item-editor's item-name field is one of these.
- **Tiny owo checkboxes (width ~16) inside nested StackLayout/ScrollContainer.** The adapter dispatches a screen-level press first and silently falls back to `widget.mouseClicked` if the toggle didn't flip. The `set_dom_value` return value is authoritative; trust it.
- **Checkbox labels** are present on both vanilla and owo checkboxes. If you see a checkbox with no `label` field, that is a regression — flag it.
- **`find_dom_elements` filter** must be honored. If you pass `{"role": "container_slot"}` and get unfiltered results back, that is a regression of the prior ArgReader bug — flag it.
- **Error envelopes are lean** by design: failures return the target element's shallow row + a `snapshotId` pointer, not the full DOM. If you see a multi-thousand-token error response with the full tree dumped, flag it.
- **Overlays** (dropdowns, modals) appear as the topmost sibling and the rest of the tree is marked `occludedByOverlay: true`. Use this to know whether a normal click will land or be intercepted.

## 6. Test phases

Execute these in order. Stop and report immediately on any **structural** failure (mod not loaded, MCP not reachable, single-player button missing, etc). For **behavioral** failures, log them and continue.

### Phase 0 — Harness smoke

1. `get_harness_status` → expect `running: true`, session info
2. `list_supported_engines` → expect `vanilla`, `owo`, `hybrid` all present
3. `get_screen_dom_summary` → expect a vanilla `TitleScreen` (engine=vanilla, screen contains "Title")

### Phase 1 — Reach the testing world

The world is named **`testing`** (a single-player world the user has pre-created).

1. From Title Screen, find and click the "Singleplayer" button (use `find_dom_elements` with `{"labelContains": "Singleplayer"}` or similar — the label may be just "Singleplayer" or could be translated).
2. On the `SelectWorldScreen`, find the entry whose name contains "testing". These are vanilla list entries — selection requires a screen-level click at the entry's coordinates (the harness handles this, but if a direct `click_dom_element` on the entry does nothing, that is the documented vanilla-list quirk — try `click_dom_query` instead).
3. Once selected, click "Play Selected World" (or whatever the join button is labeled).
4. Wait for the world to load. Poll `get_player_state` until it returns a position (you may have to wait several seconds — Minecraft world load is not instant). Do NOT spam: 1-second intervals are fine. If you go 30 seconds with no player state, something is wrong — report and stop.

### Phase 2 — Open item-editor

You may not know the exact entry point. Try in this order, snapshotting after each:

1. `send_command` with `/item-editor` or `/itemeditor` — many owo mods expose a command.
2. If that fails, check the game's keybind list. The owo `Mod Menu`-style screen for item-editor (if present in the pause menu) may have a launch action. Open the pause menu (`press_screen_key` ESCAPE), look for an Options/Mods entry.
3. If you cannot find it, report what you tried and ask the user to open item-editor manually, then resume.

Once item-editor is open:
- `list_supported_engines` is engine-discovery; the actual engine of the active screen is in `get_screen_dom`'s `engine` field. Expect **owo** or **hybrid**.
- `get_screen_dom_summary` to map the layout. Item-editor has tabs (General, Display, Enchantments, NBT/Components, etc).

### Phase 3 — Item-editor flows (the meat)

Pick a starting item — diamond sword is a known good one. Goal: tweak it across multiple tabs and verify the harness can drive every interaction type. Each step is a real test; log success/failure for each.

1. **Set item name** (`RichTextAreaComponent`): click to focus, `type_dom_text` "Test Blade". Verify via the preview component if visible.
2. **Toggle checkboxes** on the General tab — at minimum: `Unbreakable`, anything related to render heads/sprites. Use `set_dom_value` with `true`. Verify each returns success and the `targetElement.checked` reflects the new state on a follow-up snapshot. This is the **tiny-checkbox regression test** — pay attention to any width ~16 checkbox.
3. **Modify stack count** — numeric text input. Use `set_dom_text` with a string like "16". If it's a slider instead of a text box, use `set_dom_value`/`drag_dom_element`.
4. **Open a dropdown** (e.g. rarity, or enchantment selector). Verify the dropdown appears as a topmost overlay and the rest of the tree is marked `occludedByOverlay: true`. Click an option, verify it commits.
5. **Add an enchantment** — full path: open enchantment tab/section, add (typically Sharpness V), verify it appears.
6. **Tab through the editor** — navigate to every tab once via clicks. After each tab switch, get the summary and confirm the engine and screen signature changed accordingly.
7. **Apply / Give** — whatever the commit-to-inventory action is. After applying, `get_player_inventory` and verify the modified item exists in the slot you expected, with the right name and properties (look at the NBT-ish fields the harness exposes).

### Phase 4 — Negative / edge-case probes

These are deliberate misuse to make sure error envelopes are sane and the harness fails gracefully.

1. `set_dom_value` on a non-toggleable element (e.g. a label) — expect `element_does_not_support_set_value` with a **lean** error response (no full DOM dump). Eyeball the response size.
2. `set_dom_text` on an element that doesn't accept text — expect `element_does_not_accept_text`, lean envelope.
3. `click_dom_element` with a stale `snapshotId`/`elementId` — expect a graceful "element not found" style error.
4. `find_dom_elements` with `{"role": "container_slot"}` on a screen with no containers — expect `totalMatches: 0`, not an unfiltered dump.
5. `find_dom_elements` with `{"role": "checkbox"}` on item-editor — confirm result count is reasonable and every returned element really is a checkbox.
6. `release_session` then immediately make a tool call — should fail with a clear session-required error, and you should be able to re-acquire by making a call (single-session gate semantics).

### Phase 5 — Free-form exploration

If everything above passes, explore. Things worth probing:
- Open the chat screen, type a message, send it — does the chat screen DOM expose the input cleanly?
- Open inventory (`E`), then a creative inventory if available — both are `AbstractContainerScreen` derivatives. Verify slots enumerate with `modOrigin` decorators.
- Open a chest if one exists in the world — verify slot enumeration there too.
- Try the pause menu and options screens — these are deeply nested vanilla screens; good stress test for the snapshot builder.

## 7. Output: write `TESTING-RESULTS.md`

When you're done (or if you hit a blocker), write a file at `TESTING-RESULTS.md` in the repo root with:

```
# Test run — <ISO date>

## Environment
- Engines detected: <list>
- Item-editor present: yes/no, opened via: <method>
- Notable other mods discovered:

## Phase results
For each phase 0–5:
- ✅ / ⚠️ / ❌ summary line
- For ⚠️/❌: what failed, the exact tool call and arguments, the error response

## Bugs found
For each: severity (blocker/major/minor/nit), reproduction steps (tool calls + args + expected vs actual), and a one-line hypothesis if you have one.

## UX rough edges
Things that worked but felt awkward — tools you wished existed, error messages that were unclear, filter shapes that surprised you, DOM fields that were missing.

## Suggestions for v0.1
Concrete, scoped — not "rewrite the engine."
```

Keep it terse. The user reads the diff; they do not need a victory lap.

## 8. When to stop and ask

- The harness MCP tools are not in your tool list at all.
- The mod is loaded but `get_harness_status` returns `running: false`.
- You cannot reach the testing world after 30s of polling, OR the world loads but `get_player_state` returns invalid data.
- You cannot find item-editor by any of the documented methods.
- Something destructive looks imminent (e.g. a "Delete World" button is the only thing labeled "testing" — sanity-check before clicking).

Otherwise, work through the phases autonomously and write the results file.

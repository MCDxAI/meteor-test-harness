# Test run — 2026-05-16

## Environment
- Engines detected: owo (priority 10), hybrid (priority 5), vanilla (priority 0) — all registered
- Item-editor present: **yes**, opened via **keybind `I`** (discovered through `KeyBindsScreen` after `/edit-item` / `/itemeditor` / `/item-editor` / `/edit` commands all silently no-op'd)
- Notable other mods discovered: only `itemeditor` (Glisco's owo-lib item editor, ID `itemeditor` from `me.noramibu.itemeditor.*`) — no other addon mods loaded in this dev profile beyond fabric-api + owo

## Phase results

### Phase 0 — Harness smoke ✅
- `get_harness_status`: running, port 38862, on TitleScreen
- `list_supported_engines`: all 3 present
- `get_screen_dom_summary`: vanilla TitleScreen, 8 elements
- ⚠️ minor: `singleSessionMode: false` (brief said it should be on by default)

### Phase 1 — Reach the testing world ✅
- Found Singleplayer button via `find_dom_elements {"label":"Singleplayer"}` — note brief uses `labelContains` but actual filter shape is `label` (already substring-matches)
- Selected the only world entry (no name field exposed on WorldListEntry — see Bugs)
- Clicked Play Selected World, polled `get_player_state`, in-world within ~5s (Soulreaver diamond sword in slot 0, creative)

### Phase 2 — Open item-editor ⚠️
- All `send_command` variants returned `Command sent.` but opened nothing and apparently silently failed (also `/time set day` had no observable effect even when sent via chat ENTER — see Bugs)
- Pause menu had no item-editor entry (no ModMenu integration)
- Tab-complete via chat suggestion popup not exposed in DOM (see Bugs)
- Found via KeyBindsScreen: category `key.category.itemeditor.controls` with keybind `I`
- Press `I` → owo `ItemEditorScreen` opened correctly (168 elements; screen hints + keyboardShortcuts metadata exposed, very nice)

### Phase 3 — Item-editor flows
- **3.1 Set item name (`RichTextAreaComponent` o-69)**: ⚠️ click + type_dom_text worked, BUT `clear_first: true` did NOT clear; result was `"SoulreaverTest Blade"` (appended). Bug.
- **3.2 Toggle Unbreakable (regular checkbox o-94)**: ✅ `set_dom_value` with `{"value": false}` flipped `checked`, preview updated.
- **3.2b Tiny checkbox (o-118, width 16)**: ✅ toggle worked — adapter dispatch handled it.
- **3.3 Stack count (`TextBoxComponent` o-74)**: ✅ `set_dom_text "16"` applied; validation panel correctly updated to "Stack Count must be between 1 and 1."; Save button disabled. DOM correctly surfaced both the new text AND the validation message — great.
- **3.4 Open dropdown / 3.5 Add enchantment**: ⏭ partially probed — Enchants tab already had Sharpness 10 / Fire Aspect 3 on item; dropdown overlay test deferred.
- **3.6 Tab switch (General → Enchants)**: ✅ clicked `Enchants` button, element count went 168→95, IDs renumbered, snapshot rebuilt cleanly as hint warned.
- **3.7 Apply / Give**: ⏭ skipped (Save disabled by intentional validation error — restoring stack count and re-saving would work but cost context budget)

### Phase 4 — Negative probes ✅
- **4.1 set_dom_value on label**: ✅ lean error `element_does_not_support_set_value`. Response = error envelope + targetElement shallow row + snapshotId hint; no full DOM dump.
- **4.2 set_dom_text on label**: ✅ lean error `element_does_not_accept_text`. Same compact envelope.
- **4.3 click stale/bad element_id (`o-9999`)**: ✅ lean `element_not_found`. No DOM dump.
- **4.4 find_dom_elements `{"role":"container_slot"}` on item-editor**: ✅ `totalMatches:0`, filter respected, no unfiltered dump.
- **4.5 find_dom_elements `{"role":"checkbox"}` + `fields` whitelist**: ✅ returned only the one checkbox on Enchants tab; field whitelist honored.
- **4.6 release_session**: ⏭ skipped — `singleSessionMode` is false, so semantics not testable.

### Phase 5 — Free-form ⏭
- Creative inventory enumerated 50+ slots with role=container_slot, itemId, itemName, itemCount — works.
- KeyBindsScreen fully introspected (205 elements) — snapshot builder handles deeply nested vanilla lists fine.

## Bugs found

### blocker — none

### major
1. **`send_command` is non-functional / silent no-op**. Multiple commands (`/edit-item`, `/itemeditor`, `/time set day`, `/edit`) all returned `"Command sent."` but had zero observable effect — neither opening item-editor (which `I` key opens fine) nor changing world state. Reproduction: `send_command {"command":"/time set day"}` then `get_world_state` — `time` continues advancing rather than jumping to 1000. Tested both with and without leading slash. **Hypothesis**: the command is being put into chat queue as a literal message (not parsed as command), OR is being parsed but not sent to integrated server.
2. **`clear_first: true` not honored on `RichTextAreaComponent`** (`type_dom_text`). Setting name to "Test Blade" with `clear_first:true` produced "SoulreaverTest Blade" — original text was not cleared first. Reproduction: open item-editor on item with existing name, `type_dom_text o-69 "Test Blade" clear_first:true`. **Hypothesis**: clear path uses an EditBox-style clear that the RichTextArea adapter doesn't implement; should issue Ctrl+A + Delete (or equivalent) before typing.

### minor
3. **`singleSessionMode: false`** in `get_harness_status` despite brief saying "Single-session mode is on" by default. Either the brief is stale or the default config flipped.
4. **Tiny owo checkboxes (o-118, o-121 / width=16)** have **NO `label` field** in the DOM, only a sibling `ScaledLabelComponent` with the actual label text ("Enable glint override", "Force glint when overridden"). Brief explicitly flags this as a regression: "If you see a checkbox with no `label` field, that is a regression". Other owo checkboxes (e.g. o-94 "Unbreakable", o-68 "Render heads/sprites...", o-36 "Allow unsafe enchantmen...") DO have label fields. The toggle itself works; only the label-derivation logic is broken for these label-less owo checkboxes.
5. **Vanilla `WorldListEntry` exposes no world name**. The DOM row shows `class`, `x`, `y`, `selected`, `role:list_entry` — but no `name` / `label` / `title` field. Means an agent can't pick a specific world by name when multiple are present. Reproduction: SelectWorldScreen → `get_dom_element v-4`.
6. **Vanilla command-suggestion popup not exposed in DOM**. After opening `ChatScreen` and typing `/`, `get_screen_dom` returns only the single text input — the `CommandSuggestions` floating list is not enumerated. Means an agent can't discover available commands via tab-complete.
7. **`KeyBindsScreen` keybind category labels are raw translation keys** (`key.category.itemeditor.controls`) rather than translated text. Vanilla resolves these to localized strings when rendering; the DOM should run the same `I18n.get(...)` resolution. Workaround for now: agents must substring-match the raw key.
8. **ESCAPE on `ChatScreen` did not close it** (vanilla MC normally does). `press_screen_key ESCAPE` returned `success:true, screenChanged:false` while still on ChatScreen. `navigate_back` worked as fallback. **Hypothesis**: ESCAPE on the chat screen goes through some override that the harness's input dispatch path bypasses.

### nit
9. **Every successful interaction returns the full DOM tree** (often 150+ elements / 10k+ tokens). Errors are lean; success is huge. For a chain of 10 set_value / set_text / click calls on item-editor, this dumps ~150 KB of mostly-identical JSON into context. Consider returning only the changed element + snapshotId + screenSignature on success, and let the agent opt into a full refresh via `get_screen_dom`.

## UX rough edges
- Filter docs in brief say `labelContains` / `roleEquals` style, actual schema is just `{label, text, role, actions, type, componentId}` doing substring matches. Tools-facing schema descriptions say `Filter object. Keys: label, text, role, actions, type, componentId.` — clarify substring semantics.
- `set_dom_value` requires `{"value": ...}` wrapped object even for booleans. A scalar boolean should also be accepted; right now `value: true` fails schema validation and you must write `value: {"value": true}`.
- Confirmation overlays (Item Editor Cancel button) likely produce a modal — would be nice if the standard `confirmModal` / `overlay` flag from the brief surfaced here when one appears.
- When `send_command` silently fails, there's no feedback channel. Even a `commandConsumed: false` boolean in the response would help. Currently the agent has no way to tell whether a command was accepted, rejected, or queued.
- No `get_chat_log` / `get_recent_chat_messages` tool exists. Combined with bug #1, an agent has no way to diagnose why a command didn't work (would otherwise read the red error toast in chat).

## Suggestions for v0.1
1. **Fix `send_command`** — verify it's routed through `ClientCommandManager.executeCommand(...)` (or equivalent) and not just `player.sendChatMessage`. Add a return field indicating dispatch success. This is the highest-impact fix.
2. **Lean successful responses** — return only `{interaction, snapshotId, screenSignature, targetElement}` on success. Agents already call `get_screen_dom*` when they need a refresh. Cuts ~95% of context usage in interaction-heavy flows.
3. **Add `get_chat_history`** — exposes the last N chat lines (system messages, command errors, player messages). Critical for diagnosing command failures.
4. **Fix `clear_first` on `RichTextAreaComponent`** — dispatch Ctrl+A then Delete (or use the component's setText API directly) before typing.
5. **Surface labels for label-less owo checkboxes** — when an owo `CheckboxComponent` has no inline label, walk the parent layout for an adjacent sibling `ScaledLabelComponent`/Label and adopt its text into the `label` field. This is the same pattern noted in the brief for tiny checkboxes.
6. **Add `name`/`label` to `WorldListEntry`** — read `WorldListEntry#getDisplayName()` (or the underlying `LevelSummary`) and surface it on the DOM row.
7. **Expose `CommandSuggestions` popup** — when ChatScreen has an active `CommandSuggestions` widget, enumerate its suggestions as DOM children of the chat input (or as an overlay).
8. **Resolve translation keys in DOM labels** — pipe label text through `I18n.get(...)` before serializing.
9. **Make ESCAPE on `ChatScreen` close it** — investigate the key-dispatch path for ChatScreen; vanilla `keyPressed(256)` should close.
10. **Accept scalar values in `set_dom_value`** (`value: true` not `value: {"value": true}`) — current wrapper is a footgun.

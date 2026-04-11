# Meteor Test Harness - Session Handoff

## Project Overview

Meteor addon that embeds a local MCP HTTP server inside Minecraft (embedded Tomcat servlet). LLM agents connect via MCP Streamable HTTP transport to interact with the game — click UI elements, read screen DOM, manage modules, send commands, etc.

- **Minecraft**: 1.21.11, Yarn mappings (1.21.11+build.3)
- **Fabric**: Loader 0.18.2, Loom 1.14-SNAPSHOT
- **Meteor Client**: 1.21.11-SNAPSHOT
- **MCP SDK**: Java 1.1.1 (`mcp-core` + `mcp-json-jackson2`)
- **MCP Client**: pi (TypeScript), uses `@modelcontextprotocol/sdk` v1.27.1
- **Embedded Tomcat**: 11.0.13

## Working Directory

`C:\Users\coper\Documents\GitHub\1meteor-addons-etc\meteor-test-harness`

## Current State

The harness is functional end-to-end:
- MCP connection establishes cleanly (pi probe bypass was implemented)
- Tool calls execute and return data (fixed structuredContent serialization issue)
- Basic DOM clicking works (Singleplayer button on title screen works)
- SDK upgraded from 0.14.1 to 1.1.1 (fixed protocol version mismatch)

## Current Problem: List Entry Clicking

### Symptom

On the Select World screen (`SelectWorldScreen` / `class_442`), clicking a world list entry via `click_dom_element` returns success but does NOT select the world. The "Play Selected World" button stays `active: false`.

### DOM Data

```
World list container (v-3): type=class_528, x=0, y=49, width=427, height=131
  World entry (v-4): type=class_4272, x=78, y=51, width=270, height=36
Play Selected World (v-5): active=false (should become true after selection)
```

### Root Cause (confirmed via bytecode decompilation)

**The direct Element click path short-circuits the parent's event routing.**

The `click()` method currently has this order:
1. **Element direct-click** (NEW, runs first) — calls `element.mouseClicked(click, false)` directly
2. **Coordinate-based screen click** (original) — calls `screen.mouseClicked(click, false)` which routes through the widget hierarchy

For v-4 (a WorldListWidget.WorldEntry), path #1 fires first. But `AlwaysSelectedEntryListWidget.Entry.mouseClicked` is decompiled to:
```
0: iconst_1
1: ireturn
```
That's literally `return true;` — it does NOTHING. Selection doesn't happen in the entry. Selection happens in the **parent** `EntryListWidget` during its `mouseClicked` dispatch chain.

**The full selection flow requires routing through the screen → list widget → entry:**
1. `screen.mouseClicked(click, false)` → finds WorldListWidget at the click coordinates
2. `WorldListWidget` (extends `ContainerWidget` → `ParentElement`) calls `hoveredElement(x, y)` 
3. `EntryListWidget.hoveredElement()` calls `getEntryAtPosition(x, y)`
4. `getEntryAtPosition()` iterates `children()`, calls `entry.isMouseOver(x, y)` for each
5. The matching entry is found → `EntryListWidget.setSelected(entry)` is called (THIS is what selects)
6. Then `entry.mouseClicked(click, false)` is called (which just returns true)

So calling `entry.mouseClicked()` directly skips steps 1-5 and never triggers `setSelected()`.

### The Fix Needed

Reorder the click paths so coordinate-based screen click runs FIRST, and Element direct-click is a fallback:

```java
public synchronized boolean click(String id) {
    ElementRef ref = resolveRef(id);
    if (ref == null || mc.currentScreen == null) return false;

    // 1. Coordinate-based screen click (proper event routing for list entries)
    if (ref.clickableCoordinates()) {
        double clickX = ref.centerX();
        double clickY = ref.centerY();
        if (invokeScreenClick(mc.currentScreen, clickX, clickY, 0)) {
            return true;
        }
    }

    // 2. Direct Element click (fallback for elements where screen routing fails)
    if (ref.backing instanceof Element element) {
        try {
            Click click = new Click(ref.centerX(), ref.centerY(), new MouseInput(0, 0));
            element.mouseClicked(click, false);
            return true;
        } catch (Exception ignored) {}
    }

    // 3. Meteor widget action
    if (ref.backing instanceof WPressable pressable && pressable.action != null) {
        pressable.action.run();
        return true;
    }

    return false;
}
```

**IMPORTANT**: This reordering fix may not be sufficient on its own. The coordinates from the `Widget` fallback on list entries (x=78, y=51 for v-4) need to be verified as screen-relative. If they're parent-relative or don't account for scroll offset, the screen-level click will still miss. Investigate whether `EntryListWidget.Entry.getX()/getY()` returns screen-relative coordinates or if they need transformation.

### Pending Enhancement: Generic Labels for List Entries

A clean one-liner can add labels to all list entries. `AlwaysSelectedEntryListWidget.Entry<E>` has `getNarration()` → `Text` that all selectable list entries implement:

- `WorldEntry.getNarration()` → world name
- `ServerEntry.getNarration()` → server name  
- `LanguageEntry.getNarration()` → language name
- etc.

Proposed addition to `mapVanillaElement()`:
```java
if (element instanceof AlwaysSelectedEntryListWidget.Entry<?> entry) {
    mapped.putIfAbsent("label", entry.getNarration().getString());
}
```

Import needed: `net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget`

No reflection, no string lookups, fully typed. `AlwaysSelectedEntryListWidget.Entry` is the base class for all selectable list entries in Minecraft.

For `ElementListWidget.Entry` (settings/options screens), no extra work needed — those entries contain `ClickableWidget` children that already get labels through the existing parent/child recursion.

## Key Source Files

### `src/main/java/com/mcdxai/meteortestharness/services/ScreenDomService.java`
The DOM engine. Builds element trees for both vanilla and Meteor screens. Handles click, setText, setValue, navigateBack.

- `snapshot()` — builds DOM tree, clears refs, returns payload
- `buildVanillaDom(Screen)` — iterates `screen.children()`, maps each element
- `mapVanillaElement(Element)` — extracts id, type, label, coordinates for each element
  - `ClickableWidget` → full data (label, visible, active, x/y/w/h)
  - `Widget` (NEW fallback) → x/y/w/h for non-ClickableWidget elements (list entries)
  - `TextFieldWidget` → text, editable
  - `ParentElement` → recurses into children
- `click(String id)` — resolves ref, dispatches click
  - Path 1: Element direct-click (NEW, currently FIRST — needs reordering)
  - Path 2: Coordinate-based `invokeScreenClick` (currently SECOND — should be FIRST)
  - Path 3: Meteor `WPressable.action.run()`
- `invokeScreenClick(Screen, x, y, button)` — creates `Click` + `MouseInput`, calls `screen.mouseClicked` + `screen.mouseReleased`
- `ElementRef` — stores id, backing object, x/y/width/height (NaN until populated)
- `resolveRef(String id)` — looks up ref, takes fresh snapshot if not found

### `src/main/java/com/mcdxai/meteortestharness/util/McpResults.java`
Tool result construction. Recently fixed to serialize structuredContent as JSON text content (was only putting "ok" as text, actual data only in structuredContent which pi ignores).

### `src/main/java/com/mcdxai/meteortestharness/mcp/HarnessMcpRegistry.java`
Registers all 36 MCP tools and their handlers. Each tool uses `McpResults.ok(...)` or `McpResults.error(...)`.

### `src/main/java/com/mcdxai/meteortestharness/mcp/McpHarnessServer.java`
Server bootstrap. Creates `HttpServletStreamableServerTransportProvider`, `McpServer.sync()`, embedded Tomcat.

### `src/main/java/com/mcdxai/meteortestharness/mcp/SessionGate.java`
Single-session ownership lock. `claimOrValidate(sessionId, singleSessionMode)`.

### `src/main/java/com/mcdxai/meteortestharness/util/MainThreadInvoker.java`
Dispatches tool calls to Minecraft's render thread via `CompletableFuture`.

## Relevant Minecraft Class Hierarchy (Yarn names)

```
WorldListWidget (class_528)
  extends AlwaysSelectedEntryListWidget<WorldListWidget.Entry>
    extends EntryListWidget<E>
      extends ContainerWidget
        extends ScrollableWidget
          extends ClickableWidget  ← is ClickableWidget, gets coordinates
      implements ParentElement

WorldListWidget.WorldEntry (class_4272)
  extends WorldListWidget.Entry
    extends AlwaysSelectedEntryListWidget.Entry<E>
      extends EntryListWidget.Entry<E>
        implements Element, Widget  ← NOT ClickableWidget
      implements Narratable
        abstract Text getNarration()  ← world name, server name, etc.

EntryListWidget.Entry implements Widget:
  getX(), getY(), getWidth(), getHeight()  ← position set during rendering
  isMouseOver(double, double)  ← used by getEntryAtPosition
  mouseClicked(Click, boolean)  ← returns true, does nothing (selection is in parent)

AlwaysSelectedEntryListWidget.Entry.mouseClicked bytecode:
  0: iconst_1
  1: ireturn
  (literally just `return true;`)

Selection flow:
  EntryListWidget.hoveredElement(x, y)
    → getEntryAtPosition(x, y)
    → iterates children, calls entry.isMouseOver(x, y)
    → returns matching entry
  Then ContainerWidget.mouseClicked routes to hovered element
  EntryListWidget internally calls setSelected(entry)
```

## Other Relevant List Widgets (same pattern)

All extend `AlwaysSelectedEntryListWidget` → same `Entry` base class with `getNarration()`:

- `MultiplayerServerListWidget.ServerEntry` — server name
- `LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry` — language name  
- `StatsScreen.GeneralStatsListWidget.Entry` — stat name

Settings screens use `ElementListWidget.Entry` (different base, contains ClickableWidget children already handled by recursion).

## Fabric Runtime Note

At runtime, Minecraft classes use intermediary names (e.g., `class_442` not `TitleScreen`). String-based reflection (`getMethod("mouseClicked", ...)`) BREAKS because string literals are not remapped. Only direct typed method calls work — the compiler handles remapping. This is why `invokeScreenClick` uses `screen.mouseClicked(click, false)` directly, not reflection.

## Action Items (in order)

1. **Fix click path ordering** — move coordinate-based screen click before Element direct-click. Verify Widget coordinates are screen-relative for list entries.
2. **Add generic label extraction** — `AlwaysSelectedEntryListWidget.Entry<?>` → `getNarration().getString()`
3. **Test end-to-end** — Select World screen: click entry → verify "Play Selected World" becomes active → click play → verify world loads
4. **Test other list screens** — multiplayer server list, language selection, etc.

## Constraints

- No reflection with string-based method names (Fabric intermediary remapping breaks them)
- No version-specific code — target Minecraft 1.21.11 but keep patterns generic
- Minimal code — only change what's necessary
- The user prefers fully generic solutions over targeted per-screen helpers

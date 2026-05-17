# Universal MCP GUI Test Harness — Research & Design Document

**Date**: 2025-05-13  
**Status**: Research complete, design decisions finalized, implementation pending

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current Coupling Analysis](#2-current-coupling-analysis)
3. [item-editor Mod Analysis](#3-item-editor-mod-analysis)
4. [owo-lib UI Framework Internals](#4-owo-lib-ui-framework-internals)
5. [Minecraft 26.x Screen & Widget System](#5-minecraft-26x-screen--widget-system)
6. [Ecosystem Landscape](#6-ecosystem-landscape)
7. [Build Architecture Decision](#7-build-architecture-decision)
8. [Adapter System Architecture](#8-adapter-system-architecture)
9. [Repo Structure & File Layout](#9-repo-structure--file-layout)
10. [Adapter Interface Design](#10-adapter-interface-design)
11. [Engine Implementation Details](#11-engine-implementation-details)
12. [Custom Component Catalog Strategy](#12-custom-component-catalog-strategy)
13. [MCP Tool Surface](#13-mcp-tool-surface)
14. [Config System](#14-config-system)
15. [Key Design Decisions](#15-key-design-decisions)
16. [Implementation Order](#16-implementation-order)
17. [Open Questions / Future Research](#17-open-questions--future-research)

---

## 1. Executive Summary

This project embeds an MCP (Model Context Protocol) HTTP server inside a Minecraft Fabric mod, enabling LLM agents to automate GUI/screen testing. The existing implementation is tightly coupled to Meteor Client. The goal is to extend support to other Fabric mods, starting with the item-editor mod.

**Key findings:**
- **No other tool provides programmatic GUI testing for Minecraft mods.** Fabric's Client GameTest API is screenshot-only. Our DOM engine is novel.
- **item-editor uses owo-lib** (a third UI framework), not vanilla MC widgets. This necessitates a pluggable adapter system.
- **A "fully universal" solution is not possible** because mods can use arbitrary UI frameworks. But a pluggable adapter system covers the practical cases: vanilla MC (automatic), owo-lib (one adapter), Meteor Client (one adapter).
- **Recommended approach**: Separate Fabric mod in the same repo (Gradle multi-project). No refactoring of the existing Meteor addon.

---

## 2. Current Coupling Analysis

### Tier 1 — Fully Generic (~15 files, reusable as-is)
`DomQueryEngine`, `DomSnapshot`, `DomValueUtils`, `DomKeyCodec`, `DomEntryListHelper`, `DomToolHelper`, `ArgReader`, `McpResults`, `ToolSchemas`, `SessionGate`, `EmbeddedWebappClassLoader`, `McpServer` (minus EVENT_BUS), `McpRegistry`, `RegistryContext`, `KeyboardInvoker`

### Tier 2 — Mildly Coupled (~8 files, needs `Minecraft.getInstance()` swap)
`MainThreadInvoker`, `GameStateService`, `HarnessService`, `WorldActionTools`, `WorldStateTools`, vanilla DOM paths in `DomSnapshotBuilder`/`DomInteractor`/`DomActionHints`, `DomQueryTools`, `DomInteractionTools`, `DomInputTools`

### Tier 3 — Deeply Meteor-Coupled (~12 files, needs replacement/guarding)
`McTestHarnessAddon`, `HarnessConfig`, `HarnessTab`, `ModuleService`, `SettingValueCodec`, `MeteorInfoService`, `PathingService`, `ChatLogService`, `MeteorModuleMetadata`, Meteor engine in `DomSnapshotBuilder`, Meteor widget handling in `DomInteractor`, `MeteorInfoTools`, `ModuleTools`, `PathingTools`

### Coupling by Area

| Area | Meteor-Specific Files | Generic Files |
|------|----------------------|---------------|
| Entry point | `McTestHarnessAddon`, `HarnessConfig`, `HarnessTab` | `HarnessRuntime` (mostly) |
| MCP infra | `McpServer` (EVENT_BUS only) | `McpRegistry`, `RegistryContext`, `SessionGate`, `ToolSchemas`, `EmbeddedWebappClassLoader` |
| Thread dispatch | `MainThreadInvoker` (mc accessor) | Pattern is generic |
| DOM engine | `DomSnapshotBuilder` (Meteor engine), `DomInteractor` (Meteor widgets), `DomActionHints` (Meteor methods), `MeteorModuleMetadata` | `DomQueryEngine`, `DomSnapshot`, `DomValueUtils`, `DomKeyCodec`, `DomEntryListHelper`, `ElementRef` |
| Services | `ModuleService`, `ChatLogService`, `PathingService`, `SettingValueCodec`, `MeteorInfoService` | `NameMappingService`, `GameStateService` (mc accessor only) |
| MCP tools | `ModuleTools`, `MeteorInfoTools`, `PathingTools` | `CoreTools`, `WorldStateTools`, `WorldActionTools`, all DOM tools |
| Utilities | — | `McpResults`, `ArgReader` |

---

## 3. item-editor Mod Analysis

### Overview
- **Type**: Client-side Fabric mod for in-game item data editing
- **MC Version**: 26.1, Fabric Loader 0.18.4, Java 25
- **Critical Dependency**: `owo-lib 0.13.0+26.1` (hard dependency)
- **No mixins** — uses only standard Fabric/owo APIs

### Screen Inventory (7 screens)

| Screen | Base Class | Engine Needed | What It Does |
|--------|-----------|---------------|--------------|
| `ItemEditorScreen` | `BaseOwoScreen<StackLayout>` | **owo** | Main editor: category tabs, editor panels, item preview, validation |
| `ItemEntryScreen` | `BaseOwoScreen<StackLayout>` | **owo** | Entry point for empty hand: Create/Storage/Import/Cancel |
| `ItemPickerScreen` | `BaseOwoScreen<StackLayout>` | **owo** | Item grid browser with search and modded toggle |
| `ImportScreen` | `BaseOwoScreen<StackLayout>` | **owo** | Import from paste or file |
| `RawImportScreen` | `BaseOwoScreen<StackLayout>` | **owo** | Raw SNBT/JSON text import |
| `ImportedItemsScreen` | `ContainerScreen` | **vanilla** | Chest-like multi-item import viewer |
| `StorageScreen` | `ContainerScreen` + owo side panel | **hybrid** | Persistent item storage with search/sort |

### Custom Components (item-editor specific)
- `VirtualItemGridComponent` — virtualized item grid with custom scroll/click/hover
- `RichTextAreaComponent` — rich text editor with formatting toolbar
- `RawTextAreaComponent` — raw SNBT text editor
- `SearchablePickerDialog` — modal search-and-pick overlay
- `UnifiedColorPickerDialog` — color picker (HSB/hex/presets)
- `ConfirmationDialog` — yes/no modal
- `RawItemDataDialog` — view raw data with diff/copy/export
- `RichTextTokenDialog` — insert head/sprite/event tokens
- `OrbitingArmorStandComponent` — 3D armor stand preview
- `RotatableItemPreviewComponent` — 3D item preview

### Screen Navigation Tree
```
I keybind
├─ Holding item → ItemEditorScreen
│   ├── Category tabs → switch panels
│   ├── Apply/Reset buttons → ConfirmationDialog
│   ├── Ctrl+S/Ctrl+R/Ctrl+Tab
│   └── Dialogs (overlays on StackLayout):
│       ├── SearchablePickerDialog
│       ├── UnifiedColorPickerDialog
│       ├── RichTextTokenDialog
│       ├── RawItemDataDialog
│       └── ConfirmationDialog
└─ Empty hand → ItemEntryScreen
    ├── Create Item → ItemPickerScreen → ItemEditorScreen
    ├── Storage → StorageScreen (PICK_FOR_EDIT) → ItemEditorScreen
    └── Import → ImportScreen
        ├── Paste → RawImportScreen → ItemEditorScreen
        └── File → ItemEditorScreen or ImportedItemsScreen → ItemEditorScreen
/storage command → StorageScreen (MANAGE mode)
```

### Key DOM Challenges
1. **5/7 screens use owo** — vanilla engine insufficient
2. **Dialogs are overlays, not screens** — FlowLayouts on StackLayout root
3. **Custom components** — need per-type metadata extraction
4. **Hybrid screen** (StorageScreen) — vanilla chest grid + owo side panel

---

## 4. owo-lib UI Framework Internals

### Component Type Hierarchy

```
PositionedRectangle (interface: x, y, width, height)
  └── Component (interface: positioning, sizing, id, events, focus, lifecycle)
        └── ParentComponent (interface: children(), layout(), childAt(), childById())
              ├── FlowLayout (sequential: HORIZONTAL, VERTICAL, LTR_TEXT)
              ├── GridLayout
              ├── StackLayout (layers children on top of each other)
              ├── ScrollContainer<C> (wraps single child with scrolling)
              ├── DraggableContainer<C>
              ├── OverlayContainer<C>
              └── CollapsibleContainer

BaseComponent (abstract) implements Component
  ├── stores: parent, id, x, y, width, height, mounted
  ├── EventStream fields for all events
  └── inflate(), mount(), update(), draw()

BaseParentComponent (abstract) extends BaseComponent implements ParentComponent
  ├── FocusHandler (only on root component)
  ├── taskQueue (only on root)
  └── children(), layout(), drawChildren()
```

### Standard Components

| Component | Extends | Key Properties |
|-----------|---------|---------------|
| `ButtonComponent` | vanilla `ButtonWidget` | `message(Text)`, `active(boolean)`, `onPress()` |
| `LabelComponent` | `BaseComponent` | `text(Text)`, `color(Color)`, `shadow(boolean)` |
| `TextBoxComponent` | vanilla `TextFieldWidget` | `text(String)`, `onChanged()`, `setMaxLength()` |
| `TextAreaComponent` | `TextBoxComponent` | Multi-line, `maxLines()`, `displayCharCount()` |
| `CheckboxComponent` | vanilla `CheckboxWidget` | `checked(boolean)`, `isChecked()` |
| `SliderComponent` | vanilla `SliderWidget` | `value(double)` 0-1, `onChanged()` |
| `DiscreteSliderComponent` | `SliderComponent` | min/max range, discrete values |
| `ItemComponent` | `BaseComponent` | `stack(ItemStack)` |
| `EntityComponent` | `BaseComponent` | `allowMouseRotation()`, `scale()` |
| `DropdownComponent` | `BaseComponent` | Context menu entries |
| `TextureComponent` | `BaseComponent` | Texture region rendering |
| `BoxComponent` | `BaseComponent` | Colored rectangle |

### BaseOwoScreen

- Has `protected OwoUIAdapter<R> uiAdapter` field
- Required abstracts: `createAdapter()`, `build(R rootComponent)`
- Access root: `uiAdapter.rootComponent`
- The OwoUIAdapter implements vanilla `Element`/`Drawable`/`Selectable` — Minecraft's standard dispatch reaches it

### Coordinate System
- All coordinates in **scaled pixel space** (same as Minecraft GUI coordinates)
- `component.x()`, `component.y()`, `width()`, `height()` — screen-relative
- `childAt(screenX, screenY)` — recursive hit-testing with screen coordinates
- No coordinate conversion needed between owo and MC screen space

### Event Dispatch
1. Minecraft calls `screen.mouseClicked(x, y, button)`
2. Screen dispatches to children → finds `OwoUIAdapter` (registered as `Element`)
3. Adapter calls `rootComponent.onMouseDown(x, y, button)`
4. `ParentComponent.onMouseDown` reverse-iterates children for hit-testing
5. Dispatches to deepest matching child

**Key insight**: `screen.mouseClicked(x, y, button)` DOES reach owo components. Click dispatch works through the standard Minecraft pipeline.

### Tree Traversal APIs
- `ParentComponent.children()` → `List<Component>` (direct children)
- `childAt(int x, int y)` → deepest descendant at coordinates
- `collectDescendants(ArrayList)` → flat list of all descendants
- `childById(Class, String)` → depth-first search by ID
- `forEachDescendant(Consumer)` → walks entire tree

---

## 5. Minecraft 26.x Screen & Widget System

### Screen Class
```java
Screen extends AbstractContainerEventHandler implements Renderable
  title: Component
  minecraft: Minecraft
  width, height: int (scaled coordinates)
  children(): List<GuiEventListener>  // THE primary discovery mechanism
  init(), rebuildWidgets(), repositionElements()
  mouseClicked(), keyPressed(), onClose()
  addRenderableWidget(), addWidget(), removeWidget()
```

### Widget Type Hierarchy
```
GuiEventListener (interface)
  ContainerEventHandler (interface) ← GuiEventListener
    AbstractContainerEventHandler (abstract)
      Screen (abstract)
      AbstractContainerWidget (abstract) ← AbstractScrollArea
        AbstractSelectionList<E extends Entry<E>>
  
AbstractWidget (abstract) ← LayoutElement, Renderable, GuiEventListener, NarratableEntry
  message: Component, active, visible, x, y, width, height
  Button, Checkbox, CycleButton, EditBox, AbstractSliderButton, StringWidget, ImageWidget
```

### Discovery Pattern
- `screen.children()` returns ALL interactive `GuiEventListener` instances
- Must recurse `ContainerEventHandler.children()` for nested containers
- `AbstractSelectionList` entries are `LayoutElement + GuiEventListener` (not AbstractWidget)
- `screen.mouseClicked(x, y, button)` handles proper widget dispatch

### Text Extraction
| Widget | Method | Returns |
|--------|--------|---------|
| `AbstractWidget` | `getMessage()` | `Component` → `.getString()` |
| `EditBox` | `getValue()` | `String` (raw text) |
| `Checkbox` | `selected()` | `boolean` |
| `AbstractSliderButton` | `value` field | `double` (0.0-1.0) |
| `CycleButton` | `getValue()` | `T` |

---

## 6. Ecosystem Landscape

### Existing GUI Testing Tools

| Tool | GUI Testing | Notes |
|------|------------|-------|
| Fabric Client GameTest API | Screenshot-only | Can launch client + take screenshots, no widget interaction |
| MC GameTest Framework | None | Server-side only, block/entity testing |
| Mineflayer MCP Server | None | External bot via multiplayer protocol |
| Minecraft Access (accessibility) | Narration output | Screen structure → screen readers, not queryable |
| **Our DOM Engine + MCP** | **Full programmatic** | **Only tool with widget introspection + interaction** |

### The Playwright MCP Analogy
Our project is architecturally equivalent to Microsoft's Playwright MCP — accessibility-snapshot-based browser automation via MCP. We provide the same capability for Minecraft screens.

### Resolution/GUI Scale Testing
No automated tooling exists for testing GUIs across different resolutions/scales. Our harness could be extended to support this (change GUI scale, re-snapshot, validate layout) — another novel capability.

---

## 7. Build Architecture Decision

### Decision: Same repo, Gradle multi-project, no shared library

```
meteor-test-harness/                          (existing repo root)
├── settings.gradle.kts                       (include("meteor-addon"), include("universal"))
├── gradle/libs.versions.toml                 (shared version catalog)
│
├── meteor-addon/                             (existing code, MOVED from root)
│   ├── build.gradle.kts
│   └── src/main/...
│
└── universal/                                (NEW standalone Fabric mod)
    ├── build.gradle.kts
    └── src/main/...
```

### Why not single JAR with conditional Meteor loading?
- Cleaner maintenance — no Meteor decoupling/refactoring needed
- No conditional class-loading complexity
- Separate release cycles possible
- The user explicitly prefers this approach

### Why not separate repo?
- Shared version catalog (MC version, Fabric version, dependencies)
- One place to manage
- The user originally said "housed in the same repo"

### Why no common/ subproject?
- Deliberate duplication for independence
- The shared code is small (~10-15 files: MCP infra, DOM query engine, utils)
- Avoids coupling points and version coordination issues

### Migration Path
1. Move root `build.gradle.kts` + `settings.gradle.kts` + `src/` + `gradle/` → `meteor-addon/`
2. Create new root `settings.gradle.kts` with `include("meteor-addon", "universal")`
3. Create `universal/` with its own build and source
4. Shared `gradle/libs.versions.toml` stays at root

---

## 8. Adapter System Architecture

### Seven Layers

| Layer | Responsibility | Varies by Framework? |
|-------|---------------|---------------------|
| 1. Screen Detection | "What kind of screen is this?" | Yes — instanceof checks |
| 2. Tree Traversal | Walk the widget/component tree | Yes — different APIs per framework |
| 3. Element Identification | "What is this element?" | Yes — different type hierarchies |
| 4. Metadata Extraction | Text, value, state, bounds | Yes — different getter methods |
| 5. Action Hints | Click, type, scroll, drag | Partially — common actions + framework-specific |
| 6. Interaction | Click/type/set dispatch | Partially — screen.mouseClicked() works for most |
| 7. Coordinate Mapping | Element position on screen | Mostly no — all in scaled pixels |

### Architecture Diagram
```
AdapterRegistry (central)
  ├── ScreenEngine (picks the right engine for the current screen)
  │     ├── VanillaScreenEngine    ← existing buildVanillaDom() adapted
  │     ├── OwoScreenEngine        ← NEW: traverses owo UIComponent tree
  │     └── [Future engines...]
  │
  ├── WidgetAdapter (per-widget-type metadata + interaction)
  │     ├── Vanilla adapters (AbstractWidget, EditBox, Checkbox, etc.)
  │     ├── owo adapters (ButtonComponent, TextBoxComponent, LabelComponent, etc.)
  │     └── Custom adapters (VirtualItemGridComponent, RichTextAreaComponent, etc.)
  │
  └── ScreenDescriptor (screen-class → LLM hints)
        ├── item-editor screens → click/type/shortcut hints
        └── Future mod registrations
```

---

## 9. Repo Structure & File Layout

```
universal/
├── build.gradle.kts
├── gradle.properties
└── src/main/
    ├── java/io/mcdxai/harness/universal/
    │   ├── UniversalHarnessMod.java          — ClientModInitializer entry point
    │   ├── HarnessRuntime.java               — MCP server lifecycle
    │   ├── config/
    │   │   └── HarnessConfig.java             — JSON-backed config
    │   │
    │   ├── adapter/                           — THE ADAPTER SYSTEM
    │   │   ├── AdapterRegistry.java           — central registration
    │   │   ├── ScreenEngine.java              — interface
    │   │   ├── WidgetAdapter.java             — interface
    │   │   ├── ScreenDescriptor.java          — interface
    │   │   ├── DomBuildContext.java           — shared state during DOM build
    │   │   │
    │   │   ├── vanilla/
    │   │   │   ├── VanillaScreenEngine.java
    │   │   │   ├── VanillaWidgetAdapters.java
    │   │   │   └── VanillaEntryListHelper.java
    │   │   │
    │   │   └── owo/
    │   │       ├── OwoScreenEngine.java
    │   │       ├── OwoWidgetAdapters.java
    │   │       ├── OwoContainerAdapters.java
    │   │       └── OwoPresenceGuard.java
    │   │
    │   ├── dom/                               — Generic DOM engine
    │   │   ├── DomSnapshot.java
    │   │   ├── DomSnapshotBuilder.java
    │   │   ├── DomQueryEngine.java
    │   │   ├── DomInteractor.java
    │   │   ├── DomActionHints.java
    │   │   ├── ElementRef.java
    │   │   └── DomKeyCodec.java
    │   │
    │   ├── mcp/                               — MCP server
    │   │   ├── McpServer.java
    │   │   ├── McpRegistry.java
    │   │   ├── RegistryContext.java
    │   │   ├── SessionGate.java
    │   │   ├── ToolSchemas.java
    │   │   └── EmbeddedWebwebClassLoader.java
    │   │
    │   ├── mcp/tools/
    │   │   ├── CoreTools.java
    │   │   ├── DomQueryTools.java
    │   │   ├── DomInteractionTools.java
    │   │   ├── DomInputTools.java
    │   │   ├── DomToolHelper.java
    │   │   ├── WorldStateTools.java
    │   │   ├── WorldActionTools.java
    │   │   ├── ChatLogTools.java
    │   │   └── Resources.java
    │   │
    │   ├── services/
    │   │   ├── ScreenDomService.java
    │   │   ├── GameStateService.java
    │   │   ├── ChatLogService.java
    │   │   └── HarnessService.java
    │   │
    │   ├── mixin/
    │   │   ├── KeyboardInvoker.java
    │   │   └── ChatCaptureMixin.java
    │   │
    │   └── util/
    │       ├── MainThreadInvoker.java
    │       ├── McpResults.java
    │       └── ArgReader.java
    │
    └── resources/
        ├── fabric.mod.json
        └── universal-harness.mixins.json
```

---

## 10. Adapter Interface Design

### ScreenEngine

```java
public interface ScreenEngine {
    /** Human-readable name ("vanilla", "owo", etc.) */
    String engineName();
    
    /** Priority — higher = checked first. Vanilla=0, owo=10. */
    int priority();
    
    /** Can this engine handle the given screen? */
    boolean canHandle(Screen screen);
    
    /** Build the full DOM element tree for this screen */
    List<Map<String, Object>> buildDom(Screen screen, DomBuildContext ctx);
    
    /** Get the screen's title text */
    default String getScreenTitle(Screen screen) {
        return screen.getTitle().getString();
    }
}
```

### WidgetAdapter (unified interface with default no-ops)

```java
public interface WidgetAdapter<W> {
    /** The widget/component class this adapter handles */
    Class<W> widgetType();
    
    /** Extract metadata into the target map */
    default void extractMetadata(W widget, Map<String, Object> target) {}
    
    /** What actions can this widget currently perform? */
    default List<String> supportedActions(W widget) { return List.of(); }
    
    /** Handle a click. Return true if handled. */
    default boolean handleClick(Screen screen, W widget, double x, double y, int button) { 
        return false; 
    }
    
    /** Handle text input. Return true if handled. */
    default boolean handleSetText(Screen screen, W widget, String text) { 
        return false; 
    }
    
    /** Handle value setting. Return true if handled. */
    default boolean handleSetValue(Screen screen, W widget, Object value) { 
        return false; 
    }
}
```

**Why unified instead of split**: Adapters only override what they need. `LabelAdapter` implements `extractMetadata()` + `supportedActions()` but leaves interaction methods as no-ops. No interface explosion.

### ScreenDescriptor

```java
public interface ScreenDescriptor {
    boolean matches(Screen screen);
    List<String> hints();
    default Map<String, String> keyboardShortcuts() { return Map.of(); }
}
```

### AdapterRegistry

```java
public final class AdapterRegistry {
    private final List<ScreenEngine> screenEngines = new ArrayList<>();
    private final Map<Class<?>, WidgetAdapter<?>> widgetAdapters = new HashMap<>();
    private final List<ScreenDescriptor> screenDescriptors = new ArrayList<>();
    
    public void registerScreenEngine(ScreenEngine engine);
    public void registerWidgetAdapter(WidgetAdapter<?> adapter);
    public void registerScreenDescriptor(ScreenDescriptor descriptor);
    
    public Optional<ScreenEngine> findScreenEngine(Screen screen);
    public <W> Optional<WidgetAdapter<W>> findWidgetAdapter(Class<W> type);
    public List<ScreenDescriptor> findScreenDescriptors(Screen screen);
}
```

### Registration Pattern

```java
// In UniversalHarnessMod.onInitializeClient()
AdapterRegistry registry = new AdapterRegistry();

// Always register vanilla
registry.registerScreenEngine(new VanillaScreenEngine(registry));
VanillaWidgetAdapters.registerAll(registry);

// Conditionally register owo
if (FabricLoader.getInstance().isModLoaded("owo-lib")) {
    registry.registerScreenEngine(new OwoScreenEngine(registry));
    OwoWidgetAdapters.registerAll(registry);
}

// Conditionally register mod-specific descriptors
if (FabricLoader.getInstance().isModLoaded("itemeditor")) {
    ItemEditorScreenDescriptors.registerAll(registry);
}
```

---

## 11. Engine Implementation Details

### Engine Selection (priority-based)

```
Screen encountered
  → AdapterRegistry.findScreenEngine(screen)
  → Iterates ScreenEngines by priority (highest first)
  → OwoScreenEngine (priority=10): screen instanceof BaseOwoScreen? → YES → use owo
  → VanillaScreenEngine (priority=0): screen instanceof Screen? → YES → use vanilla
  → First match wins
```

### VanillaScreenEngine
Walks `screen.children()` recursively through `ContainerEventHandler.children()`. Uses registered `WidgetAdapter`s for known types. Falls back to generic `AbstractWidget` extraction for unknown types.

### OwoScreenEngine

```java
public class OwoScreenEngine implements ScreenEngine {
    public List<Map<String, Object>> buildDom(Screen screen, DomBuildContext ctx) {
        BaseOwoScreen<?> owoScreen = (BaseOwoScreen<?>) screen;
        ParentComponent root = getRootComponent(owoScreen); // via reflection on uiAdapter field
        
        List<Map<String, Object>> elements = new ArrayList<>();
        walkComponentTree(root, elements, ctx, 0);
        return elements;
    }
    
    private void walkComponentTree(Component component, List<Map<String, Object>> elements, 
                                     DomBuildContext ctx, int depth) {
        Map<String, Object> el = new LinkedHashMap<>();
        el.put("id", ctx.nextId("o"));  // o-1, o-2, ...
        el.put("type", component.getClass().getSimpleName());
        el.put("x", component.x());
        el.put("y", component.y());
        el.put("width", component.width());
        el.put("height", component.height());
        
        // Try WidgetAdapter first
        Optional<WidgetAdapter> adapter = ctx.registry().findWidgetAdapter(component.getClass());
        if (adapter.isPresent()) {
            adapter.get().extractMetadata(component, el);
            el.put("actions", adapter.get().supportedActions(component));
        } else {
            el.put("actions", List.of("read"));
        }
        
        ctx.storeRef(el.get("id"), component);
        elements.add(el);
        
        // Recurse into children if ParentComponent
        if (component instanceof ParentComponent parent) {
            if (parent instanceof ScrollContainer<?> scroll) {
                // Unwrap ScrollContainer — expose inner child
                walkComponentTree(scroll.child(), elements, ctx, depth + 1);
                return;
            }
            
            List<Component> children = parent.children();
            boolean isStack = parent instanceof StackLayout;
            
            for (int i = 0; i < children.size(); i++) {
                Component child = children.get(i);
                // StackLayout: last child is topmost overlay
                if (isStack && i == children.size() - 1 && children.size() > 1) {
                    // Mark previous children as potentially occluded
                }
                walkComponentTree(child, elements, ctx, depth + 1);
            }
        }
    }
}
```

### Hybrid Screen Handling (StorageScreen)

StorageScreen extends vanilla `ContainerScreen` but has owo components for the side panel.

**Approach**: Detect if screen has BOTH vanilla children AND an owo adapter. Run both engines and merge results. However, owo's `TextBoxComponent` extends vanilla `TextFieldWidget`, so the vanilla engine's `instanceof EditBox` check may already catch it. Need to verify in practice.

---

## 12. Custom Component Catalog Strategy

### Short-term: Hard-coded adapters

Write `WidgetAdapter` implementations for known custom components:

```java
public class VirtualItemGridAdapter implements WidgetAdapter<BaseUIComponent> {
    @Override
    public Class<BaseUIComponent> widgetType() { return BaseUIComponent.class; }
    
    @Override
    public void extractMetadata(BaseUIComponent widget, Map<String, Object> target) {
        if (widget.getClass().getSimpleName().equals("VirtualItemGridComponent")) {
            target.put("role", "item_grid");
            target.put("actions", List.of("click", "scroll", "read"));
        }
    }
}
```

Gated by mod presence:
```java
if (FabricLoader.getInstance().isModLoaded("itemeditor")) {
    registry.registerWidgetAdapter(new VirtualItemGridAdapter());
}
```

### Long-term: Configuration-driven metadata

JSON file mapping class names to metadata extraction rules:

```json
{
  "mod_id": "itemeditor",
  "widget_adapters": [
    {
      "class_pattern": "*VirtualItemGridComponent",
      "role": "item_grid",
      "actions": ["click", "scroll", "read"],
      "metadata": {
        "scroll_position": { "field": "scrollAmount", "type": "double" }
      }
    }
  ]
}
```

### Ideal long-term: Adapter module JARs

Third JAR depending on both harness + target mod, registering adapters. Clean architecture but adds distribution complexity. Worth it at 5+ supported mods.

---

## 13. MCP Tool Surface

### Tools Kept (adapted from Meteor addon)

| Tool | Changes |
|------|---------|
| `get_harness_status` | Drop Meteor references |
| `get_harness_debug_info` | Drop Meteor/mapping references |
| `release_session` | No change |
| `get_player_state` | `Minecraft.getInstance()` |
| `get_world_state` | `Minecraft.getInstance()` |
| `get_player_inventory` | `Minecraft.getInstance()` |
| `get_crosshair_target` | `Minecraft.getInstance()` |
| `get_nearby_entities` | `Minecraft.getInstance()` |
| `send_chat` | `Minecraft.getInstance()` |
| `send_command` | `Minecraft.getInstance()` |
| `disconnect_world` | `Minecraft.getInstance()` |
| `get_chat_history` | Mixin-based capture |
| `clear_chat_history` | No change |
| `get_screen_dom` | Includes engine name, adapter metadata |
| `get_screen_dom_summary` | Includes engine name |
| `find_dom_elements` | No change |
| `get_dom_element` | No change |
| `get_dom_subtree` | No change |
| `click_dom_query` | WidgetAdapter-aware |
| `click_dom_element` | WidgetAdapter-aware |
| `scroll_dom_element` | No change |
| `drag_dom_element` | No change |
| `set_dom_text` | WidgetAdapter-aware |
| `set_dom_text_query` | WidgetAdapter-aware |
| `type_dom_text` | No change |
| `press_screen_key` | No change |
| `set_dom_value` | WidgetAdapter-aware |
| `navigate_back` | No change |

### Tools Dropped (Meteor-specific)

`list_modules`, `get_module`, `set_module_state`, `list_module_settings`, `get_module_setting`, `set_module_setting`, `get_meteor_info`, `list_addon_features`, `get_active_hud`, `get_pathing_status`, `pathing_move_to`, `pathing_move_in_direction`, `wait_for_pathing_action`, `pathing_pause`, `pathing_resume`, `pathing_stop`

### New Tools

| Tool | Purpose |
|------|---------|
| `list_supported_engines` | Which UI frameworks are available |
| `set_gui_scale` | Change GUI scale for resolution testing |
| `get_screen_descriptors` | LLM hints about current screen |

---

## 14. Config System

Simple JSON file in `config/universal-harness.json`. No dependency on any mod's settings system.

```java
public class HarnessConfig {
    private static final Path CONFIG_PATH = 
        FabricLoader.getInstance().getConfigDir().resolve("universal-harness.json");
    
    public String bindHost = "127.0.0.1";
    public int bindPort = 38861;
    public String mcpEndpoint = "/mcp";
    public boolean singleSession = true;
    public boolean autoStart = true;
    
    public static HarnessConfig load() { /* GSON/Jackson */ }
    public void save() { /* write to CONFIG_PATH */ }
}
```

---

## 15. Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Single JAR vs separate mods | **Separate** | Cleaner maintenance, no Meteor refactoring |
| Same repo vs separate repo | **Same repo, subprojects** | Shared version catalog |
| Common library subproject | **No** | Deliberate duplication for independence |
| WidgetAdapter granularity | **Unified interface** | Simpler, default no-ops |
| ScreenEngine selection | **Priority-based** | Most specific wins |
| owo-lib dependency | **modCompileOnly** | Not required at runtime |
| Custom component catalog | **Hard-coded → JSON config** | Practical now, extensible later |
| Element ID scheme | **Prefix per engine** (`v-`, `o-`) | Clear origin |
| Chat capture | **Mixin-based** | No Meteor event bus |
| Config system | **Simple JSON** | No framework dependency |

---

## 16. Implementation Order

1. **Set up repo structure** — migrate root → `meteor-addon/`, create `universal/` skeleton
2. **Copy + adapt foundation** — MCP server infra, DOM query engine, util classes
3. **Build VanillaScreenEngine** — port existing vanilla DOM code into adapter framework
4. **Build OwoScreenEngine** — owo component tree traversal + standard widget adapters
5. **Test against vanilla screens** — title screen, options, inventory
6. **Test against item-editor vanilla screens** — ImportedItemsScreen, StorageScreen
7. **Test against item-editor owo screens** — all 5 BaseOwoScreen instances
8. **Add custom component adapters** — VirtualItemGridComponent, RichTextAreaComponent, etc.
9. **Add screen descriptors** — item-editor screen hints for the LLM
10. **Add `set_gui_scale` tool** — automated resolution testing

---

## 17. Open Questions / Future Research

### To verify during implementation:
- Does owo's `TextBoxComponent` (extends `TextFieldWidget`) get picked up by vanilla `instanceof EditBox` checks? If yes, hybrid screens may "just work".
- Can we access `BaseOwoScreen.uiAdapter` via reflection, or do we need an accessor mixin?
- Does `ScrollContainer` expose its wrapped child via a public API or do we need reflection?

### Future research needed:
- **Other custom GUI frameworks**: Cloth Config, ModMenu, REI, JEI, and other mods that render custom UIs outside standard widget trees
- **Fabric/NeoForge/Forge cross-platform**: Is there demand for non-Fabric support?
- **Minecraft 26.x narration system**: Could we leverage the narration API as a fallback metadata source for unknown widget types?
- **Headless rendering**: Can the universal mod work in CI (Xvfb + Fabric Client GameTest)?
- **Performance**: Snapshotting complex owo screens at scale — any bottlenecks?

### Unresolved design tension:
- The "universal" mod becomes less universal with each hard-coded adapter. At what point do we invest in the adapter-module-JAR pattern?
- Should the universal mod have its own in-game config GUI? Or is JSON config sufficient?

---

## 18. Broader GUI Framework Landscape

Research into the wider Minecraft modding ecosystem to stress-test the adapter architecture against real-world GUI patterns.

### 18.1 Framework Discoverability Matrix

| Framework/Mod | Category | Rendering Model | Widget Tree? | Discoverability | Popularity |
|---|---|---|---|---|---|
| **Cloth Config** | Config GUI | Standard Screen + custom list widget | ✅ Yes | 🟢 HIGH | ⭐⭐⭐⭐⭐ (338M+ downloads) |
| **owo-lib** | General UI | Own pipeline (wraps MC via adapter) | ⚠️ Own tree | 🟡 MEDIUM | ⭐⭐⭐⭐ (67M) |
| **LDLib2** | General UI | Own pipeline (Taffy layout engine) | ⚠️ Own tree | 🟡 MEDIUM | ⭐⭐ (niche) |
| **LibGUI** | General UI | Own WWidget hierarchy | ⚠️ Own tree | 🟡 MEDIUM | ⭐⭐ (niche) |
| **REI** | Recipe viewer | Own Widget system as overlay | ⚠️ Parallel tree | 🟡 MEDIUM | ⭐⭐⭐⭐⭐ |
| **EMI** | Recipe viewer | Own Widget system as overlay | ⚠️ Own tree | 🟡 MEDIUM | ⭐⭐⭐⭐ (39M) |
| **JEI** | Recipe viewer | Own internal rendering | ❌ Not widgets | 🔴 LOW | ⭐⭐⭐⭐⭐ |
| **ModMenu** | Mod list | Standard MC Screen + widgets | ✅ Yes | 🟢 HIGH | ⭐⭐⭐⭐⭐ |
| **Sodium** | Performance | Standard MC options screen | ✅ Yes | 🟢 HIGH | ⭐⭐⭐⭐⭐ |
| **Iris** | Shaders | Standard MC Screen | ✅ Yes | 🟢 HIGH | ⭐⭐⭐⭐⭐ |
| **Create** | Content | Standard AbstractContainerScreen + some custom | ✅ Mostly | 🟢 HIGH | ⭐⭐⭐⭐⭐ |
| **FancyMenu** | Menu customization | Own element system + screen wrapping | ⚠️ Own tree | 🟡 MEDIUM | ⭐⭐⭐⭐⭐ (187M) |
| **ReplayMod / jGui** | Replay | Own jGui framework | ⚠️ Own tree | 🟡 MEDIUM | ⭐⭐⭐⭐ |
| **Meteor Client** | Hacked client | Own W-widget framework | ⚠️ Own tree | 🟡 MEDIUM | ⭐⭐⭐ (niche) |
| **Jade** | Tooltip overlay | Direct HUD rendering | ❌ None | 🔴 LOW | ⭐⭐⭐⭐ |
| **WTHIT** | Tooltip overlay | Direct HUD rendering | ❌ None | 🔴 LOW | ⭐⭐⭐ |
| **MiniHUD** | HUD overlay | Direct HUD rendering | ❌ None | 🔴 LOW | ⭐⭐⭐ |
| **WorldEditCUI** | World overlay | Direct 3D line rendering | ❌ None | 🔴 LOW | ⭐⭐⭐ |
| **Distant Horizons** | LOD rendering | Custom 3D pipeline + Cloth Config | ✅/❌ Split | 🟡 MEDIUM | ⭐⭐⭐⭐ |
| **Figura** | Model viewer | Custom 3D + some MC widgets | ⚠️ Partial | 🟡 MEDIUM | ⭐⭐⭐ |
| **Vivecraft** | VR | Replaces entire rendering | ⚠️ Reprojected | 🟡 MEDIUM | ⭐⭐ |
| **Inv. Profiles Next** | Inv. overlay | Mixin-injected buttons | ⚠️ Partial | 🟡 MEDIUM | ⭐⭐⭐ |
| **Rise/Future Client** | Hacked client | Immediate-mode custom GUI | ❌ None | 🔴 LOW | ⭐⭐ |
| **ContainerScreen mods** | Containers | Standard MC pattern | ✅ Yes | 🟢 HIGH | Various |

### 18.2 Rendering Pattern Analysis

Five distinct rendering patterns exist in the ecosystem:

| Pattern | Detectable | Introspectable | Interactive | Prevalence | Workaround |
|---------|-----------|----------------|-------------|------------|------------|
| **1. Standard MC widgets** | ✅ | ✅ | ✅ | **Very High** | N/A (already works) |
| **2. Framework widget trees** (owo, Cloth, Meteor) | ✅ | ✅ (via adapter) | ✅ | **High** | Per-framework adapter |
| **3. Parallel overlay trees** (REI, EMI) | ✅ | ⚠️ (separate tree) | ⚠️ (separate dispatch) | **High** | Hook into overlay's widget manager |
| **4. Direct DrawContext rendering** (HUDs, some screens) | ⚠️ | ❌ | ❌ | **Medium** | DrawContext interception (lossy) |
| **5. Raw OpenGL** (some hacked clients) | ❌ | ❌ | ❌ | **Very Low** | None feasible |

**Key insight**: The adapter architecture handles Patterns 1-3 cleanly. Patterns 4-5 are fundamentally invisible to widget-level introspection and require different approaches (or acceptance of the limitation).

### 18.3 Impact on Adapter Design

**No changes needed to the core adapter architecture.** The ScreenEngine/WidgetAdapter/ScreenDescriptor interfaces handle all widget-tree-based frameworks. The breakdown:

**Works with the adapter system as designed:**
- Vanilla MC screens (Cloth Config, ModMenu, Sodium, Iris, most ContainerScreens) → VanillaScreenEngine
- owo-lib screens (item-editor, Origins, etc.) → OwoScreenEngine
- Meteor Client screens → MeteorScreenEngine (existing)
- Any Screen subclass using standard `children()` → VanillaScreenEngine catches all

**Would need a new ScreenEngine but same interface:**
- REI overlay → ReiScreenEngine (hooks into REI's widget manager)
- EMI overlay → EmiScreenEngine (hooks into EMI's WidgetHolder)
- FancyMenu → FancyMenuScreenEngine (hooks into FancyMenu's element system)
- LDLib2 → LdlibScreenEngine (traverses Taffy-based tree)
- LibGUI → LibGuiScreenEngine (traverses WWidget tree)
- ReplayMod/jGui → JGuiScreenEngine (traverses jGui widget tree)

**Fundamentally invisible (no adapter possible):**
- HUD overlays (Jade, WTHIT, MiniHUD) — direct rendering, no widgets
- World-space rendering (WorldEditCUI, Distant Horizons LODs) — 3D data
- Immediate-mode GUIs (some hacked clients) — no persistent objects
- Raw OpenGL — completely opaque

**The design is sound.** The adapter system doesn't try to handle the impossible cases. It focuses on widget-tree-based frameworks, which covers the vast majority of testable GUI screens.

---

## 19. item-editor Edge Case Deep Dive

Detailed analysis of item-editor's owo screens to identify specific challenges for the DOM adapter.

### 19.1 Dialog System

Dialogs are **NOT inner classes**. They are static factory methods in standalone classes (`ConfirmationDialog`, `SearchablePickerDialog`, etc.) that return `FlowLayout` instances.

**Lifecycle:**
1. `ItemEditorDialogController.showDialog(FlowLayout dialog)` → `screen.attachDialog(dialog)`
2. `attachDialog()` → `clearDialog()` first, then `rootLayout.child(dialog)` — adds to StackLayout root
3. `clearDialog()` → `rootLayout.removeChild(activeDialog)`

Each dialog returns a `ModalOverlayLayout` (extends `FlowLayout`, implements `GreedyInputUIComponent`) that:
- Fills 100% × 100% with semi-transparent background
- Centers the dialog card
- **Consumes all mouse/keyboard events** within its bounding box (even outside the card)

**Adapter impact: ✅ Will just work.** Dialogs are standard owo FlowLayout children. The StackLayout ensures they're topmost. Coordinate-based clicks through `screen.mouseClicked()` dispatch correctly via owo's reverse-iteration.

### 19.2 Category Tab Switching

`refreshCurrentPanel()`:
1. `panelHost.clearChildren()` — **destroys entire panel subtree**
2. Creates new `EditorPanel` via factory
3. `panel.build()` → `UiFactory.appendFillChild(panelHost, panelComponent)`

**The entire panel subtree is destroyed and recreated on every category switch.**

**Adapter impact: ⚠️ Needs special handling.**
- ElementRef paths into old panels become invalid
- Must regenerate snapshot after category switch
- `switchModule()` triggers async `requestResponsiveRelayout()` — may need tick delay for new panel inflation
- Analogous to full page navigation in a browser — old element references are gone

### 19.3 RichTextAreaComponent

- Extends `TextAreaComponent` (owo built-in), implements `GreedyInputUIComponent`
- Has internal `editBox` (MultilineTextField) for cursor/selection management
- Handles double-click (word selection), shift-click (extend selection), undo/redo (Ctrl+Z/Y)
- `mouseClicked` uses `cursorForPoint()` — screen coords → text cursor positions

**Adapter impact: ⚠️ Needs careful handling.**
- Coordinate clicks work through owo's event system
- Text input works through `charTyped()` → `applyTextMutation()` → `editBox.insertText()`
- **But**: Complex internal state (cursor position, selection, pending style, undo history). Setting text externally would desync this state.
- Best approach: use owo's own event system (focus component → type characters) rather than direct field mutation

### 19.4 StorageScreen Hybrid

`StorageScreen extends ContainerScreen` — **NOT BaseOwoScreen**.

**Architecture:**
1. Extends vanilla `ContainerScreen` (chest grid, slot rendering, vanilla background)
2. Creates `OwoUIAdapter<StackLayout>` via `OwoUIAdapter.createWithoutScreen(...)` — standalone adapter
3. Builds owo widgets into this adapter
4. Adds adapter as vanilla `Renderable` via `addRenderableWidget(panelAdapter)`
5. Panel text stats drawn via `context.text()` — NOT owo components

**Adapter impact: ⚠️ Needs special handling.**
- `screen instanceof BaseOwoScreen` is **FALSE** — the OwoScreenEngine won't detect it
- The owo component tree is accessible via `panelAdapter.rootComponent` (private field)
- Vanilla slots rendered by ContainerScreen directly
- Need a **HybridScreenEngine** that detects this pattern (ContainerScreen with embedded OwoUIAdapter) and runs BOTH vanilla engine (for slots) and owo engine (for side panel)

### 19.5 Dropdowns

`ItemEditorScreen.openDropdown()` creates `DropdownComponent` via `openContextMenu()`:
- Positioned at computed coordinates near anchor button
- Added to StackLayout root as child
- Stays open until explicitly closed

**Adapter impact: ✅ Will just work.** Standard DropdownComponent in StackLayout. Ephemeral — removed after selection.

### 19.6 Keyboard Shortcuts

All handled in `ItemEditorScreen.keyPressed()`:
- Ctrl+S → apply, Ctrl+R → reset, Ctrl+Tab → next category
- Handled at screen level, NOT in owo event system
- Uses `input.hasControlDownWithQuirk()` (Ctrl or Cmd on macOS)
- MC 26.x uses `KeyEvent` type (not old int triple)

**Adapter impact: ⚠️ Needs proper KeyEvent construction.** The `press_screen_key` tool must construct proper `KeyEvent` objects with correct modifier flags. When a dialog is open, Ctrl+S maps to "confirm dialog" instead of "apply".

### 19.7 Edge Case Summary

| Feature | Works? | Notes |
|---------|--------|-------|
| Dialogs | ✅ | Standard FlowLayout children of StackLayout |
| Modal event consumption | ✅ | owo-level dispatch handles it |
| Category switching | ⚠️ | Panel destroyed/rebuilt — cache invalidation |
| RichTextAreaComponent | ⚠️ | Use owo events for text, don't mutate directly |
| StorageScreen hybrid | ⚠️ | Not BaseOwoScreen — needs HybridScreenEngine |
| Dropdowns | ✅ | Standard component, ephemeral |
| Keyboard shortcuts | ⚠️ | Need proper KeyEvent + modifier construction |
| Responsive relayout | ⚠️ | Async multi-pass — may need tick delays |

---

## 20. Adapter Architecture Stress-Test Conclusions

### Does the broader ecosystem invalidate any design decisions?

**No.** The adapter architecture holds up well:

1. **The ScreenEngine interface is sufficient.** Every widget-tree-based framework can implement `canHandle()`, `buildDom()`, and `engineName()`. The priority system ensures the most specific engine wins.

2. **The WidgetAdapter interface is sufficient.** Unified interface with default no-ops handles all widget types. No need to split into metadata/interaction/action interfaces.

3. **The AdapterRegistry pattern works.** Registration gated by `FabricLoader.isModLoaded()` allows optional framework support without hard dependencies.

4. **The element ID scheme (prefix per engine) avoids collisions.** `v-1`, `o-1` prefixes clearly identify which engine produced the element.

5. **The "impossible" cases are correctly excluded.** Direct rendering, HUD overlays, and raw OpenGL are acknowledged as limitations, not design failures.

### What needs to be added to the design?

1. **HybridScreenEngine** — For screens like StorageScreen that combine vanilla ContainerScreen with an embedded owo adapter. Detects the pattern and runs both engines.

2. **Snapshot invalidation strategy** — When elements are dynamically added/removed (panel switches, dialog open/close), the DOM snapshot cache must be invalidated. Consider:
   - Invalidate on every `get_screen_dom` call (simple but potentially slow)
   - Track "screen generation" counter (increment on known mutation events)
   - Re-snapshot on interaction failure (fallback)

3. **Async layout awareness** — owo's responsive relayout is multi-pass and async. After triggering a relayout (category switch, resize), the adapter may need to wait 1-2 ticks for the layout to stabilize before snapshotting.

4. **KeyEvent construction helper** — MC 26.x uses `KeyEvent` objects instead of raw ints. The adapter needs a utility to construct proper `KeyEvent` instances with modifier flags.

### Priority of future ScreenEngine implementations

Based on ecosystem prevalence:

| Priority | Engine | Why |
|----------|--------|-----|
| **P0** | VanillaScreenEngine | Covers Cloth Config, ModMenu, Sodium, Iris, most container screens, Create |
| **P0** | OwoScreenEngine | Covers item-editor, Origins, growing adoption |
| **P1** | HybridScreenEngine | Covers StorageScreen-type hybrid vanilla+owo |
| **P2** | ReiScreenEngine | REI is extremely widespread; overlay is separate widget tree |
| **P2** | EmiScreenEngine | EMI growing rapidly; similar overlay pattern |
| **P3** | FancyMenuScreenEngine | 187M downloads but niche use case for testing |
| **P3** | LdlibScreenEngine | Niche but used by major tech mods |
| **P4** | LibGuiScreenEngine | Very niche |
| **P4** | JGuiScreenEngine | ReplayMod only |

### Universal mod naming consideration

Given the research, the mod name "universal" is slightly misleading — it's not truly universal (can't handle direct rendering). Better names might be:
- `mcp-gui-harness` — descriptive of what it does
- `screen-test-harness` — focused on Screen testing
- `widget-test-harness` — focused on widget introspection

But "universal" works well enough as a working name. The README can clarify the scope.

---

## 21. Architecture Stress-Test Results

Final stress-test of the adapter design against ecosystem-wide edge cases.

### 21.1 HybridScreenEngine (Necessary Addition)

**Problem**: StorageScreen extends vanilla ContainerScreen but has an embedded OwoUIAdapter (not BaseOwoScreen). OwoScreenEngine won't detect it.

**Solution**: Add a HybridScreenEngine that:
1. Checks if a vanilla Screen has an `OwoUIAdapter` field (via reflection)
2. If found, runs BOTH VanillaScreenEngine (for slots/standard widgets) AND OwoScreenEngine (for the embedded adapter's component tree)
3. Merges results with prefixed IDs to avoid collisions

```java
public class HybridScreenEngine implements ScreenEngine {
    public boolean canHandle(Screen screen) {
        // Not a BaseOwoScreen, but has an OwoUIAdapter field
        if (screen instanceof BaseOwoScreen) return false;
        return hasOwoAdapterField(screen);
    }
    
    public List<Map<String, Object>> buildDom(Screen screen, DomBuildContext ctx) {
        List<Map<String, Object>> merged = new ArrayList<>();
        
        // Run vanilla engine for standard children + container slots
        merged.addAll(vanillaEngine.buildDom(screen, ctx));
        
        // Run owo engine for the embedded adapter's component tree
        OwoUIAdapter<?> adapter = extractOwoAdapter(screen);
        merged.addAll(owoEngine.buildDomFromRoot(adapter.rootComponent(), ctx));
        
        return merged;
    }
}
```

**Priority**: 5 (between OwoScreenEngine=10 and VanillaScreenEngine=0). Checked after pure owo but before pure vanilla.

### 21.2 Snapshot Invalidation Strategy

**Problem**: owo responsive relayout and item-editor panel switching cause element references to become stale.

**Solution**: Snapshots are **stateless by design**. Each `get_screen_dom` call performs a fresh tree walk. No cross-snapshot reference stability.

- ElementRefs validate against the current snapshot only
- The 8-snapshot cache (from the Meteor addon) can be kept for diff/comparison but should NOT be relied on for interaction
- When interaction fails (element not found), the system automatically re-snapshots and retries

**Optional enhancement**: Generation counter per-screen-instance. Incremented on known mutation events (tick-based or resize-based). Cached snapshots are invalidated when generation changes.

### 21.3 owo-Wraps-Vanilla Pattern

**Problem**: owo's ButtonComponent extends vanilla ButtonWidget, TextBoxComponent extends TextFieldWidget. The VanillaScreenEngine's `instanceof AbstractWidget` check would match these.

**Solution**: This is a **feature, not a bug** — but requires priority ordering to avoid double-counting.

- OwoScreenEngine (priority=10) claims ALL BaseOwoScreen instances first
- Within OwoScreenEngine, the owo-specific traversal handles all components including vanilla-wrapped ones
- VanillaScreenEngine (priority=0) only runs on non-BaseOwoScreen, non-HybridScreen screens
- No double-counting because engines are mutually exclusive per screen

On hybrid screens (StorageScreen), the HybridScreenEngine partitions the element space: vanilla engine handles `screen.children()` (which won't include owo components), owo engine handles the adapter's root.

### 21.4 REI/EMI Overlay Augmentation (Deferred)

**Problem**: REI and EMI maintain parallel widget trees rendered as overlays on inventory screens. These are NOT in `screen.children()`.

**Solution for v1**: Not supported. Document as known limitation.

**Future architecture** (v2+): Add an OverlayAugmentor SPI:

```java
public interface OverlayAugmentor {
    boolean isPresent();
    List<Map<String, Object>> augmentOverlay(Screen screen, DomBuildContext ctx);
}
```

Registered per-overlay-mod (ReiAugmentor, EmiAugmentor). Called after the main ScreenEngine finishes. Overlay elements are merged into the snapshot with a distinct prefix (`r-` for REI, `e-` for EMI).

### 21.5 Narration API as Generic Fallback

**Discovery**: MC 26.x has a narration system where widgets implement `updateWidgetNarration(NarrationElementOutput)`. This provides text descriptions of widget state.

**Application**: For unknown widget types (no registered WidgetAdapter), the DOM engine can invoke `updateWidgetNarration()` and capture the output as a `narration` field in the element metadata. This gives LLM agents meaningful text content even for completely custom widgets.

```java
// In VanillaScreenEngine or generic fallback
if (widget instanceof NarratableEntry narratable) {
    NarrationElementOutput output = NarrationElementOutput.create();
    narratable.updateWidgetNarration(output);
    el.put("narration", output.getString());
}
```

This is an excellent low-effort fallback that works for any widget implementing the narration interface (which is most standard MC widgets and many mod widgets).

### 21.6 Migration Risk Assessment

Moving existing root project → `meteor-addon/` subdirectory:

| Risk | Impact | Mitigation |
|------|--------|------------|
| Git history | Low | `git mv` preserves history. All blame/log works across the move |
| IDE reimport | Low | IntelliJ/Eclipse handle Gradle multi-project well. One-time reimport needed |
| Build scripts | Medium | Any CI/CD referencing root `build.gradle.kts` paths needs updating |
| Fabric Loom runs | Low | Run configs generated per subproject. `./gradlew :meteor-addon:runClient` |
| settings.gradle.kts | Medium | Root becomes multi-project include. Each subproject gets own settings |
| Gradle wrapper | Low | Shared at root. Both subprojects use same wrapper |

**Recommendation**: Use `git mv` for all file moves. Test the migrated build thoroughly before creating the universal/ subproject.

### 21.7 owo-lib Version Sensitivity

**Risk**: The OwoScreenEngine accesses owo internals:
- `BaseOwoScreen.uiAdapter` field (protected, not public)
- `OwoUIAdapter.rootComponent` field
- `ParentComponent.children()` API
- `ScrollContainer.child()` accessor

**Mitigation**:
1. Cache all reflected Field/Method lookups (one-time cost)
2. Wrap all reflective access in try/catch with clear error messages
3. Version-gate the owo adapter: check `FabricLoader.getModContainer("owo-lib").get().getMetadata().getVersion()` and warn if version is untested
4. The `ParentComponent.children()` and `Component.x()/y()/width()/height()` methods are public API — stable across versions
5. The `uiAdapter` field access is the most fragile part — consider adding a fallback accessor mixin

### 21.8 Final Verdict

**The adapter architecture is sound and ready for implementation.** No fundamental changes needed. The additions identified during stress-testing are incremental:

1. ✅ HybridScreenEngine — handles StorageScreen-type hybrid screens
2. ✅ Stateless snapshots — fresh walk each time, no stale references
3. ✅ Priority-based engine dispatch — avoids double-counting
4. 📋 Overlay augmentation — deferred to v2
5. ✅ Narration API fallback — easy win for unknown widgets
6. ✅ Migration via git mv — low risk
7. ✅ Version-gated owo access — fragile but manageable

The design handles all widget-tree-based frameworks (vanilla MC, owo-lib, Cloth Config, Meteor, LDLib2, LibGUI, REI, EMI, FancyMenu) through the same adapter interfaces. The only excluded cases are direct rendering and raw OpenGL — which are correctly acknowledged as limitations.

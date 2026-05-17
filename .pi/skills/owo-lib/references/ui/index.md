# UI Framework (owo-ui)

owo-ui is owo-lib's UI framework providing a declarative, component-based system for building Minecraft GUIs.

## File Index

### Wiki / Tutorial Content
- `getting-started.md` - Overview, TOC, and code-driven paradigm tutorial
- `getting-started-data-driven.md` - Data-driven paradigm (XML) tutorial and what's next
- `component-basics.md` - Core component concepts: sizing, positioning
- `layout-basics.md` - Layout types (flow, grid), layout properties, margins, padding, overflow, scrolling
- `components-*.md` - Individual component guides (button, checkbox, collapsible-container, dropdown, flow-layout, grid-layout, label, scroll-container, slider, templates)
- `templates.md` - Value replacement in templates

### API Reference (JavaDoc)
- `api-core-types.md` - Core types: AnimatableProperty, Color, Easing, OwoUIAdapter, OwoUIGraphics, PositionedRectangle, Positioning
- `api-core-sizing.md` - Sizing class and methods
- `api-components.md` - Component classes: BoxComponent, DropdownComponent, ItemComponent, SliderComponent, TextBoxComponent, UIComponents
- `api-containers.md` - Container classes: FlowLayout, OverlayContainer, ScrollContainer, StackLayout, WrappingParentUIComponent
- `api-screens.md` - Screen/handler classes: BaseOwoScreen, BaseOwoContainerScreen, BaseUIModelScreen, etc.
- `api-hud.md` - HUD + inject classes: Hud, HudContainer, GreedyInputUIComponent, UIComponentStub
- `api-layers.md` - Layer classes: Layer, Instance, Layers
- `api-parsing.md` - Parsing/model classes: UIModel, UIModelLoader, UIParsing
- `api-util.md` - Utility interfaces: CommandOpenedScreen, Delta, DisposableScreen, MatrixStackTransformer

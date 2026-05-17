## Package: `io.wispforest.owo.ui.hud`

### `class` Hud

> A utility for displaying owo-ui components on the in-game HUD - rendered during {@link HudRenderCallback}
>

**Methods:**

- `void add(Identifier id, Supplier<UIComponent> component)`
  > Add a new component to be rendered on the in-game HUD. The root container used by the HUD does not support layout positioning - the component supplied by {@code component} must be explicitly positioned via either {@link io.wispforest.owo.ui.core.Positioning#absolute(int, int)} or {@link io.wispforest.owo.ui.core.Positioning#relative(int, int)} @param id        An ID uniquely describing this HUD component @param component A function creating the component                  when the HUD is first rendered
- `void remove(Identifier id)`
  > Remove the HUD component described by the given ID @param id The ID of the HUD component to remove
- `boolean hasComponent(Identifier id)`
  > @return {@code true} if there is an active HUD component described by {@code id}

### `class` HudContainer

> Very simple extension of {@link io.wispforest.owo.ui.container.FlowLayout} that does not allow children to be layout-positioned, used by {@link Hud}
>

**Methods:**

- `void mountChild(@Nullable UIComponent child, Consumer<UIComponent> layoutFunc)`

## Package: `io.wispforest.owo.ui.inject`

### `interface` GreedyInputUIComponent

> A marker interface for components which consume text input when focused - this is used to prevent handled screens from closing when said component is focused and the inventory key is pressed
>

### `interface` UIComponentStub

> Stub-version of component which adds implementations for all methods that unconditionally throw - used for interface-injecting onto vanilla widgets
>


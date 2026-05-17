## Package: `io.wispforest.owo.ui.layers`

### `class` Layer

**Fields:**

- `S screen` — The screen this instance is attached to
- `OwoUIAdapter<R> adapter` — The UI adapter of this instance - get the {@link OwoUIAdapter#rootComponent} from this to start building your UI tree
- `boolean aggressivePositioning` — Whether this layer should aggressively update widget-relative positioning every frame - useful if the targeted widget moves frequently

**Methods:**

- `Instance instantiate(S screen)`
- `void resize(int width, int height)`
- `void alignComponentToWidget(Predicate<AbstractWidget> locator, AnchorSide anchor, float justification, UIComponent component)`
  > Align the given component to a widget in the attached screen's widget tree. The widget is located by passing the locator predicate to {@link #queryWidget(Predicate)} and getting the position of the resulted widget. <p> If no widget can be found, the component gets positioned at 0,0 @param locator       A predicate to match which identifies the targeted widget @param anchor        On which side of the targeted widget to anchor the component @param justification How far along the anchor side of the widget in positive axis direction                      to position the component @param component     The component to position
- `void alignComponentToHandledScreenCoordinates(UIComponent component, int x, int y)`
  > Align the given component relative to the handled screen coordinates as used by vanilla for positioning slots <p> For obvious reasons, this method may only be invoked on layers which are pushed onto instances of {@link AbstractContainerScreen} @param component The component to position @param x         The X coordinate of the component, relative to the handled screen's origin @param y         The Y coordinate of the component, relative to the handled screen's origin
- `void dispatchLayoutUpdates()`

### `class` Instance

**Fields:**

- `S screen` — The screen this instance is attached to
- `OwoUIAdapter<R> adapter` — The UI adapter of this instance - get the {@link OwoUIAdapter#rootComponent} from this to start building your UI tree
- `boolean aggressivePositioning` — Whether this layer should aggressively update widget-relative positioning every frame - useful if the targeted widget moves frequently

**Methods:**

- `void resize(int width, int height)`
- `void alignComponentToWidget(Predicate<AbstractWidget> locator, AnchorSide anchor, float justification, UIComponent component)`
  > Align the given component to a widget in the attached screen's widget tree. The widget is located by passing the locator predicate to {@link #queryWidget(Predicate)} and getting the position of the resulted widget. <p> If no widget can be found, the component gets positioned at 0,0 @param locator       A predicate to match which identifies the targeted widget @param anchor        On which side of the targeted widget to anchor the component @param justification How far along the anchor side of the widget in positive axis direction                      to position the component @param component     The component to position
- `void alignComponentToHandledScreenCoordinates(UIComponent component, int x, int y)`
  > Align the given component relative to the handled screen coordinates as used by vanilla for positioning slots <p> For obvious reasons, this method may only be invoked on layers which are pushed onto instances of {@link AbstractContainerScreen} @param component The component to position @param x         The X coordinate of the component, relative to the handled screen's origin @param y         The Y coordinate of the component, relative to the handled screen's origin
- `void dispatchLayoutUpdates()`

### `class` Layers

> A system for adding owo-ui components onto existing screens.
>
> You can create a new layer by calling {@link #add(BiFunction, Consumer, Class[])}. The second argument to this function is the instance initializer, which is where you configure instances of your layer added onto screens when they get initialized. This is the place to configure the UI adapter of your layer as well as building your UI tree onto the root component of said adapter
>
> Just like proper owo-ui screens, layers preserve state when the client's window is resized - they are only initialized once, when the screen is first opened
>

**Fields:**

- `Identifier INIT_PHASE` — The event phase during which owo-ui layer instances are created and initialized. This runs after the default phase

**Methods:**

- `Layer<S, R> add(BiFunction<Sizing, Sizing, R> rootComponentMaker, Consumer<Layer<S, R>.Instance> instanceInitializer, Class<? extends S>... screenClasses)`
  > Add a new layer to the given screens @param rootComponentMaker  A function which will create the root component of this layer @param instanceInitializer A function which will initialize any instances of this layer which get created.                            This is where you add components or configure the UI adapter of the generated layer instance @param screenClasses       The screens onto which to add the new layer


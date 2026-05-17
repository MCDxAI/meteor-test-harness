## Package: `io.wispforest.owo.ui.base`

### `class` BaseOwoContainerScreen

**Fields:**

- `OwoUIAdapter<R> uiAdapter` — The UI adapter of this screen. This handles all user input as well as setting up GL state for rendering and managing component focus
- `boolean invalid` — Whether this screen has encountered an unrecoverable error during its lifecycle and should thus close itself on the next frame

**Methods:**

- `void build(R rootComponent)`
  > Build the component hierarchy of this screen, called after the adapter and root component have been initialized by {@link #createAdapter()} @param rootComponent The root component created                      in the previous initialization step
- `void init()`
- `void drawComponentTooltip(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta)`
  > Draw the tooltip of this screen's component tree, invoked by {@link ScreenEvents#afterRender(Screen)} so that tooltips are properly rendered above content
- `void disableSlot(int index)`
  > Disable the slot at the given index. Note that this is hard override and the slot cannot re-enable itself @param index The index of the slot to disable
- `void disableSlot(Slot slot)`
  > Disable the given slot. Note that this is hard override and the slot cannot re-enable itself
- `void enableSlot(int index)`
  > Enable the slot at the given index. Note that this is an override and cannot enable a slot that is disabled through its own will @param index The index of the slot to enable
- `void enableSlot(Slot slot)`
  > Enable the given slot. Note that this is an override and cannot enable a slot that is disabled through its own will
- `boolean isSlotEnabled(int index)`
  > @return whether the given slot is enabled or disabled using the {@link OwoSlotExtension} disabling functionality
- `boolean isSlotEnabled(Slot slot)`
  > @return whether the given slot is enabled or disabled using the {@link OwoSlotExtension} disabling functionality
- `SlotComponent slotAsComponent(int index)`
  > Wrap the slot at the given index in this screen's menu into a component, so it can be managed by the UI system @param index The index the slot occupies in the menu's slot list @return The wrapped slot
- `C component(Class<C> expectedClass, String id)`
  > A convenience shorthand for querying a component from the adapter's root component via {@link ParentUIComponent#childById(Class, String)}
- `Stream<UIComponent> componentsForExclusionAreas()`
- `void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta)`
- `void render(GuiGraphics vanillaContext, int mouseX, int mouseY, float delta)`
- `boolean keyPressed(KeyEvent input)`
- `boolean mouseClicked(MouseButtonEvent click, boolean doubled)`
- `boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY)`
- `boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)`
- `void dispose()`
- `void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY)`
- `void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta)`
- `void update(float delta, int mouseX, int mouseY)`
- `void drawTooltip(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta)`
- `boolean shouldDrawTooltip(double mouseX, double mouseY)`
- `int determineHorizontalContentSize(Sizing sizing)`
- `int determineVerticalContentSize(Sizing sizing)`
- `void updateX(int x)`
- `void updateY(int y)`

### `class` BaseOwoScreen

> A minimal implementation of a Screen which fully supports all aspects of the UI system. Implementing this class is trivial, as you only need to provide implementations for {@link #createAdapter()} to initialize the UI system and {@link #build(ParentUIComponent)} which is where you declare your component hierarchy.
>
> Should you be locked into a different superclass on your screen already, you can easily copy all code from this class into your screen - as you can see supporting the entire feature-set of owo-ui only requires very few changes to how a vanilla screen works @param <R> The type of root component this screen uses
>

**Fields:**

- `OwoUIAdapter<R> uiAdapter` — The UI adapter of this screen. This handles all user input as well as setting up GL state for rendering and managing component focus
- `boolean invalid` — Whether this screen has encountered an unrecoverable error during its lifecycle and should thus close itself on the next frame

**Methods:**

- `void build(R rootComponent)`
  > Build the component hierarchy of this screen, called after the adapter and root component have been initialized by {@link #createAdapter()} @param rootComponent The root component created                      in the previous initialization step
- `void init()`
- `void drawComponentTooltip(GuiGraphics drawContext, int mouseX, int mouseY, float tickDelta)`
  > Draw the tooltip of this screen's component tree, invoked by {@link ScreenEvents#afterRender(Screen)} so that tooltips are properly rendered above content
- `C component(Class<C> expectedClass, String id)`
  > A convenience shorthand for querying a component from the adapter's root component via {@link ParentUIComponent#childById(Class, String)}
- `void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta)`
- `void render(GuiGraphics context, int mouseX, int mouseY, float delta)`
- `boolean keyPressed(KeyEvent input)`
- `boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY)`
- `void dispose()`

### `class` BaseParentUIComponent

> The reference implementation of the {@link ParentUIComponent} interface, serving as a base for all parent components on owo-ui. If you need your own parent component, it is often beneficial to subclass one of owo-ui's existing layout classes, especially {@link WrappingParentUIComponent} is often useful
>

**Methods:**

- `void update(float delta, int mouseX, int mouseY)`
- `void parentUpdate(float delta, int mouseX, int mouseY)`
  > Update the state of this component before drawing the next frame. This method is separated from {@link #update(float, int, int)} to enforce the task queue always being run last @param delta  The duration of the last frame, in partial ticks @param mouseX The mouse pointer's x-coordinate @param mouseY The mouse pointer's y-coordinate
- `void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta)`
- `void queue(Runnable task)`
- `ParentUIComponent verticalAlignment(VerticalAlignment alignment)`
- `VerticalAlignment verticalAlignment()`
- `ParentUIComponent horizontalAlignment(HorizontalAlignment alignment)`
- `HorizontalAlignment horizontalAlignment()`
- `ParentUIComponent padding(Insets padding)`
- `AnimatableProperty<Insets> padding()`
- `boolean allowOverflow()`
- `Surface surface()`
- `void inflate(Size space)`
- `void updateLayout()`
- `void runAndDeferEvents(Runnable action)`
- `void onChildMutated(UIComponent child)`
- `boolean onMouseDown(MouseButtonEvent click, boolean doubled)`
- `boolean onMouseUp(MouseButtonEvent click)`
- `boolean onMouseScroll(double mouseX, double mouseY, double amount)`
- `boolean onMouseDrag(MouseButtonEvent click, double deltaX, double deltaY)`
- `boolean onKeyPress(KeyEvent input)`
- `boolean onCharTyped(CharacterEvent input)`
- `void updateX(int x)`
- `void updateY(int y)`
- `Size childMountingOffset()`
  > @return The offset from the origin of this component at which children can start to be mounted. Accumulates padding as well as padding from content sizing
- `void mountChild(@Nullable UIComponent child, Consumer<UIComponent> layoutFunc)`
  > Mount a child using the given mounting function if its positioning is equal to {@link Positioning#layout()}, or according to its intrinsic positioning otherwise @param child      The child to mount @param layoutFunc The mounting function for components which follow the layout
- `void drawChildren(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta, List<? extends UIComponent> children)`
  > Draw the children of this component along with their focus outline and tooltip, optionally clipping them if {@link #allowOverflow} is {@code false} @param children The list of children to draw
- `Size calculateChildSpace(Size thisSpace)`
  > Calculate the space for child inflation. If a given axis is content-sized, return the respective value from {@code thisSpace} @param thisSpace The space for layout inflation of this widget @return The available space for child inflation
- `BaseParentUIComponent positioning(Positioning positioning)`
- `BaseParentUIComponent margins(Insets margins)`

### `class` BaseUIComponent

> The reference implementation of the {@link UIComponent} interface, ideally you should extend this when making your own components
>

**Methods:**

- `int determineHorizontalContentSize(Sizing sizing)`
  > @return The horizontal size this component needs to fit its contents
- `int determineVerticalContentSize(Sizing sizing)`
  > @return The vertical size this component needs to fit its contents
- `void inflate(Size space)`
- `void applySizing()`
  > Calculate and apply the sizing of this component according to the last known expansion space
- `void notifyParentIfMounted()`
- `C configure("unchecked")`
- `void runAndDeferEvents(Runnable action)`
- `void update(float delta, int mouseX, int mouseY)`
- `void updateHoveredState(int mouseX, int mouseY, boolean nowHovered)`
- `boolean onMouseDown(MouseButtonEvent click, boolean doubled)`
- `EventSource<MouseDown> mouseDown()`
- `boolean onMouseUp(MouseButtonEvent click)`
- `EventSource<MouseUp> mouseUp()`
- `boolean onMouseScroll(double mouseX, double mouseY, double amount)`
- `EventSource<MouseScroll> mouseScroll()`
- `boolean onMouseDrag(MouseButtonEvent click, double deltaX, double deltaY)`
- `EventSource<MouseDrag> mouseDrag()`
- `boolean onKeyPress(KeyEvent input)`
- `EventSource<KeyPress> keyPress()`
- `boolean onCharTyped(CharacterEvent input)`
- `EventSource<CharTyped> charTyped()`
- `void onFocusGained(FocusSource source)`
- `EventSource<FocusGained> focusGained()`
- `void onFocusLost()`
- `EventSource<FocusLost> focusLost()`
- `EventSource<MouseEnter> mouseEnter()`
- `EventSource<MouseLeave> mouseLeave()`
- `CursorStyle cursorStyle()`
- `UIComponent tooltip(List<ClientTooltipComponent> tooltip)`
- `List<ClientTooltipComponent> tooltip()`
- `void mount(ParentUIComponent parent, int x, int y)`
- `void dismount(DismountReason reason)`
- `ParentUIComponent parent()`
- `BaseUIComponent positioning(Positioning positioning)`
- `AnimatableProperty<Positioning> positioning()`
- `BaseUIComponent margins(Insets margins)`
- `AnimatableProperty<Insets> margins()`
- `AnimatableProperty<Sizing> horizontalSizing()`
- `UIComponent verticalSizing(Sizing verticalSizing)`
- `AnimatableProperty<Sizing> verticalSizing()`
- `UIComponent id(@Nullable String id)`

### `class` BaseUIModelContainerScreen

**Fields:**

- `UIModel model` — The UI model this screen is built upon, parsed from XML. This is usually not relevant to subclasses, the UI adapter inherited from {@link BaseOwoScreen} is more interesting

**Methods:**

- `boolean keyPressed(KeyEvent input)`

### `class` BaseUIModelScreen

> A simple base implementation of a screen that builds its UI upon the base of a UI model parsed from an XML file. To work with this system, declare your UI structure in an XML file and pass it into the super constructor call using the relevant {@link DataSource}.
>
> You can then query and set up different components of your UI hierarchy using {@link ParentUIComponent#childById(Class, String)} in the {@link #build(ParentUIComponent)} method @param <R> The type of root component this screen expects from the UI model
>

#### `interface` DataSource

> A source of UI model data, by default can be loaded from a file or resourcepack. If you need a different way of fetching the model - implement this interface and pass it to the {@code super(...)} call in your constructor
>

**Fields:**

- `UIModel model` — The UI model this screen is built upon, parsed from XML. This is usually not relevant to subclasses, the UI adapter inherited from {@link BaseOwoScreen} is more interesting
- `interface DataSource` — A source of UI model data, by default can be loaded from a file or resourcepack. If you need a different way of fetching the model - implement this interface and pass it to the {@code super(...)} call in your constructor

**Methods:**

- `boolean keyPressed(KeyEvent input)`
- `void reportError()`
- `void reportError()`

### `interface` DataSource

> A source of UI model data, by default can be loaded from a file or resourcepack. If you need a different way of fetching the model - implement this interface and pass it to the {@code super(...)} call in your constructor
>

**Methods:**

- `void reportError()`
- `void reportError()`


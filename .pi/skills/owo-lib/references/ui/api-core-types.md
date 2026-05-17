## Package: `io.wispforest.owo.ui.core`

### `class` AnimatableProperty

> A container which holds an animatable object, used to manage to properties of UI components. Extends the {@link Observable} container so that changes in its value can be propagated to the holder of the property @param <A> The type of animatable object this property describes
>

**Methods:**

- `Animation<A> animate(int duration, Easing easing, A to)`
  > Create an animation object which interpolates the state of this property from the current one to {@code to} in {@code duration} milliseconds, applying the given easing <p> This method replaces the current animation object of this property - it will not be updated anymore @param duration The duration of the animation to create, in milliseconds @param easing   The easing method to use @param to       The target state of this property @return The new animation of this property.
- `void update(float delta)`
  > Update the currently stored animation object of this property @param delta The duration of the last frame, in partial ticks

### `record` Color

**Methods:**

- `Color ofArgb(int argb)`
- `Color ofRgb(int rgb)`
- `Color ofHsv(float hue, float saturation, float value)`
- `Color ofHsv(float hue, float saturation, float value, float alpha)`
- `Color ofFormatting(@NotNull ChatFormatting formatting)`
- `Color ofDye(@NotNull DyeColor dyeColor)`
- `Color random()`
  > Generates a random color @apiNote Don't tell glisco about this @author chyzman
- `int rgb()`
- `int argb()`
- `float[] hsv()`
- `String asHexString(boolean includeAlpha)`
- `Color interpolate(Color next, float delta)`
- `Color parse(Node node)`
  > Tries to interpret the given node's text content as a color in {@code #RRGGBB} or {@code #AARRGGBB} format, or as the name of a text color @return The parsed color as an unsigned integer @throws UIModelParsingException If the text content does not match                                 the expected color format
- `int parseAndPack(Node node)`

### `interface` Easing

> An easing function which can smoothly move an interpolation value from 0 to 1
>

### `class` OwoUIAdapter

> A UI adapter constitutes the main entrypoint to using owo-ui. It takes care of rendering the UI tree correctly, handles input events and cursor styling as well as the component inspector.
>
> Additionally, the adapter implements all interfaces required for it to be treated as a normal widget by the vanilla screen system - this means even if you choose to not use {@link io.wispforest.owo.ui.base.BaseOwoScreen} you can always simply add it as a widget and get most of the functionality working out of the box
>
> To draw the UI tree managed by this adapter, call {@link OwoUIAdapter#render(GuiGraphics, int, int, float)}. Note that this does not draw the current tooltip of the UI - this must be done separately by invoking {@link #drawTooltip(GuiGraphics, int, int, float)}. If in a scenario with multiple adapters or other sources rendering UI elements to the screen, it is generally desirable to delay tooltip drawing until after all UI is drawn to avoid layering issues. @see io.wispforest.owo.ui.base.BaseOwoScreen
>

**Methods:**

- `OwoUIAdapter<R> create(Screen screen, BiFunction<Sizing, Sizing, R> rootComponentMaker)`
  > Create a UI adapter for the given screen. This also sets it up to be rendered and receive input events, without needing you to do any more setup @param screen             The screen for which to create an adapter @param rootComponentMaker A function which will create the root component of this screen @param <R>                The type of root component the created adapter will use @return The new UI adapter, already set up for the given screen
- `OwoUIAdapter<R> createWithoutScreen(int x, int y, int width, int height, BiFunction<Sizing, Sizing, R> rootComponentMaker)`
  > Create a new UI adapter without the specific context of a screen - use this method when you want to embed owo-ui into a different context @param x                  The x-coordinate of the top-left corner of the root component @param y                  The y-coordinate of the top-left corner of the root component @param width              The width of the available area, in pixels @param height             The height of the available area, in pixels @param rootComponentMaker A function which will create the root component of the adapter @param <R>                The type of root component the created adapter will use @return The new UI adapter, ready for layout inflation
- `void inflateAndMount()`
  > Begin the layout process of the UI tree and mount the tree once the layout is inflated <p> After this method has executed, this adapter is ready for rendering
- `void moveAndResize(int x, int y, int width, int height)`
- `void dispose()`
- `boolean toggleInspector()`
  > @return Toggle rendering of the inspector
- `boolean toggleGlobalInspector()`
  > @return Toggle the inspector between hovered and global mode
- `int x()`
- `int y()`
- `int width()`
- `int height()`
- `void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)`
- `void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)`
  > Draw the current tooltip of the UI managed by this adapter. This method must not be called without a previous, corresponding call to {@link #render(GuiGraphics, int, int, float)} @since 0.12.19
- `boolean mouseReleased(MouseButtonEvent click)`
- `boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)`
- `boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY)`
- `boolean keyPressed(KeyEvent input)`
- `boolean charTyped(CharacterEvent input)`
- `NarrationPriority narrationPriority()`
- `void updateNarration(NarrationElementOutput builder)`

### `class` OwoUIGraphics

**Methods:**

- `OwoUIGraphics of(GuiGraphics graphics)`
- `UtilityScreen utilityScreen()`
- `boolean intersectsScissor(PositionedRectangle other)`
- `void drawRectOutline(int x, int y, int width, int height, int color)`
- `void drawRectOutline(RenderPipeline pipeline, int x, int y, int width, int height, int color)`
  > Draw the outline of a rectangle @param x      The x-coordinate of top-left corner of the rectangle @param y      The y-coordinate of top-left corner of the rectangle @param width  The width of the rectangle @param height The height of the rectangle @param color  The color of the rectangle
- `void drawGradientRect(int x, int y, int width, int height, int topLeftColor, int topRightColor, int bottomRightColor, int bottomLeftColor)`
- `void drawGradientRect(RenderPipeline pipeline, int x, int y, int width, int height, int topLeftColor, int topRightColor, int bottomRightColor, int bottomLeftColor)`
  > Draw a filled rectangle with a gradient @param x                The x-coordinate of top-left corner of the rectangle @param y                The y-coordinate of top-left corner of the rectangle @param width            The width of the rectangle @param height           The height of the rectangle @param topLeftColor     The color at the rectangle's top left corner @param topRightColor    The color at the rectangle's top right corner @param bottomRightColor The color at the rectangle's bottom right corner @param bottomLeftColor  The color at the rectangle's bottom left corner
- `void drawPanel(int x, int y, int width, int height, boolean dark)`
  > Draw a panel that looks like the background of a vanilla inventory screen @param x      The x-coordinate of top-left corner of the panel @param y      The y-coordinate of top-left corner of the panel @param width  The width of the panel @param height The height of the panel @param dark   Whether to use the dark version of the panel texture
- `void drawSpectrum(int x, int y, int width, int height, boolean vertical)`
- `void drawText(Component text, float x, float y, float scale, int color)`
- `void drawText(Component text, float x, float y, float scale, int color, TextAnchor anchorPoint)`
- `void drawLine(int x1, int y1, int x2, int y2, double thiccness, Color color)`
- `void drawLine(RenderPipeline pipeline, int x1, int y1, int x2, int y2, double thiccness, Color color)`
- `void drawCircle(int centerX, int centerY, int segments, double radius, Color color)`
- `void drawCircle(int centerX, int centerY, double angleFrom, double angleTo, int segments, double radius, Color color)`
- `void drawCircle(RenderPipeline pipeline, int centerX, int centerY, int segments, double radius, Color color)`
- `void drawCircle(RenderPipeline pipeline, int centerX, int centerY, double angleFrom, double angleTo, int segments, double radius, Color color)`
- `void drawRing(int centerX, int centerY, int segments, double innerRadius, double outerRadius, Color innerColor, Color outerColor)`
- `void drawRing(int centerX, int centerY, double angleFrom, double angleTo, int segments, double innerRadius, double outerRadius, Color innerColor, Color outerColor)`
- `void drawRing(RenderPipeline pipeline, int centerX, int centerY, int segments, double innerRadius, double outerRadius, Color innerColor, Color outerColor)`
- `void drawRing(RenderPipeline pipeline, int centerX, int centerY, double angleFrom, double angleTo, int segments, double innerRadius, double outerRadius, Color innerColor, Color outerColor)`
- `void drawTooltip(Font textRenderer, int x, int y, List<ClientTooltipComponent> components)`
- `void drawTooltip(Font textRenderer, int x, int y, List<ClientTooltipComponent> components, @Nullable Identifier texture)`
- `void drawInsets(OwoUIGraphics self, int x, int y, int width, int height, Insets insets, int color)`
- `void drawInsets(OwoUIGraphics self, RenderPipeline pipeline, int x, int y, int width, int height, Insets insets, int color)`
  > Draw the area around the given rectangle which the given insets describe @param x      The x-coordinate of top-left corner of the rectangle @param y      The y-coordinate of top-left corner of the rectangle @param width  The width of the rectangle @param height The height of the rectangle @param insets The insets to draw around the rectangle @param color  The color to draw the inset area with
- `void drawInspector(OwoUIGraphics self, ParentUIComponent root, double mouseX, double mouseY, boolean onlyHovered)`
  > Draw the element inspector for the given tree, detailing the position, bounding box, margins and padding of each component @param root        The root component of the hierarchy to draw @param mouseX      The x-coordinate of the mouse pointer @param mouseY      The y-coordinate of the mouse pointer @param onlyHovered Whether to only draw the inspector for the hovered widget
- `boolean handleTextClick(Style style, Screen screenAfterRun)`

### `interface` PositionedRectangle

> Represents a rectangle positioned in 2D-space
>

**Methods:**

- `int x()`

### `class` Positioning

#### `enum` Type

> Position the component using whatever layout method the parent component wants to apply
>

**Methods:**

- `Positioning withX(int x)`
- `Positioning withY(int y)`
- `Positioning interpolate(Positioning next, float delta)`
- `Positioning absolute(int xPixels, int yPixels)`
  > Position the component at an absolute offset from the root of parent @param xPixels The offset on the x-axis @param yPixels The offset on the y-axis
- `Positioning relative(int xPercent, int yPercent)`
  > Position the component at a relative offset inside the parent. This respect the size of the component itself. As such: <ul>     <li>50,50 centers the component inside the parent</li>     <li>100,50 centers to component vertically and pushes it all the way to the right</li>     <li>100,100 pushes the component all the way into the bottom right corner of the parent</li> </ul> @param xPercent The offset on the x-axis @param yPercent The offset on the y-axis
- `Positioning across(int xPercent, int yPercent)`
  > Position the component the specified percentage across the parent, <i>not including the component's own size</i> @param xPercent The offset on the x-axis @param yPercent The offset on the y-axis
- `Positioning layout()`
  > Position the component using whatever layout method the parent component wants to apply
- `Positioning parse(Element positioningElement)`

### `record` Size

> Represents a two-dimensional value, used for describing position-less rectangles in 2D-space @param width  The width of the rectangle @param height The height of the rectangle
>

**Methods:**

- `Size of(int width, int height)`
- `Size square(int sideLength)`
- `Size zero()`
  > @return A size with both values equal to 0


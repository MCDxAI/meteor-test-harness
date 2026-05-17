## Package: `io.wispforest.owo.ui.component`

### `class` BoxComponent

> A colored rectangle either filled or outlined by a given color or gradient
>

#### `enum` GradientDirection

> @return The current end color of this component's gradient
>

**Methods:**

- `void update(float delta, int mouseX, int mouseY)`
- `void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta)`
- `BoxComponent fill(boolean fill)`
  > Set whether this component should be filled with color or outlined
- `boolean fill()`
  > @return {@code true} if this component is currently filled with color, {@code false} if it is outlined
- `BoxComponent direction(GradientDirection direction)`
  > Set the direction in which the gradient inside this component should travel
- `GradientDirection direction()`
  > @return The direction in which the gradient inside this component currently travels
- `BoxComponent color(Color color)`
  > Set the color of this component. Equivalent to calling both {@link #startColor(Color)} and {@link #endColor(Color)} @param color The start and end color of this              component's color gradient
- `BoxComponent startColor(Color startColor)`
  > Set the start color of this component's gradient
- `AnimatableProperty<Color> startColor()`
  > @return The current start color of this component's gradient
- `BoxComponent endColor(Color endColor)`
  > Set the end color of this component's gradient
- `AnimatableProperty<Color> endColor()`
  > @return The current end color of this component's gradient
- `void parseProperties(UIModel model, Element element, Map<String, Element> children)`

### `class` DropdownComponent

**Methods:**

- `DropdownComponent openContextMenu(Screen screen, R rootComponent, BiConsumer<R, DropdownComponent> mountFunction, double mouseX, double mouseY, Consumer<DropdownComponent> builder)`
  > Open a context menu at the given location in the given screen, adjusting the position if needed to avoid overflowing screen space @param screen        The screen on which to operate @param rootComponent The root component onto which to mount the dropdown @param mountFunction The mounting function to use @param mouseX        The x-coordinate at which to open the dropdown @param mouseY        The y-coordinate at which to open the dropdown @param builder       A function to add entries to the dropdown
- `ParentUIComponent surface(Surface surface)`
- `void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta)`
- `void layout(Size space)`
- `DropdownComponent divider()`
- `DropdownComponent text(Component text)`
- `DropdownComponent button(Component text, Consumer<DropdownComponent> onClick)`
- `DropdownComponent checkbox(Component text, boolean state, Consumer<Boolean> onClick)`
- `DropdownComponent nested(Component text, Sizing horizontalSizing, Consumer<DropdownComponent> builder)`
- `FlowLayout removeChild(UIComponent child)`
- `DropdownComponent closeWhenNotHovered(boolean closeWhenNotHovered)`
- `boolean closeWhenNotHovered()`
- `void parseProperties(UIModel model, Element element, Map<String, Element> children)`
- `void parseAndApplyEntries(Element container)`
- `void drawIconFromTexture(OwoUIGraphics context, ParentUIComponent dropdown, int y, int u, int v)`
- `void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta)`
- `void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta)`
- `int determineHorizontalContentSize(Sizing sizing)`
- `boolean onMouseDown(MouseButtonEvent click, boolean doubled)`
- `void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta)`
- `void playInteractionSound()`
- `void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta)`
- `int determineHorizontalContentSize(Sizing sizing)`
- `void playInteractionSound()`

### `class` ItemComponent

**Methods:**

- `int determineHorizontalContentSize(Sizing sizing)`
- `int determineVerticalContentSize(Sizing sizing)`
- `void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta)`
- `void updateTooltipForStack()`
- `ItemComponent stack(ItemStack stack)`
- `ItemStack stack()`
- `ItemComponent showOverlay(boolean drawOverlay)`
- `boolean showOverlay()`
- `List<ClientTooltipComponent> tooltipFromItem(ItemStack stack, Item.TooltipContext context, @Nullable Player player, @Nullable TooltipFlag type)`
  > Obtain the full item stack tooltip, including custom components provided via {@link net.minecraft.world.item.Item#getTooltipImage(ItemStack)} @param stack   The item stack from which to obtain the tooltip @param context the tooltip context @param player  The player to use for context, may be {@code null} @param type    The tooltip type - {@code null} to fall back to the default provided by                {@link net.minecraft.client.Options#advancedItemTooltips}
- `void parseProperties(UIModel model, Element element, Map<String, Element> children)`

### `class` SliderComponent

#### `interface` OnChanged

> @deprecated Use {@link #message(Function)} instead, as the message set by this method will be overwritten the next time this slider is moved
>

#### `interface` OnSlideEnd

> @deprecated Use {@link #message(Function)} instead, as the message set by this method will be overwritten the next time this slider is moved
>

**Methods:**

- `SliderComponent value(double value)`
- `double value()`
- `SliderComponent message(Function<String, Component> messageProvider)`
- `SliderComponent scrollStep(double scrollStep)`
- `double scrollStep()`
- `SliderComponent active(boolean active)`
- `boolean active()`
- `EventSource<OnChanged> onChanged()`
- `EventSource<OnSlideEnd> slideEnd()`
- `void updateMessage()`
- `void applyValue()`
- `boolean onMouseScroll(double mouseX, double mouseY, double amount)`
- `boolean onMouseUp(MouseButtonEvent click)`
- `boolean keyPressed(KeyEvent input)`
- `void parseProperties(UIModel model, Element element, Map<String, Element> children)`
- `void setMessage(Component message)`
  > @deprecated Use {@link #message(Function)} instead, as the message set by this method will be overwritten the next time this slider is moved

### `class` TextBoxComponent

**Methods:**

- `void setResponder(forRemoval = true)`
  > @deprecated Subscribe to {@link #onChanged()} instead
- `void drawFocusHighlight(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta)`
- `boolean keyPressed(KeyEvent input)`
- `void updateX(int x)`
- `void updateY(int y)`
- `EventSource<OnChanged> onChanged()`
- `TextBoxComponent text(String text)`
- `void parseProperties(UIModel spec, Element element, Map<String, Element> children)`

### `class` UIComponents

> Utility methods for creating UI components
>

**Methods:**

- `ButtonComponent button(Component message, Consumer<ButtonComponent> onPress)`
- `TextBoxComponent textBox(Sizing horizontalSizing)`
- `TextBoxComponent textBox(Sizing horizontalSizing, String text)`
- `TextAreaComponent textArea(Sizing horizontalSizing, Sizing verticalSizing)`
- `TextAreaComponent textArea(Sizing horizontalSizing, Sizing verticalSizing, String text)`
- `EntityComponent<E> entity(Sizing sizing, EntityType<E> type, @Nullable CompoundTag nbt)`
- `EntityComponent<E> entity(Sizing sizing, E entity)`
- `ItemComponent item(ItemStack item)`
- `BlockComponent block(BlockState state)`
- `BlockComponent block(BlockState state, BlockEntity blockEntity)`
- `BlockComponent block(BlockState state, @Nullable CompoundTag nbt)`
- `LabelComponent label(net.minecraft.network.chat.Component text)`
- `CheckboxComponent checkbox(net.minecraft.network.chat.Component message)`
- `SliderComponent slider(Sizing horizontalSizing)`
- `DiscreteSliderComponent discreteSlider(Sizing horizontalSizing, double min, double max)`
- `SpriteComponent sprite(Material spriteId)`
- `SpriteComponent sprite(TextureAtlasSprite sprite)`
- `TextureComponent texture(Identifier texture, int u, int v, int regionWidth, int regionHeight, int textureWidth, int textureHeight)`
- `TextureComponent texture(Identifier texture, int u, int v, int regionWidth, int regionHeight)`
- `BoxComponent box(Sizing horizontalSizing, Sizing verticalSizing)`
- `DropdownComponent dropdown(Sizing horizontalSizing)`
- `SlimSliderComponent slimSlider(SlimSliderComponent.Axis axis)`
- `SmallCheckboxComponent smallCheckbox(Component label)`
- `SpacerComponent spacer(int percent)`
- `SpacerComponent spacer()`
- `FlowLayout list(List<T> data, Consumer<FlowLayout> layoutConfigurator, Function<T, C> componentMaker, boolean vertical)`
- `VanillaWidgetComponent wrapVanillaWidget(AbstractWidget widget)`
- `T createWithSizing(Supplier<T> componentMaker, Sizing horizontalSizing, Sizing verticalSizing)`


## Package: `io.wispforest.owo.client.screens`

### `class` MenuUtils

> A collection of utilities to ease implementing a simple {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen}
>

**Methods:**

- `ItemStack handleSlotTransfer(AbstractContainerMenu menu, int clickedSlotIndex, int upperInventorySize)`
  > Can be used as an implementation of {@link net.minecraft.world.inventory.AbstractContainerMenu#quickMoveStack(Player, int)} for simple screens with a lower (player) and upper (main) inventory <pre> {@code @Override public ItemStack quickMove(PlayerEntity player, int invSlot) {     return MenuUtils.handleSlotTransfer(this, invSlot, this.inventory.size()); } } </pre> @param menu               The target AbstractContainerMenu @param clickedSlotIndex   The slot index that was clicked @param upperInventorySize The size of the upper (main) inventory @return The return value for {{@link net.minecraft.world.inventory.AbstractContainerMenu#quickMoveStack(Player, int)}}
- `boolean insertIntoSlotRange("BooleanMethodIsAlwaysInverted")`
  > Shorthand of {@link #insertIntoSlotRange(AbstractContainerMenu, ItemStack, int, int, boolean)} with {@code false} for {@code fromLast}
- `boolean insertIntoSlotRange("BooleanMethodIsAlwaysInverted")`
  > Tries to insert the {@code addition} stack into all slots in the given range @param menu       The AbstractContainerMenu to operate on @param beginIndex The index of the first slot to check @param endIndex   The index of the last slot to check @param addition   The ItemStack to try and insert, this gets mutated                   if insertion (partly) succeeds @param fromLast   If {@code true}, iterate the range of slots in                   opposite order @return {@code true} if state was modified

### `class` SlotGenerator

> Stateful slot generation utility for easily arranging the slot grid used in a {@link net.minecraft.world.inventory.AbstractContainerMenu}
>

#### `interface` SlotFactory

> Set the slot factory of this generator, used for instantiating each generated slot, to {@code slotFactory}
>

**Methods:**

- `SlotGenerator begin(Consumer<Slot> slotConsumer, int anchorX, int anchorY)`
  > Begin generating slots into {@code slotConsumer}, starting at ({@code anchorX}, {@code anchorY}). Usually, the {@code slotConsumer} will be the {@code addSlot} method of the screen handler for which slots are being generated <p> <pre> {@code SlotGenerator.begin(this::addSlot, 50, 10)     .grid(someInventory, 0, 3, 3) // add a 3x3 grid of slots 0-8 of 'someInventory'     .moveTo(10, 100)     .playerInventory(playerInventory); // add the player inventory and hotbar slots } </pre>
- `SlotGenerator moveTo(int anchorX, int anchorY)`
  > Move the top-left anchor point of generated grids to ({@code anchorX}, {@code anchorY})
- `SlotGenerator spacing(int spacing)`
  > Shorthand for calling both {@link #horizontalSpacing} and {@link #verticalSpacing} with {@code spacing}
- `SlotGenerator horizontalSpacing(int horizontalSpacing)`
- `SlotGenerator verticalSpacing(int verticalSpacing)`
- `SlotGenerator slotConsumer(Consumer<Slot> slotConsumer)`
- `SlotGenerator defaultSlotFactory()`
  > Reset the slot factory of this generator to the default {@link Slot#Slot(Container, int, int, int)} constructor
- `SlotGenerator slotFactory(SlotFactory slotFactory)`
  > Set the slot factory of this generator, used for instantiating each generated slot, to {@code slotFactory}
- `SlotGenerator grid(Container container, int startIndex, int width, int height)`
- `SlotGenerator playerInventory(Inventory playerInventory)`

### `class` ValidatingSlot

> A slot that uses the provided {@code insertCondition} to decide which items can be inserted
>

**Methods:**

- `boolean mayPlace(ItemStack stack)`

## Package: `io.wispforest.owo.client.texture`

### `class` AnimatedTextureDrawable

> A drawable that can draw an animated texture, very similar to how .mcmeta works on stitched textures in ticked atlases
>
> Originally from Animawid, adapted for oωo</p> @author Tempora @author glisco
>

**Methods:**

- `void render(int x, int y, GuiGraphics context, int mouseX, int mouseY, float delta)`
  > Renders this drawable at the given position. The position of this drawable is mutated non-temporarily
- `void render("IntegerDivisionInFloatingPointContext")`

### `record` SpriteSheetMetadata

> A simple container to define the sprite sheet an {@link AnimatedTextureDrawable} uses
>
> Originally from Animawid, adapted for oωo</p> @author Tempora @author glisco
>


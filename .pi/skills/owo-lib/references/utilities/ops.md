## Package: `io.wispforest.owo.ops`

### `class` ItemOps

> A collection of common checks and operations done on {@link ItemStack}
>

**Methods:**

- `boolean canStack(ItemStack base, ItemStack addition)`
  > Checks if stack one can stack onto stack two @param base     The base stack @param addition The stack to be added @return {@code true} if addition can stack onto base
- `boolean canIncrease(ItemStack stack)`
  > Checks if a stack can increase @param stack The stack to test @return stack.getCount() &lt; stack.getMaxCount()
- `boolean canIncreaseBy(ItemStack stack, int by)`
  > Checks if a stack can increase by the given amount @param stack The stack to test @param by    The amount to test for @return {@code true} if the stack can increase by the given amount
- `ItemStack singleCopy(ItemStack stack)`
  > Returns a copy of the given stack with count set to 1
- `boolean emptyAwareDecrement(ItemStack stack)`
  > Decrements the stack @param stack The stack to decrement @return {@code false} if the stack is empty after the operation
- `boolean emptyAwareDecrement(ItemStack stack, int amount)`
  > Decrements the stack @param stack  The stack to decrement @param amount The amount to decrement @return {@code false} if the stack is empty after the operation
- `boolean decrementPlayerHandItem(Player player, InteractionHand hand)`
  > Decrements the stack in the players hand and replaces it with {@link ItemStack#EMPTY} if the result would be an empty stack @param player The player to operate on @param hand   The hand to affect @return {@code false} if the stack is empty after the operation
- `boolean decrementPlayerHandItem(Player player, InteractionHand hand, int amount)`
  > Decrements the stack in the players hand and replaces it with {@link ItemStack#EMPTY} if the result would be an empty stack @param player The player to operate on @param hand   The hand to affect @param amount The amount to decrement @return {@code false} if the stack is empty after the operation

### `class` LevelOps

> A collection of common operations done on {@link Level}
>

**Methods:**

- `void breakBlockWithItem(Level level, BlockPos pos, ItemStack breakItem)`
  > Break the specified block with the given item @param level     The level the block is in @param pos       The position of the block to break @param breakItem The item to break the block with
- `void breakBlockWithItem(Level level, BlockPos pos, ItemStack breakItem, @Nullable Entity breakingEntity)`
  > Break the specified block with the given item @param level          The level the block is in @param pos            The position of the block to break @param breakItem      The item to break the block with @param breakingEntity The entity which is breaking the block
- `void playSound(Level level, Vec3 pos, SoundEvent sound, SoundSource category)`
  > Plays the provided sound at the provided location. This works on both client and server. Volume and pitch default to 1 @param level    The level to play the sound in @param pos      Where to play the sound @param sound    The sound to play @param category The category for the sound
- `void playSound(Level level, BlockPos pos, SoundEvent sound, SoundSource category)`
- `void playSound(Level level, Vec3 pos, SoundEvent sound, SoundSource category, float volume, float pitch)`
  > Plays the provided sound at the provided location. This works on both client and server @param level    The level to play the sound in @param pos      Where to play the sound @param sound    The sound to play @param category The category for the sound @param volume   The volume to play the sound at @param pitch    The pitch, or speed, to play the sound at
- `void playSound(Level level, BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch)`
- `void updateIfOnServer(Level level, BlockPos pos)`
  > Causes a block update at the given position, if {@code level} is an instance of {@link ServerLevel} @param level The target level @param pos   The target position
- `void teleportToLevel(ServerPlayer player, ServerLevel target, Vec3 pos)`
  > Same as {@link LevelOps#teleportToLevel(ServerPlayer, ServerLevel, Vec3, float, float)} but defaults to {@code 0} for {@code pitch} and {@code yaw}
- `void teleportToLevel(ServerPlayer player, ServerLevel target, Vec3 pos, float yaw, float pitch)`
  > Teleports the given player to the given world, syncing all the annoying data like experience and status effects that minecraft doesn't @param player The player to teleport @param target The level to teleport to @param pos    The target position @param yaw    The target yaw @param pitch  The target pitch

### `class` LootOps

> A simple utility class to make injecting simple items or ItemStacks into one or multiple LootTables a one-line operation
>

**Methods:**

- `void injectItem(ItemLike item, float chance, Identifier... targetTables)`
  > Injects a single item entry into the specified LootTable(s) @param item         The item to inject @param chance       The chance for the item to actually generate @param targetTables The LootTable(s) to inject into
- `void injectItemWithCount(ItemLike item, float chance, int min, int max, Identifier... targetTables)`
  > Injects an item entry into the specified LootTable(s), with a random count between {@code min} and {@code max} @param item         The item to inject @param chance       The chance for the item to actually generate @param min          The minimum amount of items to generate @param max          The maximum amount of items to generate @param targetTables The LootTable(s) to inject into
- `void injectItemStack(ItemStack stack, float chance, Identifier... targetTables)`
  > Injects a single ItemStack entry into the specified LootTable(s) @param stack        The ItemStack to inject @param chance       The chance for the ItemStack to actually generate @param targetTables The LootTable(s) to inject into
- `boolean anyMatch(Identifier target, Identifier... predicates)`
  > Test is {@code target} matches against any of the {@code predicates}. Used to easily target multiple LootTables @param target     The target identifier (this would be the current table) @param predicates The identifiers to test against (this would be the targeted tables) @return {@code true} if target matches any of the predicates
- `void registerListener()`

### `class` TextOps

> A collection of common operations for working with and stylizing {@link Component}
>

**Methods:**

- `MutableComponent concat(Component prefix, Component text)`
  > Appends the {@code text} onto the {@code prefix} without modifying the siblings of either one @param prefix The prefix @param text   The text to add onto the prefix @return The combined text
- `MutableComponent withColor(String text, int color)`
  > Creates a new {@link Component} with the specified color already applied @param text  The text to create @param color The color to use in {@code RRGGBB} format @return The colored text, specifically a {@link net.minecraft.network.chat.contents.PlainTextContents}
- `MutableComponent translateWithColor(String text, int color)`
  > Creates a new {@link Component} with the specified color already applied @param text  The text to create @param color The color to use in {@code RRGGBB} format @return The colored text, specifically a {@link TranslatableContents}
- `MutableComponent withFormatting(String text, ChatFormatting... formatting)`
  > Applies multiple {@link ChatFormatting}s to the given String, with each one after the first one beginning on a {@code §} symbol. The amount of {@code §} symbols must equal the amount of supplied formattings - 1 @param text       The text to format, with optional format delimiters @param formatting The formattings to apply @return The formatted text
- `MutableComponent withColor(String text, int... colors)`
  > Applies multiple colors to the given String, with each one after the first one beginning on a {@code §} symbol. The amount of {@code §} symbols must equal the amount of supplied colors - 1 @param text   The text to colorize, with optional color delimiters @param colors The colors to apply, in {@code RRGGBB} format @return The colorized text @see #color(ChatFormatting)
- `int width(Font renderer, Iterable<Component> texts)`
  > Determine the width of the given iterable of texts, which is defined as the width of the widest text in the iterable @param renderer The text renderer responsible for rendering                 the text later on @param texts    The texts to check @return The width of the widest text in the collection
- `int widthOrdered(Font renderer, Iterable<FormattedCharSequence> texts)`
  > Determine the width of the given iterable of texts, which is defined as the width of the widest text in the iterable @param renderer The text renderer responsible for rendering                 the text later on @param texts    The texts to check @return The width of the widest text in the collection
- `int color(ChatFormatting formatting)`
  > @return The color value associated with the given formatting in {@code RRGGBB} format, or {@code 0} if there is none


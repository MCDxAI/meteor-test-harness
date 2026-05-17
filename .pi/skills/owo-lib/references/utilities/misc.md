## Package: `io.wispforest.owo.blockentity`

### `class` LinearProcess

> Represents a process made of steps than can be executed tick by tick using a respective {@link LinearProcessExecutor}. This can, for example, be used on BlockEntities that perform rituals or similar activities that are made of consecutive steps.
>
> A process defines the pattern of steps and events that shall be followed, thus there is one (usually static) instance of it. You then create a new instance of {@link LinearProcessExecutor} using the {@link #createExecutor(Object)} method for each instance of your BlockEntity of whatever else if supposed to run it
>
> To create a new process, call {@link #LinearProcess(int)} with the length it should have. A process always has the same length. Then, in the constructor of each object that will use an executor, use {@link #createExecutor(Object)} to obtain an instance. This then has to be told whether it lives on the client or server using {@link #configureExecutor(LinearProcessExecutor, boolean)}. On a BlockEntity this can be achieved by overriding {@link net.minecraft.world.level.block.entity.BlockEntity#setLevel(Level)} and configuring after the super call using the provided world
>
> Steps and events should be added to process once, ideally in the {@code static} initializer block of the containing class. After the process is complete, call {@link #finish()} to prevent further changes @param <T> The type of object this process will be executed on,            a {@link net.minecraft.world.level.block.entity.BlockEntity} in most cases
>

**Methods:**

- `LinearProcessExecutor<T> createExecutor(T target)`
  > Creates a new executor for the given target object @param target The object the executor should operate on @return The created executor. This is not ready for use yet @see #configureExecutor(LinearProcessExecutor, boolean)
- `void configureExecutor(LinearProcessExecutor<T> executor, boolean client)`
  > Configures an executor to use either the server or client instructions @param executor The executor to configure @param client   {@code true} if the client instructions should be used
- `void addCommonStep(int when, int length, BiConsumer<LinearProcessExecutor<T>, T> executor)`
  > Adds a new step to this process on both client and server @param when     When the step should start @param length   How long it should last @param executor The code to be run each tick while the step is active
- `void addClientStep(int when, int length, BiConsumer<LinearProcessExecutor<T>, T> executor)`
  > @see #addCommonStep(int, int, BiConsumer)
- `void addServerStep(int when, int length, BiConsumer<LinearProcessExecutor<T>, T> executor)`
  > @see #addCommonStep(int, int, BiConsumer)
- `void addCommonEvent(int when, BiConsumer<LinearProcessExecutor<T>, T> executor)`
  > Adds an event that is executed once, on both client and server @param when     When the event should occur @param executor The code to be run on the given tick @see #addClientEvent(int, BiConsumer) @see #addServerEvent(int, BiConsumer)
- `void addClientEvent(int when, BiConsumer<LinearProcessExecutor<T>, T> executor)`
  > @see #addCommonEvent(int, BiConsumer)
- `void addServerEvent(int when, BiConsumer<LinearProcessExecutor<T>, T> executor)`
  > @see #addCommonEvent(int, BiConsumer)
- `void whenFinishedCommon(BiConsumer<LinearProcessExecutor<T>, T> executor)`
  > Defines code to be run when this process has successfully finished, on both client and server @param executor The code to be run @see #whenFinishedClient(BiConsumer) @see #whenFinishedServer(BiConsumer)
- `void whenFinishedServer(BiConsumer<LinearProcessExecutor<T>, T> executor)`
  > @see #whenFinishedCommon(BiConsumer)
- `void whenFinishedClient(BiConsumer<LinearProcessExecutor<T>, T> executor)`
  > @see #whenFinishedCommon(BiConsumer)
- `void onCancelledCommon(BiConsumer<LinearProcessExecutor<T>, T> executor)`
  > Defines code to be run on both client and server when this process is unexpectedly cancelled mid-execution, use this to clean up after you. @param executor The code to be run @see #onCancelledClient(BiConsumer) @see #onCancelledServer(BiConsumer)
- `void onCancelledServer(BiConsumer<LinearProcessExecutor<T>, T> executor)`
  > @see #onCancelledCommon(BiConsumer)
- `void onCancelledClient(BiConsumer<LinearProcessExecutor<T>, T> executor)`
  > @see #onCancelledCommon(BiConsumer)
- `void runConditionally(Predicate<LinearProcessExecutor<T>> condition)`
  > Defines a condition that has to be met every tick this process runs, otherwise it cancels itself @param condition The condition that should be satisfied during the entire                  process execution
- `void finish()`
  > Marks this process and completely built and ready for execution

### `class` LinearProcessExecutor

> A handler that executes the steps defined in a {@link LinearProcess}. Each object that is supposed to run the process needs an instance of this, and each instance of this refers back to the object it operates on @param <T> The type of object this executor operates on
>

#### `record` ProcessStep

> Restores the saved state of this executor @param targetTag The nbt to read state from
>

#### `class` Info

> Restores the saved state of this executor @param targetTag The nbt to read state from
>

**Methods:**

- `void configure(Int2ObjectMap<BiConsumer<LinearProcessExecutor<T>, T>> eventTable, Int2ObjectMap<ProcessStep<T>> processStepTable)`
- `void tick()`
- `boolean begin()`
  > Attempts to begin execution @return {@code true} if execution will start next tick, {@code false} if execution is already running
- `boolean running("BooleanMethodIsAlwaysInverted")`
  > @return {@code true} if this executor is currently running
- `int getProcessTick()`
  > @return The last processing tick this executor completed
- `T getTarget()`
  > @return The object this executor is operating on
- `boolean cancel()`
  > Attempts to instantly cancel execution @return {@code true} if execution was successfully cancelled, {@code false} if this executor was not running
- `void writeState(CompoundTag targetTag)`
  > Saves the state of this executor @param targetTag The nbt to write state into
- `void readState(CompoundTag targetTag)`
  > Restores the saved state of this executor @param targetTag The nbt to read state from
- `Info<T> createInfo(int index)`
- `Info<T> createInfo(int index, int tick)`
- `boolean tick(LinearProcessExecutor<T> target)`

## Table of Contents

- [EnumArgumentType](#enumargumenttype) — `io.wispforest.owo.command`

## Package: `io.wispforest.owo.command`

### `class` EnumArgumentType

> A simple implementation of {@link ArgumentType} that works with any {@code enum}. It is recommended to create one instance of this and use it both in the call to {@link net.minecraft.commands.Commands#argument(String, ArgumentType)} as well as for getting the supplied argument via {@link #get(CommandContext, String)} @param <T> The {@code enum} this instance can parse
>

**Methods:**

- `CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)`
- `T parse(StringReader reader)`

## Table of Contents

- [Owo](#owo) — `io.wispforest.owo`

## Package: `io.wispforest.owo`

### `class` Owo

**Fields:**

- `boolean DEBUG` — Whether oωo debug is enabled, this defaults to {@code true} in a development environment. To override that behavior, add the {@code -Dowo.debug=false} java argument

**Methods:**

- `void debugWarn(Logger logger, String message)`
- `void debugWarn(Logger logger, String message, Object... params)`
- `MinecraftServer currentServer()`
  > @return The currently active minecraft server instance. If running on a physical client, this will return the integrated server while in a local singleplayer world and {@code null} otherwise
- `Identifier id(String path)`


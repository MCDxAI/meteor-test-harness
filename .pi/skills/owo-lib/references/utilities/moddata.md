## Package: `io.wispforest.owo.moddata`

### `interface` ModDataConsumer

> A class that can accept some JSON data loaded from a subdirectory of all other mods' {@code data} directories when instructed to using {@link ModDataLoader#load(ModDataConsumer)}
>

### `class` ModDataLoader

> Contains the logic to load JSON from all other mods' data directories when {@link #load(ModDataConsumer)} is called. This should ideally be done one and in a {@link net.fabricmc.api.ModInitializer}
>

**Methods:**

- `void load(ModDataConsumer consumer)`
  > Loads the data the {@code consumer} requests @param consumer The consumer to load data for


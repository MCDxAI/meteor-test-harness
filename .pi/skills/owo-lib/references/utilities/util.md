## Package: `io.wispforest.owo.util`

### `interface` ImplementedContainer

> A simple {@code Inventory} implementation with only default methods + an item list getter.
>
> Originally by Juuz
>

### `class` KawaiiUtil

**Methods:**

- `String uwuify(String string)`
  > Prepends a randomly chosen Kaomoji to the given String @param string The string the process @return {@code string} with a random Kaomoji prepended
- `String uwuGen()`
  > @return A random Kaomoji

### `class` Maldenhagen

> A simple utility class for making ore blocks update after they are generated. This is especially useful for ores that are supposed to glow, as with the normal ore feature they won't do that since lighting is never calculated for them
>

**Methods:**

- `void injectCopium(Block block)`
  > Marks a block for update after generation @param block The block to update
- `boolean isOnCopium(Block block)`
  > @param block The block to test @return {@code true} if the block should update after generation

### `class` NumberReflection

**Methods:**

- `boolean isNumberType(Class<?> clazz)`
  > Determines whether the given class represents a number type @param clazz The class to test @return {@code true} if {@code clazz} is either a primitive number type or one the respective wrappers
- `boolean isFloatingPointType(Class<?> clazz)`
  > Determines whether the given class represents a floating point type @param clazz The class to test @return {@code true} if {@code clazz} is either a primitive floating point type or {@link Float} or {@link Double}
- `T convert("unchecked")`
  > Tries to convert the given number to {@code targetClass} by calling the corresponding {@code Number.<type>Value()} method @param in          The number to convert @param targetClass The target class, must be something which satisfies {@link #isNumberType(Class)} @return The input number, converted to the target type @throws IllegalArgumentException if either {@code targetClass} does not satisfy {@link #isNumberType(Class)}
- `T maxValue("unchecked")`
  > Tries to determine the maximum value supported by the number type which {@code numberType} represents @param numberType The target number type, must be something which satisfies {@link #isNumberType(Class)} @return The maximum value of the given number type @throws IllegalArgumentException if either {@code targetClass} does not satisfy {@link #isNumberType(Class)}
- `T minValue("unchecked")`
  > Tries to determine the minimum value supported by the number type which {@code numberType} represents @param numberType The target number type, must be something which satisfies {@link #isNumberType(Class)} @return The minimum value of the given number type @throws IllegalArgumentException if either {@code targetClass} does not satisfy {@link #isNumberType(Class)}

### `class` Observable

> A container which allows observing changes to its value. Every time the value is <i>changed</i>, i.e. {@code Objects.equals(value, newValue)} evaluates to {@code false}, all observers added via {@link #observe(Consumer)} will be notified and passed the new value @param <T> The type of object this observable holds @see #observeAll(Runnable, Observable[])
>

**Methods:**

- `Observable<T> of(T initial)`
  > Creates a new observable container with the given initial value
- `void observeAll(Runnable observer, Observable<?>... observables)`
  > Notify the given observer whenever <i>any</i> of the given observables are updated. Context-less version {@link #observeAll(Consumer, Observable[])} which allows observing multiple observables of different types @param observer    The observer to notify @param observables The list of observable to observe
- `void observeAll(Consumer<T> observer, Observable<T>... observables)`
  > Notify the given observer whenever <i>any</i> of the given observables are updated @param observer    The observer to notify @param observables The list of observable to observe
- `T get()`
  > @return The current value stored in this container
- `void set(T newValue)`
  > Change the value stored in this container to {@code newValue}. Observers will only be notified if {@code Objects.equals(value, newValue)} evaluates to {@code false} @param newValue The new value to store
- `void observe(Consumer<T> observer)`
  > Add an observer function to be run every time the value stored in this container changes
- `void notifyObservers(T value)`

### `class` OwoFreezer

> A simple utility for freezing services after mod initialization.
>

**Methods:**

- `void registerFreezeCallback(Runnable callback)`
  > Registers an on freeze callback. The callback will be called when services are frozen @param callback the callback to register
- `boolean isFrozen()`
  > @return {@code true} if services are frozen
- `void checkRegister(String pluralName)`
  > Shorthand for checking if services aren't frozen, and throwing if not. @param pluralName the plural of the service being registered (e.g. "Network channels") @throws ServicesFrozenException if services are frozen
- `void freeze()`

### `class` ReflectionUtils

#### `interface` FieldConsumer

> Determines the n-th type argument of the given type. If {@code type} is not a parameterized type, {@code null} is returned @param type  The type to query @param index The index of the type argument the retrieve @return The n-th type argument of {@code type} or {@code null} if {@code index} is out of bounds or the type argument is not a {@link Class}
>

**Methods:**

- `C tryInstantiateWithNoArgs(Class<C> clazz)`
  > Tries to instantiate the given class with a zero-args constructor call, throws a {@link RuntimeException} if it fails @param clazz The class to instantiate @param <C>   The type of object that results @return The created instance of <b>C</b>
- `C instantiate(Constructor<C> constructor, Object... args)`
  > Calls the {@link Constructor#newInstance(Object...)} method and wraps the exception in a {@link RuntimeException}, thus making it unchecked. <br> <b>Use this when you would otherwise rethrow</b> @param constructor The constructor to call @param args        The arguments to pass the constructor @param <C>         The type of object to create @return The created object
- `Constructor<C> getNoArgsConstructor(Class<C> clazz)`
  > Tries to obtain the public zero-args constructor of the given class. <b>Use this when no constructor constitutes an error condition or you previously checked for its existence with {@link #requireZeroArgsConstructor(Class, Function)}</b> @param clazz The class to get the constructor from @param <C>   The type of object the constructor will create @return The public zero-args constructor of the given class
- `void iterateAccessibleStaticFields("unchecked")`
  > Iterates all accessible static fields of the given class and calls the field consumer on each applicable one @param clazz           The target class @param targetFieldType The field type match @param fieldConsumer   The function to apply to each field, supplied                        with the field's value and ID @param <C>             The type of {@code clazz} @param <F>             The type of field to match
- `String getFieldName(Field field)`
  > Returns the name of field in all lowercase, or the name defined by an {@link AssignedName} annotation @param field The field to check @return the properly formatted field name
- `void forApplicableSubclasses(Class<?> parent, Class<?> targetType, Consumer<Class<?>> action)`
  > Executes the given consumer on all subclasses that match {@code targetType} @param parent     The parent class @param targetType The subclass type to match @param action     The action to execute on each subclass
- `void requireZeroArgsConstructor(Class<?> clazz, Function<String, String> reasonFormatter)`
  > Verifies that the given class provides a public zero-args constructor. Throws an exception with a caller-controlled message if the constructor doesn't exist @param clazz           The class to check the existence of a zero-args constructor for @param reasonFormatter The error message to throw, gets the class name passed
- `String getCallingClassName(int depth)`
  > Tries to acquire the name of the calling class, {@code depth} frames up the call stack @param depth How many frames upwards to walk the call stack @return The name of the class at {@code depth} in the call stack or {@code <unknown>} if the class name was not found

### `interface` Scary

> Annotations used to indicate that a given whatever is design in some manor that may or may not cause you pain. Combined it might scare your skeleton out of your body having it run down the block.
>

### `class` TagInjector

> A simple utility for inserting values into Tags at runtime
>

#### `record` TagLocation

> Inject the given tags into the given tag, effectively nesting them. This is equivalent to prefixing an entry in the tag JSON's {@code values} array with a {@code #} @param registry The registry the target tag is for @param tag      The identifier of the tag to inject into @param values   The values to inject
>

**Methods:**

- `void injectRaw(Registry<?> registry, Identifier tag, Function<Identifier, TagEntry> entryMaker, Collection<Identifier> values)`
  > Inject the given identifiers into the given tag <p> If any of the identifiers don't correspond to an entry in the given registry, you <i>will</i> break the tag. If the tag does not exist, it will be created. @param registry   The registry for which the injected tags should apply @param tag        The tag to insert into, this could contain all kinds of values @param entryMaker The function to use for creating tag entries from the given identifiers @param values     The values to insert
- `void injectRaw(Registry<?> registry, Identifier tag, Function<Identifier, TagEntry> entryMaker, Identifier... values)`
- `void inject(Registry<T> registry, Identifier tag, Collection<T> values)`
  > Inject the given values into the given tag, obtaining their identifiers from the given registry @param registry The registry the target tag is for @param tag      The identifier of the tag to inject into @param values   The values to inject @param <T>      The type of the target registry
- `void inject(Registry<T> registry, Identifier tag, T... values)`
- `void injectDirectReference(Registry<?> registry, Identifier tag, Collection<Identifier> values)`
  > Inject the given identifiers into the given tag @param registry The registry the target tag is for @param tag      The identifier of the tag to inject into @param values   The values to inject
- `void injectDirectReference(Registry<?> registry, Identifier tag, Identifier... values)`
- `void injectTagReference(Registry<?> registry, Identifier tag, Collection<Identifier> values)`
  > Inject the given tags into the given tag, effectively nesting them. This is equivalent to prefixing an entry in the tag JSON's {@code values} array with a {@code #} @param registry The registry the target tag is for @param tag      The identifier of the tag to inject into @param values   The values to inject
- `void injectTagReference(Registry<?> registry, Identifier tag, Identifier... values)`
- `record TagLocation(String type, Identifier tagId)`

### `class` VectorRandomUtils

> Utility class for getting random offsets within a {@link Level}
>

**Methods:**

- `Vec3 getRandomCenteredOnBlock(Level level, BlockPos pos, double deviation)`
  > Generates a random point centered on the given block @param level     The level to operate in @param pos       The block position to take the center from @param deviation The size of cube from which positions are picked @return A random point no further than {@code deviation} from the center of {@code pos}
- `Vec3 getRandomWithinBlock(Level level, BlockPos pos)`
  > Generates a random point within the given block @param level The level to operate in @param pos   The block in which to pick a point @return A random point somewhere within the bounding box of {@code pos}
- `Vec3 getRandomOffset(Level level, Vec3 center, double deviation)`
  > Generates a random point @param level     The level to operate in @param center    The center point @param deviation The size of cube from which positions are picked @return A random point within a cube with side length of {@code deviation} centered on {@code center}
- `Vec3 getRandomOffsetSpecific(Level level, Vec3 center, double deviationX, double deviationY, double deviationZ)`
  > Generates a random point offset from {@code center} @param level      The level to operate in @param center     The center position to start with @param deviationX The length of the selection cuboid on the x-axis @param deviationY The length of the selection cuboid on the y-axis @param deviationZ The length of the selection cuboid on the z-axis @return The generated point

### `class` VectorSerializer

> Utility class for reading and storing {@link Vec3} and {@link Vector3f} from and into {@link net.minecraft.nbt.CompoundTag}
>

**Methods:**

- `CompoundTag put(CompoundTag nbt, String key, Vec3 vec3d)`
  > Stores the given vector  as an array at the given key in the given nbt compound @param nbt   The nbt compound to serialize into @param key   The key to use @param vec3d The vector to serialize @return {@code nbt}
- `CompoundTag putf(CompoundTag nbt, String key, Vector3f vec3f)`
  > Stores the given vector  as an array at the given key in the given nbt compound @param vec3f The vector to serialize @param nbt   The nbt compound to serialize into @param key   The key to use @return {@code nbt}
- `CompoundTag puti(CompoundTag nbt, String key, Vec3i vec3i)`
  > Stores the given vector  as an array at the given key in the given nbt compound @param vec3i The vector to serialize @param nbt   The nbt compound to serialize into @param key   The key to use @return {@code nbt}
- `Vec3 get(CompoundTag nbt, String key)`
  > Gets the vector stored at the given key in the given nbt compound @param nbt The nbt compound to read from @param key The key the read from @return The deserialized vector
- `Vector3f getf(CompoundTag nbt, String key)`
  > Gets the vector stored at the given key in the given nbt compound @param nbt The nbt compound to read from @param key The key the read from @return The deserialized vector
- `Vec3i geti(CompoundTag nbt, String key)`
  > Gets the vector stored at the given key in the given nbt compound @param nbt The nbt compound to read from @param key The key the read from @return The deserialized vector
- `void write(FriendlyByteBuf buffer, Vec3 vec3d)`
  > Writes the given vector into the given packet buffer @param vec3d  The vector to write @param buffer The packet buffer to write into
- `void writef(FriendlyByteBuf buffer, Vector3f vec3f)`
  > Writes the given vector into the given packet buffer @param vec3f  The vector to write @param buffer The packet buffer to write into
- `void writei(FriendlyByteBuf buffer, Vec3i vec3i)`
  > Writes the given vector into the given packet buffer @param vec3i  The vector to write @param buffer The packet buffer to write into
- `Vec3 read(FriendlyByteBuf buffer)`
  > Reads one vector from the given packet buffer @param buffer The buffer to read from @return The deserialized vector
- `Vector3f readf(FriendlyByteBuf buffer)`
  > Reads one vector from the given packet buffer @param buffer The buffer to read from @return The deserialized vector
- `Vec3i readi(FriendlyByteBuf buffer)`
  > Reads one vector from the given packet buffer @param buffer The buffer to read from @return The deserialized vector


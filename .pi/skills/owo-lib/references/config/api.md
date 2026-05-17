## Table of Contents

- [ConfigWrapper](#configwrapper) — `io.wispforest.owo.config`
- [Option](#option) — `io.wispforest.owo.config`
- [Key](#key) — `io.wispforest.owo.config`
- [ConfigScreen](#configscreen) — `io.wispforest.owo.config.ui`
- [ConfigScreenProviders](#configscreenproviders) — `io.wispforest.owo.config.ui`
- [OptionComponentFactory](#optioncomponentfactory) — `io.wispforest.owo.config.ui`

## Package: `io.wispforest.owo.config`

### `class` ConfigWrapper

> The common base class of all generated config classes. The majority of all config functionality resides in here
>
> Do not extend this class yourself - instead annotate a class describing your config model with {@link Config}, just as you would do with other libraries like Cloth Config @see Config
>

**Methods:**

- `void save()`
  > Save the config represented by this wrapper
- `void load({"unchecked"})`
  > Load the config represented by this wrapper from its associated file, or create it if it does not exist
- `String name()`
  > @return The name of this config, used for translation keys and the filename
- `Path fileLocation()`
  > @return The location to which this config is saved
- `void forEachOption(Consumer<Option<?>> action)`
  > Execute the given action once for each option in this config
- `record Constraint({"rawtypes", "unchecked"})`
- `boolean test(Object value)`
- `record SerializationBuilder(Jankson.Builder janksonBuilder, ReflectiveEndecBuilder endecBuilder)`
- `SerializationBuilder addEndec(Class<T> clazz, Endec<T> endec)`

### `class` Option

> Describes a single option in a config. Instances of this class keep a reference to the field in the model class which stores the value used for serialization.
>
> An option may enter the so-called "detached" state, which means its value is being overridden by the server. In this state, the option is completely immutable and can only be changed again afterwards
>

#### `class` of

> @return The current value of this option
>

#### `enum` SyncMode

> @return The way in which this option should be synchronized between sever and client
>

#### `record` Key

> Describes an option's location inside a config, generated from its name a potential parents it is nested in @param path The segments of the path making up this key
>

#### `record` BoundField

> A simple container which stores both a non-static field and an instance of the containing class on which to query values @param owner The owner object which holds the value              the field points to @param field The field itself @param <T>   The type of object this field stores
>

**Methods:**

- `void set(T value)`
  > Update the current value of this option, or do nothing if the given value is invalid @param value The new value of the option
- `T value()`
  > @return The current value of this option
- `Class<T> clazz()`
  > @return The class of this option's value
- `void synchronizeWithBackingField()`
  > Synchronize the value stored in the backing field and this option's mirror - used for either correcting an invalid value after updating the field or updating the mirror
- `boolean verifyConstraint(T value)`
  > Check whether the given value passes the constraint of this option and emit a warning if it does not @param value The value to test @return {@code true} if either the given value passes the constraint put on this option or this option is unconstrained
- `void observe(Consumer<T> observer)`
  > Add an observer function to be run every time the value of this option changes
- `String translationKey()`
  > @return The translation key of this option
- `String configName()`
  > @return The name of the config this option is contained in
- `Key key()`
  > @return The key of this option
- `T defaultValue()`
  > @return The default value of this option
- `BoundField<T> backingField()`
  > @return The field which is backing this option, used for serialization as well as storing the client's value while the option is detached
- `boolean detached()`
  > @return {@code true} if this option is currently detached
- `SyncMode syncMode()`
  > @return The way in which this option should be synchronized between sever and client
- `record Key(String[] path)`
  > Describes an option's location inside a config, generated from its name a potential parents it is nested in @param path The segments of the path making up this key
- `Key parent()`
  > @return The immediate parent of this key, or {@link #ROOT} if the parent is the root key
- `Key child(String childName)`
  > Create the key for a child of this key @param childName The name of the child
- `String asString()`
  > @return The segments of this key joined with {@code .}
- `String name()`
  > @return The name of the element this key describes, without any of its parents
- `boolean isRoot()`
  > @return {@code true} if and only if this key is reference-equal to {@link #ROOT}
- `boolean hasAnnotation(Class<? extends Annotation> annotationClass)`

### `record` Key

> Describes an option's location inside a config, generated from its name a potential parents it is nested in @param path The segments of the path making up this key
>

**Methods:**

- `Key parent()`
  > @return The immediate parent of this key, or {@link #ROOT} if the parent is the root key
- `Key child(String childName)`
  > Create the key for a child of this key @param childName The name of the child
- `String asString()`
  > @return The segments of this key joined with {@code .}
- `String name()`
  > @return The name of the element this key describes, without any of its parents
- `boolean isRoot()`
  > @return {@code true} if and only if this key is reference-equal to {@link #ROOT}

## Package: `io.wispforest.owo.config.ui`

### `class` ConfigScreen

> A screen which generates components for each option in the provided config. The general structure of the screen is determined by the XML config model it uses - the default one is located at {@code assets/owo/owo_ui/config.xml}. Changing which model is used via {@link #createWithCustomModel(Identifier, ConfigWrapper, Screen)} can often be enough to visually customize the generated screen - should you need custom functionality however, extending this class is usually your best bet @see io.wispforest.owo.config.annotation.Modmenu @see ConfigWrapper
>

**Methods:**

- `ConfigScreen create(ConfigWrapper<?> config, @Nullable Screen parent)`
  > Create a config screen with the default model ({@code owo:config}) @param config The config to create a screen for @param parent The parent screen to return to               when the created screen is closed
- `ConfigScreen createWithCustomModel(Identifier modelId, ConfigWrapper<?> config, @Nullable Screen parent)`
  > Create a config screen with a custom model located in your mod's assets @param modelId The ID of the model to use @param config  The config to create a screen for @param parent  The parent screen to return to                when the created screen is closed
- `void build({"ConstantConditions", "unchecked"})`
- `void appendSection(Map<UIComponent, Component> sections, Field field, FlowLayout container)`
- `List<SearchAnchorComponent> collectSearchAnchors(ParentUIComponent root)`
- `boolean keyPressed(KeyEvent input)`
- `void onClose("unchecked")`
- `void removed("unchecked")`
- `record SearchMatches(String query, List<SearchAnchorComponent> matches)`
- `void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta)`
- `void update(float delta, int mouseX, int mouseY)`

### `class` ConfigScreenProviders

**Methods:**

- `void register(String modId, Function<Screen, S> supplier)`
  > Register the given config screen provider. This is primarily used for making a config screen available in ModMenu and to the {@code /owo-config} command, although other places my use it as well @param modId    The mod id for which to supply a config screen @param supplier The supplier to register - this gets the parent screen                 as argument @throws IllegalArgumentException If a config screen provider is                                  already registered for the given mod id
- `void forEach(BiConsumer<String, Function<Screen, ? extends Screen>> action)`

### `interface` OptionComponentFactory

> A function which creates an instance of {@link OptionValueProvider} fitting for the given config option. Whatever component is created should accurately reflect if the option is currently detached and thus immutable - ideally it is non-interactable @param <T> The type of option for which this factory can create components
>


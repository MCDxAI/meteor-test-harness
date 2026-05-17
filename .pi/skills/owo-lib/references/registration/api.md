## API Reference

## Table of Contents

- [ComplexRegistryAction](#complexregistryaction) — `io.wispforest.owo.registration`
- [Builder](#builder) — `io.wispforest.owo.registration`
- [RegistryHelper](#registryhelper) — `io.wispforest.owo.registration`
- [AutoRegistryContainer](#autoregistrycontainer) — `io.wispforest.owo.registration.reflect`
- [FieldProcessingSubject](#fieldprocessingsubject) — `io.wispforest.owo.registration.reflect`
- [FieldRegistrationHandler](#fieldregistrationhandler) — `io.wispforest.owo.registration.reflect`
- [SimpleFieldProcessingSubject](#simplefieldprocessingsubject) — `io.wispforest.owo.registration.reflect`

## Package: `io.wispforest.owo.registration`

### `class` ComplexRegistryAction

> An action to be executed by a {@link RegistryHelper} if and only if all of it's required entries are present in that helper's registry @see ComplexRegistryAction.Builder#create(Runnable)
>

**Methods:**

- `boolean preCheck(Registry<T> registry)`
- `boolean update(Identifier id, Collection<Runnable> actionList)`
- `Builder create(Runnable action)`
  > Creates a new builder to link the provided action to a list of identifiers @param action The action to run once all identifiers are found in the targeted registry @see #entry(Identifier) @see #entries(Collection)
- `Builder entry(Identifier id)`
- `Builder entries(Collection<Identifier> ids)`
- `ComplexRegistryAction build()`
  > Creates a registry action that can get run by a {@link RegistryHelper} once all the entries added via this builder are found in the target registry @return The built action

### `class` Builder

**Methods:**

- `Builder create(Runnable action)`
  > Creates a new builder to link the provided action to a list of identifiers @param action The action to run once all identifiers are found in the targeted registry @see #entry(Identifier) @see #entries(Collection)
- `Builder entry(Identifier id)`
- `Builder entries(Collection<Identifier> ids)`
- `ComplexRegistryAction build()`
  > Creates a registry action that can get run by a {@link RegistryHelper} once all the entries added via this builder are found in the target registry @return The built action

### `class` RegistryHelper

> A simple helper to run code conditionally based on whether certain registry entries are present or not. Use {@link #get(Registry)} to obtain the instance for a given registry
>

**Methods:**

- `RegistryHelper<T> get("unchecked")`
  > Gets the {@link RegistryHelper} instance for the provided registry @param registry The target registry @return The helper for the targeted registry
- `void runWhenPresent(Identifier id, Consumer<T> action)`
  > Runs the given consumer supplied with the registered object as soon as the requested ID exists in the registry @param id     The ID the registry must contain for {@code action} to be run @param action The code to run once {@code id} is present
- `void runWhenPresent(ComplexRegistryAction action)`
  > Runs the given action once all of its required entries are present in the registry @param action The {@link ComplexRegistryAction} to run or queue

## Package: `io.wispforest.owo.registration.reflect`

### `interface` AutoRegistryContainer

> A special version of {@link FieldProcessingSubject} that contains fields which should be registered into a {@link Registry} using the field names in lowercase as ID
>
> Use {@link #register(Class, String, boolean)} to automatically register all fields of a given implementation into its specified registry @param <T> The type of objects to register, same as the Registry's type parameter
>

### `interface` FieldProcessingSubject

> A class that can have its accessible static fields that match the class of <b>T</b> processed by the {@link FieldRegistrationHandler}
>
> <b>All implementations must provide a zero-args constructor</b> @param <T> The type of field to be processed
>

### `class` FieldRegistrationHandler

**Methods:**

- `void process(Class<? extends FieldProcessingSubject<T>> clazz, ReflectionUtils.FieldConsumer<T> processor, boolean recurseIntoInnerClasses)`
  > Applies the given processor to all applicable fields of the targeted class @param clazz                   The class to target, must implement {@link FieldProcessingSubject} @param processor               The function to apply to each applicable field @param recurseIntoInnerClasses Whether this method should recursively process all inner classes of {@code clazz} @param <T>                     The type of field to match
- `void processSimple(Class<? extends SimpleFieldProcessingSubject<T>> clazz, boolean recurseIntoInnerClasses)`
  > Processes all fields of the given class with the implementation of {@code processField(T, String)} it provides @param clazz                   The class to target, must implement {@link SimpleFieldProcessingSubject} @param recurseIntoInnerClasses Whether this method should recursively process all inner classes of {@code clazz} @param <T>                     The type of field to match
- `void register(Class<? extends AutoRegistryContainer<T>> clazz, String namespace, boolean recurseIntoInnerClasses)`
  > Registers all public static fields of the specified class that match its type parameter into the registry it specifies @param clazz     The class from which to take the fields, must implement {@link AutoRegistryContainer} @param namespace The namespace to use in the generated identifiers @param <T>       The type of object to register

### `interface` SimpleFieldProcessingSubject

> A simpler to use version of {@link FieldProcessingSubject} that provides the processor to apply to its fields @param <T>
>


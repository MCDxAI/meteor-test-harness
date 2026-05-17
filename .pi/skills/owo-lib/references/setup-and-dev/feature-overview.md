### Global API

oωo exposes the global `RenderDoc` API class that wraps all relevant RenderDoc API interfaces in a type-safe manner. You can use it to configure the overlay, set up the hotkeys for capturing and change RenderDoc's capture configuration.

Some methods of particular interest include: 

 - `startFrameCapture()` and `endFrameCapture()`, which you can use to capture only the draw calls between these method invocations

 - `getCapture(int index)` which you can use to obtain information about a past capture

 - `launchReplayUI(boolean connect)` and `showReplayUI()` which open a new replay UI window or bring an existing one to the front

and most importantly `isAvailable()`, which reports whether the RenderDoc dynamic library is loaded and oωo could successfully connect to the API

## Registration

oωo offers a flexible system for automatically registering a class' fields into `Registry` instances. It is very quick to implement and usually eliminates a lot of boilerplate. At the same time though, it is extremely flexible and allows significant customization and complete control over how the fields are processed using the `FieldProcessingSuject` API tree.

## Networking 

oωo provides a fully-featured network serialization system. Built around `OwoNetChannel` and the `Endec` framework, packet data is automatically (de-)serialized and the corresponding handlers invoked without the need of keeping track of Identifiers or channel associations - everything is derived from the data class. Packet contents are defined as Java `record`s, making it highly ergonomic. The serialization backend fully supports serializers for custom types and, in development, all channel-layout related data is synced between client and server making it so you cannot forget to register a handler on one side.

## Item Groups

The Item Group API allows you to create an Item Group with custom textures and sub-tabs in very few lines of code. It is still pretty open to customization and can make custom buttons for linking to your mod's project pages or even executing custom actions relevant to your mod.

## Particle Effects

oωo provides two main systems for handling particles. Primarily there's a client-sided set of utility methods for displaying multiple particles in the world, including drawing lines and cubes, randomized velocity and other general utility. To then allow easily composing and triggering these methods from the server, the `ParticleSystem` API, built on top of oωo's networking stack, can be used.

## Debug/Dev Features

When in a development environment, oωo's debug mode is automatically enabled which adds a host of features like commands for damaging/healing the player or dumping information about game objects, automatically **disabled weather and daylight cycle** and a few more.

## Moddata

The `ModDataLoader` and related API are designed to allow loading JSON-formatted data from the `mod` directory of all present mods. This makes it very easy to utilize datapack-like files for data that needs to be available before a world is loaded. For the purposes of modpack customization, the files are loaded from `.minecraft/moddata` as a replacement for datapacks.

## Screen Utilities

oωo contains a few very simple helpers to facilitate the creation of a simple `HandledScreen` and `ScreenHandler`, namely a type of slot that only allows certain items as well as methods for generating the player inventory slots required for almost all screens.

## NBT Handling

The `NbtKey` class is a simple serialization wrapper that allows inserting and extracting data values from `NbtCompound` instances. It essentially wraps the `get(...)` and `put(...)` methods to eliminate magic strings and centralize serialization code.

## Tag Injection

The `TagInjector` system allows you to inject entries into tags at runtime. There are numerous helper methods available for easily injecting blocks and items, but the barebones inject instructions are exposed as well to enable injection into Tags of arbitrary registries.

## Offline Data Access

The `OfflineDataLookup` and `OfflineAdvancementLookup` interfaces enable easily querying and/or modifying the NBT and Advancement data of offline players. As everything in oωo, the API surface is non-verbose to use and usually does not require more than a single method call.

## UI Framework

owo-ui is a declarative UI framework that helps with building dynamic screens quickly and easily. It strives to be highly embeddable, performant and, most of all, super easy to use. More information pertaining to it features and capabilities can be found within UI section

## Configuration

oωo provides a highly flexible annotation-driven configuration system. It aims to be simple yet powerful and offers a wide range of customizability and features as discussed within the Config section

## Endec

[endec](https://github.com/wisp-forest/endec) is a format-agnostic serialization framework inspired by Rust's [serde](https://serde.rs) library and the Codec API from Mojang's [DataFixerUpper](https://github.com/mojang/datafixerupper). More information on its features and implementation details can be found within the Endec Section

## API Reference

## Table of Contents

- [RenderDoc](#renderdoc) — `io.wispforest.owo.renderdoc`

## Package: `io.wispforest.owo.renderdoc`

### `class` RenderDoc

#### `class` CaptureOption

> Set the comments attached to a specific capture @param capture  The capture to modify, obtain with {@link #getCapture(int)} @param comments The new capture comments
>

**Methods:**

- `boolean isAvailable()`
  > @return {@code true} if the RenderDoc dynamic library is loaded and owo has successfully connected to the API
- `String getAPIVersion()`
  > @return The version of the RenderDoc API that owo is connected to, in &lt;major&gt;.&lt;minor&gt;.&lt;patch&gt; semver format
- `boolean setCaptureOption(CaptureOption<T> option, T value)`
  > Set the value of a RenderDoc capture option @param option The option to modify @param value  The value to change the option to @return {@code true} if the value was correct and the option was successfully modified
- `T getCaptureOption("unchecked")`
  > Get the value of a RenderDoc capture option @param option The option to query @return The current value of the option
- `void setCaptureKeys(Key... keys)`
  > Set the hotkeys used to trigger a capture
- `EnumSet<OverlayOption> getOverlayOptions()`
  > Query the current configuration of the RenderDoc overlay @return All parts of the overlay which are currently enabled
- `void enableOverlayOptions(OverlayOption... options)`
  > Enable some parts of the RenderDoc overlay @param options The options to enable
- `void disableOverlayOptions(OverlayOption... options)`
  > Disable some parts of the RenderDoc overlay @param options The options to enable
- `void removeHooks()`
  > Try to remove all RenderDoc hooks from the process. If this is called after a graphics API has been initialized, behavior is undefined
- `void unloadCrashHandler()`
  > Remove RenderDoc's crash handler from the process
- `void setCaptureFilePathTemplate(String template)`
  > Set the template used to generate new capture file names
- `String getCaptureFilePathTemplate()`
  > @return the template used to generate new capture file names
- `Capture getCapture(int index)`
  > Query information about a specific capture @param index The index to query @return The path and timestamp of the capture at the given index, or {@code null} if no such capture exists
- `int getNumCaptures()`
  > @return How many captures have been made
- `void triggerCapture()`
  > Trigger a capture of the next frame, as if the user had pressed on the capture hotkeys
- `void startFrameCapture()`
  > Immediately begin a capture
- `boolean isFrameCapturing()`
  > @return {@code true} if a capture is currently being performed
- `void endFrameCapture()`
  > Immediately end an active capture
- `boolean isReplayUIConnected()`
  > @return {@code true} if a RenderDoc replay UI instance is currently attached to this process
- `int launchReplayUI(boolean connect)`
  > Open the RenderDoc replay UI @param connect {@code true} if the new UI instance should instantly                attach to this process @return The PID of the spawned process, or {@code 0} if the UI could not be opened
- `boolean showReplayUI()`
  > Request the currently connected replay UI to raise its window to the top - this is not guaranteed to work on every OS @return {@code true} if the UI tried to raise its window, {@code false} if some error occurred while passing on the command or no UI is connected
- `void setCaptureComments(Capture capture, String comments)`
  > Set the comments attached to a specific capture @param capture  The capture to modify, obtain with {@link #getCapture(int)} @param comments The new capture comments
- `record Capture(String path, Instant timestamp)`


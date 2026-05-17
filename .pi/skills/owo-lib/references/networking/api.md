## API Reference

## Table of Contents

- [OwoNetChannel](#owonetchannel) — `io.wispforest.owo.network`
- [ClientHandle](#clienthandle) — `io.wispforest.owo.network`
- [ServerHandle](#serverhandle) — `io.wispforest.owo.network`
- [EnvironmentAccess](#environmentaccess) — `io.wispforest.owo.network`

## Package: `io.wispforest.owo.network`

### `class` OwoNetChannel

> An efficient networking abstraction that uses {@code record}s to store and define packet data. Serialization for most types is fully automatic and no custom handling needs to be done.
>
> Should one of your record components be of an unsupported type, either use {@link io.wispforest.endec.impl.ReflectiveEndecBuilder#register(Endec, Class)} to register an appropriate endec, or supply it directly using {@link #registerClientbound(Class, StructEndec, ChannelHandler)} and {@link #registerServerbound(Class, StructEndec, ChannelHandler)}
>
> To define a packet class suited for use with this wrapper, simply create a standard Java {@code record} class and put the desired data into the record header.
>
> To register a packet onto this channel, use either {@link #registerClientbound(Class, ChannelHandler)} or {@link #registerServerbound(Class, ChannelHandler)}, depending on which direction the packet goes. Bidirectional registration of the same class is explicitly supported. <b>For synchronization purposes, all registration must happen on both client and server, even for clientbound packets. Otherwise, joining the server will fail with a handshake error</b>
>
> To send a packet, use any of the {@code handle} methods to obtain a handle for sending. These are named after where the packet is sent <i>from</i>, meaning the {@link #clientHandle()} is used for sending <i>to the server</i> and vice-versa.
>
> The registered packet handlers are executed synchronously on the target environment's game thread instead of Netty's event loops - there is no need to call {@code .execute(...)}
>

#### `class` ServerHandle

> Sends the given messages to the server @param messages The messages to send
>

#### `interface` ChannelHandler

> Sends the given messages to the configured target(s) <b>Resets the target(s) after sending - this cannot be used multiple times on the same handle</b> @param messages The messages to send
>

#### `interface` EnvironmentAccess

> A simple wrapper that provides access to the environment a packet is being received / message is being handled in @param <P> The type of player to receive the packet @param <R> The runtime that the packet is being received in @param <N> The network handler that received the packet
>

**Methods:**

- `OwoNetChannel create(Identifier id)`
  > Creates a new required channel with given ID. Duplicate channel IDs are not allowed - if there is a collision, the name of the class that previously registered the channel will be part of the exception. <b>This may be called at any stage during mod initialization</b> @param id The desired channel ID @return The created channel
- `OwoNetChannel createOptional(Identifier id)`
  > Creates a new optional channel with given ID. Duplicate channel IDs are not allowed - if there is a collision, the name of the class that previously registered the channel will be part of the exception. <b>This may be called at any stage during mod initialization</b> @param id The desired channel ID @return The created channel
- `OwoNetChannel addEndecs(Consumer<ReflectiveEndecBuilder> endecBuilder)`
- `ReflectiveEndecBuilder builder()`
- `void registerClientbound(Class<R> messageClass, ChannelHandler<R, ClientAccess> handler)`
  > Registers a handler <i>on the client</i> for the specified message class. This also ensures the required endec is available. If an exception about a missing endec is thrown, register one @param messageClass The type of packet data to send and serialize @param handler      The handler that will receive the deserialized @see #serverHandle(Player) @see #serverHandle(MinecraftServer) @see #serverHandle(ServerLevel, BlockPos)
- `void registerClientboundDeferred(Class<R> messageClass)`
  > Registers a message class <i>on the client</i> with deferred handler registration. This also ensures the required endec is available. If an exception about a missing endec is thrown, register one @param messageClass The type of packet data to send and serialize @see #serverHandle(Player) @see #serverHandle(MinecraftServer) @see #serverHandle(ServerLevel, BlockPos)
- `void registerServerbound(Class<R> messageClass, ChannelHandler<R, ServerAccess> handler)`
  > Registers a handler <i>on the server</i> for the specified message class. This also ensures the required endec is available. If an exception about a missing endec is thrown, register one @param messageClass The type of packet data to send and serialize @param handler      The handler that will receive the deserialized @see #clientHandle()
- `void registerClientbound(Class<R> messageClass, StructEndec<R> endec, ChannelHandler<R, ClientAccess> handler)`
  > Registers a handler <i>on the client</i> for the specified message class @param messageClass The type of packet data to send and serialize @param endec        The endec to serialize messages with @param handler      The handler that will receive the deserialized @see #serverHandle(Player) @see #serverHandle(MinecraftServer) @see #serverHandle(ServerLevel, BlockPos)
- `void registerClientboundDeferred(Class<R> messageClass, StructEndec<R> endec)`
  > Registers a message class <i>on the client</i> with deferred handler registration @param messageClass The type of packet data to send and serialize @param endec        The endec to serialize messages with @see #serverHandle(Player) @see #serverHandle(MinecraftServer) @see #serverHandle(ServerLevel, BlockPos)
- `void registerServerbound(Class<R> messageClass, StructEndec<R> endec, ChannelHandler<R, ServerAccess> handler)`
  > Registers a handler <i>on the server</i> for the specified message class @param messageClass The type of packet data to send and serialize @param endec        The endec to serialize messages with @param handler      The handler that will receive the deserialized @see #clientHandle()
- `boolean canSendToPlayer(ServerPlayer player)`
- `boolean canSendToPlayer(ServerGamePacketListenerImpl networkHandler)`
- `boolean canSendToServer(EnvType.CLIENT)`
- `ClientHandle clientHandle()`
  > Obtains the client handle of this channel, used to send packets <i>to the server</i> @return The client handle of this channel
- `ServerHandle serverHandle(MinecraftServer server)`
  > Obtains a server handle used to send packets <i>to all players on the given server</i> <p> <b>This handle will be reused - do not retain references</b> @param server The server to target @return A server handle configured for sending packets to all players on the given server
- `ServerHandle serverHandle(Collection<ServerPlayer> targets)`
  > Obtains a server handle used to send packets <i>to all given players</i>. Use {@link PlayerLookup} to obtain the required collections <p> <b>This handle will be reused - do not retain references</b> @param targets The players to target @return A server handle configured for sending packets to all players in the given collection @see PlayerLookup
- `ServerHandle serverHandle(Player player)`
  > Obtains a server handle used to send packets <i>to the given player only</i> <p> <b>This handle will be reused - do not retain references</b> @param player The player to target @return A server handle configured for sending packets to the given player only
- `ServerHandle serverHandle(BlockEntity entity)`
  > Obtains a server handle used to send packets <i>to all players tracking the given block entity</i> <p> <b>This handle will be reused - do not retain references</b> @param entity The block entity to look up trackers for @return A server handle configured for sending packets to all players tracking the given block entity
- `ServerHandle serverHandle(ServerLevel world, BlockPos pos)`
  > Obtains a server handle used to send packets <i>to all players tracking the given position in the given world</i> <p> <b>This handle will be reused - do not retain references</b> @param world The world to look up players in @param pos   The position to look up trackers for @return A server handle configured for sending packets to all players tracking the given position in the given world
- `void send(R message)`
  > Sends the given message to the server @param message The message to send @see #send(Record[])
- `void send(R... messages)`
  > Sends the given messages to the server @param messages The messages to send
- `void send(R message)`
  > Sends the given message to the configured target(s) <b>Resets the target(s) after sending - this cannot be used for multiple messages on the same handle</b> @param message The message to send @see #send(Record[])
- `void send(R... messages)`
  > Sends the given messages to the configured target(s) <b>Resets the target(s) after sending - this cannot be used multiple times on the same handle</b> @param messages The messages to send
- `IndexedEndec<R> create(Class<R> rClass, StructEndec<R> endec, int index, EnvType target)`
- `int handlerIndex(EnvType target)`
- `Type<? extends CustomPacketPayload> type()`

### `class` ClientHandle

**Methods:**

- `void send(R message)`
  > Sends the given message to the server @param message The message to send @see #send(Record[])
- `void send(R... messages)`
  > Sends the given messages to the server @param messages The messages to send

### `class` ServerHandle

**Methods:**

- `void send(R message)`
  > Sends the given message to the configured target(s) <b>Resets the target(s) after sending - this cannot be used for multiple messages on the same handle</b> @param message The message to send @see #send(Record[])
- `void send(R... messages)`
  > Sends the given messages to the configured target(s) <b>Resets the target(s) after sending - this cannot be used multiple times on the same handle</b> @param messages The messages to send

### `interface` EnvironmentAccess

> A simple wrapper that provides access to the environment a packet is being received / message is being handled in @param <P> The type of player to receive the packet @param <R> The runtime that the packet is being received in @param <N> The network handler that received the packet
>


# Particles

## Table of Contents

  - [API Reference](#api-reference)
  - [Table of Contents](#table-of-contents)
  - [Package: `io.wispforest.owo.particles`](#package)
    - [`class` ClientParticles](#clientparticles)
  - [Package: `io.wispforest.owo.particles.systems`](#package)
    - [`class` ParticleSystem](#particlesystem)
    - [`class` ParticleSystemController](#particlesystemcontroller)

owo provides two systems for particles: client-sided utility methods for spawning particle effects (lines, cubes, randomized velocity), and the `ParticleSystem` API built on owo's networking stack for triggering effects from the server.

## API Reference

## Table of Contents

- [ClientParticles](#clientparticles) — `io.wispforest.owo.particles`
- [ParticleSystem](#particlesystem) — `io.wispforest.owo.particles.systems`
- [ParticleSystemController](#particlesystemcontroller) — `io.wispforest.owo.particles.systems`

## Package: `io.wispforest.owo.particles`

### `class` ClientParticles

**Methods:**

- `void persist()`
  > Marks the values set by {@link ClientParticles#setParticleCount(int)} and {@link ClientParticles#setVelocity(Vec3)} to be persistent
- `void setParticleCount(int particleCount)`
  > How many particles to spawn per operation <br><b> Volatile unless {@link ClientParticles#persist()} is called before the next operation </b>
- `void setVelocity(Vec3 velocity)`
  > The velocity added to each spawned particle <br><b> Volatile unless {@link ClientParticles#persist()} is called before the next operation </b>
- `void randomizeVelocity(double scalar)`
  > Makes the system use a random velocity for each particle <br><b> Volatile unless {@link ClientParticles#persist()} is called before the next operation </b> @param scalar The scalar to use for the generated velocities which               nominally range from -0.5 to 0.5 on each axis
- `void randomizeVelocityOnAxis(double scalar, Direction.Axis axis)`
  > Makes the system use a random velocity for each particle <br><b> Volatile unless {@link ClientParticles#persist()} is called before the next operation </b> @param scalar The scalar to use for the generated velocities which               nominally range from -0.5 to 0.5 on each axis @param axis   The axis on which to apply random velocity
- `void reset()`
  > Forces a reset of velocity and particleCount
- `void spawnCenteredOnBlock(ParticleOptions particle, Level world, BlockPos pos, double deviation)`
  > Spawns particles with a maximum offset of {@code deviation} from the center of {@code pos} @param particle  The particle to spawn @param world     The world to spawn the particles in, must be {@link net.minecraft.client.multiplayer.ClientLevel} @param pos       The block to center on @param deviation The maximum deviation from the center of pos
- `void spawnLine(ParticleOptions particle, Level world, Vec3 start, Vec3 end, float deviation)`
  > Spawns a line of particles going from {@code start} to {@code end} @param particle  The particle to spawn @param world     The world to spawn the particles in, must be {@link net.minecraft.client.multiplayer.ClientLevel} @param start     The line's origin @param end       The line's end point @param deviation A random offset from the line that particles can have
- `void spawnCubeOutline(ParticleOptions particle, Level world, Vec3 origin, float size, float deviation)`
  > Spawns a cube outline starting at {@code origin} and expanding by {@code size} in positive direction on all axis @param particle  The particle to spawn @param world     The world to spawn the particles in, must be {@link net.minecraft.client.multiplayer.ClientLevel} @param origin    The cube's origin @param size      The cube's side length @param deviation A random offset from the line that particles can have
- `void spawnWithinBlock(ParticleOptions particle, Level world, BlockPos pos)`
  > Spawns particles randomly distributed within {@code pos} @param particle The particle to spawn @param world    The world to spawn the particles in, must be {@link net.minecraft.client.multiplayer.ClientLevel} @param pos      The block to spawn particles in
- `void spawnWithOffsetFromBlock(ParticleOptions particle, Level world, BlockPos pos, Vec3 offset, double deviation)`
  > Spawns particles with a maximum offset of {@code deviation} from {@code pos + offset} @param particle  The particle to spawn @param world     The world to spawn the particles in, must be {@link net.minecraft.client.multiplayer.ClientLevel} @param pos       The base position @param offset    The offset from {@code pos} @param deviation The scalar for random distribution
- `void spawn(ParticleOptions particle, Level world, Vec3 pos, double deviation)`
  > Spawns particles at the given location with a maximum offset of {@code deviation} @param particle  The particle to spawn @param world     The world to spawn the particles in, must be {@link net.minecraft.client.multiplayer.ClientLevel} @param pos       The base position @param deviation The scalar from random distribution
- `void spawnPrecise(ParticleOptions particle, Level world, Vec3 pos, double deviationX, double deviationY, double deviationZ)`
  > Spawns particles at the given location with per-axis deviation control @param particle   The particle to spawn @param world      The world to spawn the particles in, must be {@link net.minecraft.client.multiplayer.ClientLevel} @param pos        The base position @param deviationX The scalar from random distribution on x @param deviationY The scalar from random distribution on y @param deviationZ The scalar from random distribution on z
- `void spawnEnchantParticles(Level world, Vec3 origin, Vec3 destination, float deviation)`
  > Spawns enchant particles travelling from origin to destination @param world       The world to spawn the particles in, must be {@link net.minecraft.client.multiplayer.ClientLevel} @param origin      The origin of the particle stream @param destination The destination of the particle stream @param deviation   The scalar for random distribution around {@code origin}
- `<T extends ParticleOptions> void spawnWithMaxAge(T particleType, Vec3 pos, int maxAge)`
  > Spawns a particle at the given location with a custom lifetime @param particleType The type of the particle to spawn @param pos          The position to spawn at @param maxAge       The maxAge to set for the spawned particle

## Package: `io.wispforest.owo.particles.systems`

### `class` ParticleSystem

> Represents a particle effect that can be played at a position in a world <i>on both client and server</i>, with some optional data attached. <br> To run this effect, call {@link #spawn(Level, Vec3, Object)}. If you call this on the server, a command will be sent to the client to execute the system. <b>Thus, it is important this is registered on both client and server</b>
>
> In case your particle effect not required any additional data, use {@link Void} as the data class and pass {@code null} to {@link #spawn(Level, Vec3, Object)} @param <T> The data class
>

**Methods:**

- `void setHandler(ParticleSystemExecutor<T> handler)`
  > Sets the particle system's handler. @param handler the code that is run to actually display the particle system @throws NetworkException if this particle system already has a handler
- `void spawn(Level level, Vec3 pos, @Nullable T data)`
  > Spawns, or displays, whichever term you prefer, this particle system in the given level at the given position and with the passed context data <p><b>{@code null} data is only allowed if the data class of this particle system is {@link Void}</b> @param level The level to execute in @param pos   The position to execute at @param data  The context to execute with
- `void spawn(Level level, Vec3 pos)`
  > Convenience wrapper for {@link #spawn(Level, Vec3, Object)} that always passes {@code null} data @param level The level to execute in @param pos   The position to execute at

### `class` ParticleSystemController

> A controller object that manages and creates {@link ParticleSystem}s. It is recommended to have one of these per mod.
>
> To obtain a new particle system, call {@link #register(Class, Endec, ParticleSystemExecutor)} with the system's context data class and handler function. <b>It is important that this is done on both client and server, otherwise joining the server will fail in a handshake error</b>
>

**Methods:**

- `ReflectiveEndecBuilder endecBuilder()`
- `ParticleSystem<T> register(Class<T> dataClass, Endec<T> endec, ParticleSystemExecutor<T> executor)`
  > Registers the given system executor with the given context data class, thereby creating a new system @param dataClass The class to use as context data @param executor  The code that is run to actually display the particle system @param <T>       The type of context data to use @return The created particle system
- `ParticleSystem<T> register(Class<T> dataClass, ParticleSystemExecutor<T> executor)`
  > Shorthand for {{@link #register(Class, Endec, ParticleSystemExecutor)}} which creates the endec through {@link ReflectiveEndecBuilder#get(Class)}
- `ParticleSystem<T> registerDeferred(Class<T> dataClass, Endec<T> endec)`
  > Registers the given system executor with the given context data class, thereby creating a new system <p> This method defers executor registration, so you must register the handler later in a client entrypoint. @param dataClass The class to use as context data @param <T>       The type of context data to use @return The created particle system @see ParticleSystem#setHandler(ParticleSystemExecutor)
- `ParticleSystem<T> registerDeferred(Class<T> dataClass)`
  > Shorthand for {{@link #registerDeferred(Class, Endec)}} which creates the endec through {@link ReflectiveEndecBuilder#get(Class)}
- `void execute(Level level, Vec3 pos)`
- `Type<? extends CustomPacketPayload> type()`

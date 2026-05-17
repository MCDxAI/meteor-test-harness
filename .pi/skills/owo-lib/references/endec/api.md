## API Reference

## Table of Contents

- [CodecUtils](#codecutils) — `io.wispforest.owo.serialization`
- [ContextHolder](#contextholder) — `io.wispforest.owo.serialization.format`

## Package: `io.wispforest.owo.serialization`

### `class` CodecUtils

**Methods:**

- `Endec<T> toEndec(Codec<T> codec)`
  > Create a new endec serializing the same data as {@code codec} <p> This method is implemented by converting all data to be (de-)serialized to the Endec Data Model data format (hereto-forth to be referred to as EDM) which has both an endec ({@link EdmEndec}) and DynamicOps implementation ({@link EdmOps}). Since EDM encodes structure using a self-described format's native structural types, <b>this means that for JSON and NBT, the created endec's serialized representation is identical to that of {@code codec}</b>. In general, for non-self-described formats, the serialized representation is a byte array <p> When decoding, an EDM element is read from the deserializer and then parsed using {@code codec} <p> When encoding, the value is encoded using {@code codec} to an EDM element which is then written into the serializer
- `Endec<T> toEndec(Codec<T> codec, StreamCodec<ByteBuf, T> packetCodec)`
- `Endec<T> toEndecWithRegistries(Codec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec)`
- `Codec<T> toCodec(Endec<T> endec, SerializationContext assumedContext)`
  > Create a codec serializing the same data as this endec, assuming that the serialized format posses the {@code assumedAttributes} <p> This method is implemented by converting between a given DynamicOps' datatype and EDM (see {@link #toEndec(Codec)}) and then encoding/decoding from/to an EDM element using the {@link EdmSerializer} and {@link EdmDeserializer} <p> The serialized representation of a codec created through this method is generally identical to that of a codec manually created to describe the same data
- `DataResult<D> encode(T input, DynamicOps<D> ops, D prefix)`
- `Codec<T> toCodec(Endec<T> endec)`
- `MapCodec<T> toMapCodec(StructEndec<T> structEndec, SerializationContext assumedContext)`
- `Stream<T1> keys(DynamicOps<T1> ops)`
- `DataResult<T> decode(DynamicOps<T1> ops, MapLike<T1> input)`
- `RecordBuilder<T1> encode(T input, DynamicOps<T1> ops, RecordBuilder<T1> prefix)`
- `MapCodec<T> toMapCodec(StructEndec<T> structEndec)`
- `StructEndec<T> toStructEndec(MapCodec<T> mapCodec)`
- `void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, T value)`
- `T decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct)`
- `StreamCodec<B, T> toPacketCodec(Endec<T> endec)`
- `T decode(B buf)`
- `void encode(B buf, T value)`
- `SerializationContext createContext(DynamicOps<?> ops, SerializationContext assumedContext)`
- `void registerCodecAdapter(CodecAdapter<?, ?, ?> adapter)`
- `NbtSerializer createSerializer()`
- `NbtDeserializer createDeserializer(Tag value)`
- `Tag unpackMapLike(MapLike<Tag> mapLike)`
- `RecordBuilder<Tag> addToBuilder(Tag value, RecordBuilder<Tag> builder)`
- `void encodeStruct(SerializationContext ctx, NbtSerializer serializer, Serializer.Struct struct, Tag value)`
- `Tag copyDecodedStruct(SerializationContext ctx, NbtDeserializer deserializer, Deserializer.Struct struct)`
- `GsonSerializer createSerializer()`
- `GsonDeserializer createDeserializer(JsonElement value)`
- `JsonElement unpackMapLike(MapLike<JsonElement> mapLike)`
- `RecordBuilder<JsonElement> addToBuilder(JsonElement value, RecordBuilder<JsonElement> builder)`
- `void encodeStruct(SerializationContext ctx, GsonSerializer serializer, Serializer.Struct struct, JsonElement value)`
- `JsonElement copyDecodedStruct(SerializationContext ctx, GsonDeserializer serializer, Deserializer.Struct struct)`

## Package: `io.wispforest.owo.serialization.format`

### `interface` ContextHolder

> A common interface for parts of a serialization infrastructure which provide an instance of {@link SerializationContext}. Primarily used for attaching context to {@link com.mojang.serialization.DynamicOps}
>


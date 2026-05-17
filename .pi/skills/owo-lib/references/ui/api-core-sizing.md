### `class` Sizing

#### `class` Random

> A collection of utility methods for generating random sizing instances @author chyzman
>

#### `enum` Method

> The content factor of a sizing instance describes where on the spectrum from content to fixed sizing it sits. Specifically, this is used to lerp the reference frame used for calculating {@code fill(...)} sizing on children between the available space in this component (content factor 0) and this component's own available space (content factor 1), both of which can be independently determined prior to layout calculations
>

**Fields:**

- `class Random` — A collection of utility methods for generating random sizing instances @author chyzman

**Methods:**

- `int inflate(int space, Function<Sizing, Integer> contentSizeFunction)`
  > Inflate into the given space @param space               The available space @param contentSizeFunction A function for making the component set the                            size based on its content
- `Sizing fixed(int value)`
- `Sizing content()`
  > Dynamically size the component based on its content, without any padding
- `Sizing content(int padding)`
  > Dynamically size the component based on its content @param padding Padding to add onto the size of the content
- `Sizing fill()`
  > Dynamically size the component to fill the available space
- `Sizing fill(int percent)`
  > Dynamically size the component based on the available space @param percent How many percent of the available space to take up
- `Sizing expand()`
  > Dynamically size the component based on the remaining space <i>after all other components have been laid out</i>
- `Sizing expand(int percent)`
  > Dynamically size the component based on the remaining space <i>after all other components have been laid out</i> @param percent How many percent of the available space to take up
- `Sizing fill(int min, int max)`
  > Generate a random fill sizing instance with a value between {@code min} and {@code max} @param min The minimum value @param max The maximum value @return A random sizing instance
- `Sizing fill(int max)`
  > Generate a random fill sizing instance with a value between 0 and {@code max} @param max The maximum value @return A random sizing instance
- `Sizing fill()`
  > Generate a random fill sizing instance with a value between 0 and 100 @return A random sizing instance
- `Sizing expand(int min, int max)`
  > Generate a random expand sizing instance with a value between {@code min} and {@code max} @param min The minimum value @param max The maximum value @return A random sizing instance
- `Sizing expand(int max)`
  > Generate a random expand sizing instance with a value between 0 and {@code max} @param max The maximum value @return A random sizing instance
- `Sizing expand()`
  > Generate a random expand sizing instance with a value between 0 and 100 @return A random sizing instance
- `Sizing fixed(int min, int max)`
  > Generate a random fixed sizing instance with a value between {@code min} and {@code max} @param min The minimum value @param max The maximum value @return A random sizing instance
- `Sizing fixed(int max)`
  > Generate a random fixed sizing instance with a value between 0 and {@code max} @param max The maximum value @return A random sizing instance
- `Sizing fixed()`
  > Generate a random fixed sizing instance with a value between 0 and 100 @return A random sizing instance
- `Sizing content(int min, int max)`
  > Generate a random content sizing instance with a padding value between {@code min} and {@code max} @param min The minimum value @param max The maximum value @return A random sizing instance
- `Sizing content(int max)`
  > Generate a random content sizing instance with a padding value between 0 and {@code max} @param max The maximum value @return A random sizing instance
- `Sizing content()`
  > Generate a random content sizing instance with a padding value between 0 and 100 @return A random sizing instance
- `Sizing random(int min, int max)`
  > Generate a random sizing instance with a value between {@code min} and {@code max} @param min The minimum value @param max The maximum value @return A random sizing instance @apiNote May crash if put on a component that doesn't support content sizing
- `Sizing random(int max)`
  > Generate a random sizing instance with a value between 0 and {@code max} @param max The maximum value @return A random sizing instance @apiNote May crash if put on a component that doesn't support content sizing
- `Sizing random()`
  > Generate a random sizing instance with a value between 0 and 100 @return A random sizing instance @apiNote May crash if put on a component that doesn't support content sizing
- `Sizing noContent(int min, int max)`
  > Generate a random sizing instance with a value between {@code min} and {@code max} that is not content-based @param min The minimum value @param max The maximum value @return A random sizing instance
- `Sizing noContent(int max)`
  > Generate a random sizing instance with a value between 0 and {@code max} that is not content-based @param max The maximum value @return A random sizing instance
- `Sizing noContent()`
  > Generate a random sizing instance that is not content-based @return A random sizing instance
- `boolean isContent()`
  > @return {@code true} if this sizing instance uses the {@linkplain Method#CONTENT CONTENT} method
- `boolean isExpand()`
  > @return {@code true} if this sizing instance uses the {@linkplain Method#EXPAND EXPAND} method
- `float contentFactor()`
  > The content factor of a sizing instance describes where on the spectrum from content to fixed sizing it sits. Specifically, this is used to lerp the reference frame used for calculating {@code fill(...)} sizing on children between the available space in this component (content factor 0) and this component's own available space (content factor 1), both of which can be independently determined prior to layout calculations
- `Sizing interpolate(Sizing next, float delta)`
- `Sizing parse(Element sizingElement)`
- `int inflate(int space, Function<Sizing, Integer> contentSizeFunction)`
- `Sizing interpolate(Sizing next, float delta)`
- `float contentFactor()`

### `class` Random

> A collection of utility methods for generating random sizing instances @author chyzman
>

**Methods:**

- `Sizing fill(int min, int max)`
  > Generate a random fill sizing instance with a value between {@code min} and {@code max} @param min The minimum value @param max The maximum value @return A random sizing instance
- `Sizing fill(int max)`
  > Generate a random fill sizing instance with a value between 0 and {@code max} @param max The maximum value @return A random sizing instance
- `Sizing fill()`
  > Generate a random fill sizing instance with a value between 0 and 100 @return A random sizing instance
- `Sizing expand(int min, int max)`
  > Generate a random expand sizing instance with a value between {@code min} and {@code max} @param min The minimum value @param max The maximum value @return A random sizing instance
- `Sizing expand(int max)`
  > Generate a random expand sizing instance with a value between 0 and {@code max} @param max The maximum value @return A random sizing instance
- `Sizing expand()`
  > Generate a random expand sizing instance with a value between 0 and 100 @return A random sizing instance
- `Sizing fixed(int min, int max)`
  > Generate a random fixed sizing instance with a value between {@code min} and {@code max} @param min The minimum value @param max The maximum value @return A random sizing instance
- `Sizing fixed(int max)`
  > Generate a random fixed sizing instance with a value between 0 and {@code max} @param max The maximum value @return A random sizing instance
- `Sizing fixed()`
  > Generate a random fixed sizing instance with a value between 0 and 100 @return A random sizing instance
- `Sizing content(int min, int max)`
  > Generate a random content sizing instance with a padding value between {@code min} and {@code max} @param min The minimum value @param max The maximum value @return A random sizing instance
- `Sizing content(int max)`
  > Generate a random content sizing instance with a padding value between 0 and {@code max} @param max The maximum value @return A random sizing instance
- `Sizing content()`
  > Generate a random content sizing instance with a padding value between 0 and 100 @return A random sizing instance
- `Sizing random(int min, int max)`
  > Generate a random sizing instance with a value between {@code min} and {@code max} @param min The minimum value @param max The maximum value @return A random sizing instance @apiNote May crash if put on a component that doesn't support content sizing
- `Sizing random(int max)`
  > Generate a random sizing instance with a value between 0 and {@code max} @param max The maximum value @return A random sizing instance @apiNote May crash if put on a component that doesn't support content sizing
- `Sizing random()`
  > Generate a random sizing instance with a value between 0 and 100 @return A random sizing instance @apiNote May crash if put on a component that doesn't support content sizing
- `Sizing noContent(int min, int max)`
  > Generate a random sizing instance with a value between {@code min} and {@code max} that is not content-based @param min The minimum value @param max The maximum value @return A random sizing instance
- `Sizing noContent(int max)`
  > Generate a random sizing instance with a value between 0 and {@code max} that is not content-based @param max The maximum value @return A random sizing instance
- `Sizing noContent()`
  > Generate a random sizing instance that is not content-based @return A random sizing instance


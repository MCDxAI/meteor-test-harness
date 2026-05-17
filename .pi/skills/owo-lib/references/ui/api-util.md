## Package: `io.wispforest.owo.ui.util`

### `interface` CommandOpenedScreen

> A marker interface for screens that are opened by client-sided commands which prevents the chat screen from closing the newly opened screen
>

### `class` Delta

> Trying to give this utility class a sensible name makes me mald
>

**Methods:**

- `float compute(float current, float target, float delta)`
  > Compute an additive interpolator for smoothly approaching the target value given the current value and some interpolation delta @param current The current value @param target  The target value to approach @param delta   The interpolation delta - this is usually the frame delta,                optionally multiplied by some factor @return The computed interpolator, to be added to the current value
- `double compute(double current, double target, double delta)`
  > Compute an additive interpolator for smoothly approaching the target value given the current value and some interpolation delta @param current The current value @param target  The target value to approach @param delta   The interpolation delta - this is usually the frame delta,                optionally multiplied by some factor @return The computed interpolator, to be added to the current value

### `interface` DisposableScreen

> Screens that wish to be notified when the players navigates back to the game instead of to another screen may implement this interface for a more reliable alternative to {@link Screen#removed()}
>

### `interface` MatrixStackTransformer

> Helper interface implemented on top of the {@link GuiGraphics} to allow for easier matrix stack transformations
>


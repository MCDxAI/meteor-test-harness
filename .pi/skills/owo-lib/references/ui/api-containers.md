## Package: `io.wispforest.owo.ui.container`

### `class` FlowLayout

**Methods:**

- `int determineHorizontalContentSize(Sizing sizing)`
- `int determineVerticalContentSize(Sizing sizing)`
- `void layout(Size space)`
- `FlowLayout child(UIComponent child)`
  > Add a single child to this layout. If you need to add multiple children, use {@link #children(Collection)} instead @param child The child to append to this layout
- `FlowLayout children(Collection<? extends UIComponent> children)`
  > Add a collection of children to this layout. If you only need to add a single child to, use {@link #child(UIComponent)} instead @param children The children to add to this layout
- `FlowLayout child(int index, UIComponent child)`
  > Insert a single child into this layout. If you need to insert multiple children, use {@link #children(int, Collection)} instead @param index The index at which to insert the child @param child The child to append to this layout
- `FlowLayout children(int index, Collection<? extends UIComponent> children)`
  > Insert a collection of children into this layout. If you only need to insert a single child to, use {@link #child(int, UIComponent)} instead @param index    The index at which to begin inserting children @param children The children to add to this layout
- `FlowLayout removeChild(UIComponent child)`
- `FlowLayout clearChildren()`
  > Remove all children from this layout
- `List<UIComponent> children()`
- `FlowLayout gap(int gap)`
  > Set the gap, in logical pixels, this layout should insert between all child components
- `int gap()`
  > @return The gap, in logical pixels, this layout inserts between all child components
- `void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta)`
- `void parseProperties(UIModel model, Element element, Map<String, Element> children)`
- `MutableComponent inspectorDescriptor()`
- `FlowLayout parse(Element element)`

### `class` OverlayContainer

**Methods:**

- `void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta)`
- `void drawFocusHighlight(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta)`
- `void mount(ParentUIComponent parent, int x, int y)`
- `void dismount(DismountReason reason)`
- `boolean onMouseDown(MouseButtonEvent click, boolean doubled)`
- `boolean onMouseScroll(double mouseX, double mouseY, double amount)`
- `boolean canFocus(FocusSource source)`
- `int childMountX()`
- `int childMountY()`
- `OverlayContainer<C> closeOnClick(boolean closeOnClick)`
  > Set whether this overlay should close when a mouse click occurs outside the bounds of its contents
- `boolean closeOnClick()`
  > Whether this overlay should close when a mouse click occurs outside the bounds of its contents

### `class` ScrollContainer

**Methods:**

- `int determineHorizontalContentSize(Sizing sizing)`
- `int determineVerticalContentSize(Sizing sizing)`
- `void layout(Size space)`
- `int childMountX()`
- `int childMountY()`
- `void parentUpdate(float delta, int mouseX, int mouseY)`
- `void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta)`
- `boolean canFocus(FocusSource source)`
- `boolean onMouseDown(MouseButtonEvent click, boolean doubled)`
- `boolean onMouseDrag(MouseButtonEvent click, double deltaX, double deltaY)`
- `boolean onKeyPress(KeyEvent input)`
- `boolean onMouseUp(MouseButtonEvent click)`
- `void scrollBy(double offset, boolean instant, boolean showScrollbar)`
- `ScrollContainer<C> scrollTo(UIComponent component)`
  > Scroll to the given component
- `ScrollContainer<C> scrollTo(@Range(from = 0, to = 1) double progress)`
  > Scroll to the specified point along the entire length of this container's content
- `ScrollContainer<C> scrollbarThiccness(int scrollbarThiccness)`
  > Set the thickness of this container's scrollbar, in logical pixels
- `int scrollbarThiccness()`
  > @return The thickness of this container's scrollbar, in logical pixels
- `ScrollContainer<C> scrollbar(Scrollbar scrollbar)`
  > Set the scrollbar this container should display. To create one, look at the static methods on {@link Scrollbar} or use a lambda
- `Scrollbar scrollbar()`
  > @return The scrollbar this container is currently displaying
- `ScrollContainer<C> scrollStep(int scrollStep)`
  > Set the increment, or step size, this container should scroll by. If this is anything other than {@code 0}, all scrolling in this container will snap to the closest multiple of this value
- `int scrollStep()`
  > @return The current scroll step size of this container
- `ScrollContainer<C> fixedScrollbarLength(int fixedScrollbarLength)`
  > Set a fixed length for the scrollbar of this container, {@code 0} for dynamic sizing
- `int fixedScrollbarLength()`
  > @return The current fixed length of this container's scrollbar, or {@code 0} if it adjusts based on the content
- `void parseProperties(UIModel model, Element element, Map<String, Element> children)`
- `ScrollContainer<?> parse(Element element)`
- `double choose(double horizontal, double vertical)`

### `class` StackLayout

**Methods:**

- `int determineHorizontalContentSize(Sizing sizing)`
- `int determineVerticalContentSize(Sizing sizing)`
- `void layout(Size space)`
- `void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta)`
- `StackLayout child(UIComponent child)`
  > Add a single child to this layout. If you need to add multiple children, use {@link #children(Collection)} instead @param child The child to append to this layout
- `StackLayout children(Collection<? extends UIComponent> children)`
  > Add a collection of children to this layout. If you only need to add a single child to, use {@link #child(UIComponent)} instead @param children The children to add to this layout
- `StackLayout child(int index, UIComponent child)`
  > Insert a single child into this layout. If you need to insert multiple children, use {@link #children(int, Collection)} instead @param index The index at which to insert the child @param child The child to append to this layout
- `StackLayout children(int index, Collection<? extends UIComponent> children)`
  > Insert a collection of children into this layout. If you only need to insert a single child to, use {@link #child(int, UIComponent)} instead @param index    The index at which to begin inserting children @param children The children to add to this layout
- `StackLayout removeChild(UIComponent child)`
- `StackLayout clearChildren()`
  > Remove all children from this layout
- `List<UIComponent> children()`

### `class` WrappingParentUIComponent

**Methods:**

- `int determineHorizontalContentSize(Sizing sizing)`
- `int determineVerticalContentSize(Sizing sizing)`
- `void layout(Size space)`
- `int childMountX()`
  > @return The x-coordinate at which to mount the child
- `int childMountY()`
  > @return The y-coordinate at which to mount the child
- `WrappingParentUIComponent<C> child(C newChild)`
- `C child()`
- `List<UIComponent> children()`
- `void parseProperties("unchecked")`


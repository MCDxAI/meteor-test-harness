## Layout Types
owo-ui has two built-in layout components, which we'll explore now. Should neither of these meet your needs, you can always make your own - simply extend `BaseParentComponent`.

### Flow Layout
![vertical flow layout reference](../../assets/owo/flow-layout-reference.png)

The flow layout is the simplest layout you could possibly think of - as the name suggests, it simply places all children one after the other. Depending on the axis, components flow top-to-bottom or left-to-right in the order that they were added.

### Grid Layout

The grid layout is equally simple as the flow layout - it places every child in its own cell of a grid. The amount of rows and columns must be defined when you create the layout, as it internally represent the grid as a fixed-size array.

When a grid layout is content-sized, every row and column becomes as large as it needs to be to fit the largest cell. Otherwise, every row and column is evenly split along the layout's length in that direction.

![grid layout reference](../../assets/owo/grid-layout-reference.png)

## Layout Properties

Every layout (in fact, all parent components) share some additional properties which normal components don't define. The ones relevant to layout are listed here.

### Alignment

The horizontal and vertical alignment of a parent component dictates where within itself the children should be positioned. For most components this is relative to the whole component, however the grid is an exception to this - children are always aligned relative to their respective cell.

### Padding

As explained in component basics, all parent components define padding which is removed from the area available to their children. This is most useful in combination with a Surface, as it means you can add some extra visual breathing room around the children and make the border of your surface clearly visible.

### Overflow Clipping

While playing around with the framework, you may have encountered a situation under which a child component extends outside its parent without the protruding parts actually being rendered. This is due to overflow clipping - by default all parent components only allow rendering inside their bounding box. You can of course disable this and allow overflow to be rendered, although that generally leads to more problems than it solves.

A child which is completely invisible due to this is skipped entirely during rendering, which majorly improves performance with large scroll containers.

Besides the obvious essentials like buttons, labels and basic layouts, owo-ui also offers a range of utility components that solve problems commonly encountered during UI design.

### Scroll Container

The scroll container is flexible solution to making anything scrollable. It's a parent component that accepts a single child and provides it infinite space on the scrollable axis. When scrolled, it relocates its children according to the current scroll offset instead of only shifting their rendering positions - it does not however cause any state updates to be emitted.

### Draggable Container

When you want a component to be movable on screen (just like an application window), you can use the draggable container. It wraps its single child and adds an extra bit of space (dubbed the "forehead") above, which can be grabbed with the mouse to move the entire component on screen.

### Collapsible Container

The collapsible container is an extension of a flow layout that allows collapsible its children out of sight. It displays an arrow hinting at the fact it can be expanded as well as a title describing its content. Other than that, it behaves like a standard flow layout.

### Overlay Container

The overlay container does exactly what it sounds like - display its child as an overlay over its entire parent. This is primarily intended for dialog-like pop-ups, in which case you'd want to add the overlay as a child directly to the root component. 

By default, the overlay closes when clicked outside the bounding box of its content - you can configure this behavior with `closeOnClick(...)`


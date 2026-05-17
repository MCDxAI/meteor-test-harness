# UI Framework (owo-ui)

## Table of Contents

  - [Choosing a paradigm](#choosing-a-paradigm)
    - [Code-driven](#code-driven)
    - [Data-driven](#data-driven)
  - [What's next](#whats-next)
      - [Setup](#setup)
      - [Using the Inspector](#using-the-inspector)
  - [Using the Inspector](#using-the-inspector)
  - [Margins](#margins)
  - [Padding](#padding)
  - [Sizing](#sizing)
  - [Positioning](#positioning)
  - [Layout Types](#layout-types)
    - [Flow Layout](#flow-layout)
    - [Grid Layout](#grid-layout)
  - [Layout Properties](#layout-properties)
    - [Alignment](#alignment)
    - [Padding](#padding)
    - [Overflow Clipping](#overflow-clipping)
    - [Scroll Container](#scroll-container)
    - [Draggable Container](#draggable-container)
    - [Collapsible Container](#collapsible-container)
    - [Overlay Container](#overlay-container)
    - [Dropdown Component](#dropdown-component)
  - [Value Replacement in Templates](#value-replacement-in-templates)
  - [API Reference](#api-reference)
  - [Table of Contents](#table-of-contents)
  - [Package: `io.wispforest.owo.ui.base`](#package)
    - [`class` BaseOwoContainerScreen](#baseowocontainerscreen)
    - [`class` BaseOwoScreen](#baseowoscreen)
    - [`class` BaseParentUIComponent](#baseparentuicomponent)
    - [`class` BaseUIComponent](#baseuicomponent)
    - [`class` BaseUIModelContainerScreen](#baseuimodelcontainerscreen)
    - [`class` BaseUIModelScreen](#baseuimodelscreen)
      - [`interface` DataSource](#datasource)
    - [`interface` DataSource](#datasource)
  - [Package: `io.wispforest.owo.ui.component`](#package)
    - [`class` BoxComponent](#boxcomponent)
      - [`enum` GradientDirection](#gradientdirection)
    - [`class` DropdownComponent](#dropdowncomponent)
    - [`class` ItemComponent](#itemcomponent)
    - [`class` SliderComponent](#slidercomponent)
      - [`interface` OnChanged](#onchanged)
      - [`interface` OnSlideEnd](#onslideend)
    - [`class` TextBoxComponent](#textboxcomponent)
    - [`class` UIComponents](#uicomponents)
  - [Package: `io.wispforest.owo.ui.container`](#package)
    - [`class` FlowLayout](#flowlayout)
    - [`class` OverlayContainer](#overlaycontainer)
    - [`class` ScrollContainer](#scrollcontainer)
    - [`class` StackLayout](#stacklayout)
    - [`class` WrappingParentUIComponent](#wrappingparentuicomponent)
  - [Package: `io.wispforest.owo.ui.core`](#package)
    - [`class` AnimatableProperty](#animatableproperty)
    - [`record` Color](#color)
    - [`interface` Easing](#easing)
    - [`class` OwoUIAdapter](#owouiadapter)
    - [`class` OwoUIGraphics](#owouigraphics)
    - [`interface` PositionedRectangle](#positionedrectangle)
    - [`class` Positioning](#positioning)
      - [`enum` Type](#type)
    - [`record` Size](#size)
    - [`class` Sizing](#sizing)
      - [`class` Random](#random)
      - [`enum` Method](#method)
    - [`class` Random](#random)
  - [Package: `io.wispforest.owo.ui.hud`](#package)
    - [`class` Hud](#hud)
    - [`class` HudContainer](#hudcontainer)
  - [Package: `io.wispforest.owo.ui.inject`](#package)
    - [`interface` GreedyInputUIComponent](#greedyinputuicomponent)
    - [`interface` UIComponentStub](#uicomponentstub)
  - [Package: `io.wispforest.owo.ui.layers`](#package)
    - [`class` Layer](#layer)
    - [`class` Instance](#instance)
    - [`class` Layers](#layers)
  - [Package: `io.wispforest.owo.ui.parsing`](#package)
    - [`class` IncompatibleUIModelException](#incompatibleuimodelexception)
    - [`class` UIModel](#uimodel)
    - [`class` UIModelLoader](#uimodelloader)
    - [`class` UIModelParsingException](#uimodelparsingexception)
    - [`class` UIParsing](#uiparsing)
  - [Package: `io.wispforest.owo.ui.util`](#package)
    - [`interface` CommandOpenedScreen](#commandopenedscreen)
    - [`class` Delta](#delta)
    - [`interface` DisposableScreen](#disposablescreen)
    - [`interface` MatrixStackTransformer](#matrixstacktransformer)

owo-ui is a declarative UI framework for building dynamic Minecraft screens. It supports both code-driven and data-driven (XML) UI design, with features like dynamic sizing, component inspector, animations, and hot-reloading during development.

owo-ui is a declarative UI framework that helps with building dynamic screens quickly and easily. It strives to be highly embeddable, performant and, most of all, super easy to use. We provide the following major features:

    Every part of your UI is represented as one or multiple Components, which you compose in a simple tree structure - just like modern web tech

    You can choose to describe your layout in code or alternatively in a custom format implemented through standard XML. When using the XML-based approach, you essentially get hotswapping for free - when in dev-mode the file is reloaded every time you open the screen, making for blazingly fast iteration times

    We strive to keep every part of the framework documented and provide an interactive tutorial (owo-ui-academy) which teaches you the basics. While developing, you can easily debug your UI using the always-available component inspector, which works quite similarly to the "Inspect element" tool in modern browsers

    You rarely need to hardcode the position or size of a component - everything is computed dynamically to match current layout and available screen space

    Even the most functional of screens can use a little animation now and then - owo-ui got you covered. Animating the size or position of a component over time is as simple as calling a single function

## Choosing a paradigm

Before creating your first screen with owo-ui, you need to decide which approach you want to take: code-driven or data-driven. Here's a quick comparison:

|                    [Code-driven ](#code-driven)                    |     [Data-driven ](#data-driven)     |
|:---------------------------------------------------------------------------------:|:---------------------------------------------------:|
| **+** Everything in one place: your logic and <br> layout reside in a single file | **+** Superior, more readable <br> visual structure |
|                          **+** Slightly faster to set up                          |       **+** Can be changed by resource packs        |
|                           **-** Hotswapping can be slow                           |               **+** Instant reloading               |
|                                                                                   |       **-** Slightly more hassle to maintain        |

In general, the code-driven approach is recommended for beginners. However, it is often preferable to switch to data-driven UI design once you're more familiar with the framework.

While there is currently no tool for converting between the two paradigms, it's usually pretty easy to rewrite your UI in either of them - so don't worry too much about the decision you make here.

### Code-driven

In order to begin building your first screen with the code-driven approach, create a screen class which extends `BaseOwoScreen`. You will notice that this superclass requires a type parameter - this is the type of root component you want to use. Since we're just starting out, let's pick the `FlowLayout` and implement the `createAdapter()` and `build(...)` methods.

```java
public class MyFirstScreen extends BaseOwoScreen<FlowLayout> {

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        // TODO
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        // TODO
    }
}
```

You now need to fill in these two methods. In `createAdapter()` you have to initialize the UI system. For this job we use `OwoUIAdapter.create(...)` and pass in the screen and root component factory, for which we'll choose the `FlowLayout` with the `VERTICAL` algorithm via `Containers.verticalFlow(...)`, making the final implementation look like this:

```java
@Override
protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
    return OwoUIAdapter.create(this, Containers::verticalFlow);
}
```

The UI Adapter is the primary entrypoint to using owo-ui. It handles input events, rendering the component tree, initiating layout inflation, and most importantly it implements all interfaces required to function as a vanilla widget. This means that adding owo-ui to an existing screen can easily be done by creating an adapter and `addDrawableChild`ing it. This isn't a requirement however - the fact that you can use this one handle to utilize the entire UI system is the main reason it is as embeddable as it is. In general: If you have a 2D rendering context, you can use owo-ui.

And that's it! You now have the screen all initialized and ready to go. If you open it at this point, you'll see precisely nothing:

![a blank screen](../../assets/owo/getting-started/step-0.png)

Let's fix that by giving our root component a so-called surface, in this case the standard dark, translucent background most vanilla UIs use.

```java
@Override
protected void build(FlowLayout rootComponent) {
    rootComponent.surface(Surface.VANILLA_TRANSLUCENT);
}
```

Every parent component (that is, every component which can contain other components) has what's called a "Surface" which is in charge of rendering the component's background. There are several defaults available to use directly on the `Surface` interface which should cover most bases, and all surfaces can be chained using the `and(...)` method. Should you require a more custom solution however, simply provide your surface as a lambda - it's only a rendering function after all.

Now the screen looks like this:

![a screen with a dark translucent background](../../assets/owo/getting-started/step-1.png)

Great! Let's begin by simply adding a button right in the center. To accomplish this, we first set the root component's alignment to `CENTER` on both axes and then add our button - like this:

```java
@Override
protected void build(FlowLayout rootComponent) {
    rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

    rootComponent.child(
        Components.button(
                Text.literal("A Button"),
                button -> {
                    System.out.println("click");
                }
            )
    );
}
```

While writing this code you probably noticed that all the methods we're calling on the root component return the component, so we can easily chain calls. You may even have tried to append the `.child(...)` call after the alignment has been set up, but couldn't because the method suddenly did not exist anymore. This is due to type decay - methods on components will always return the least specific type of component they're defined on. Because alignment is a property common to all parent components, the method returns precisely that - a `ParentComponent` instead of a `FlowLayout`, which does not define a way to add children. You will encounter this often, so remember that you can simply avoid it by changing the order of your method calls.

The result is as expected, a button saying "A Button" right in the center of our screen:

![a screen with a centered button](../../assets/owo/getting-started/step-2.png)

Before we finish off this introduction, let's wrap the button in a container and style that like a vanilla UI panel. To do this, we only need to create the container via `Containers.verticalFlow(...)` and add our button and surface to it like normal. If you understood everything so far, the following code should be pretty simple to grasp:

```java
@Override
protected void build(FlowLayout rootComponent) {
    rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);
    
    rootComponent.child(
            Containers.verticalFlow(Sizing.content(), Sizing.content()) // 1
                .child(Components.button(Text.literal("A Button"), button -> { System.out.println("click"); }))
                .padding(Insets.of(10)) // 2
                .surface(Surface.DARK_PANEL)
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
    );
}
```

1. When creating the container we need to tell it how large it's supposed to be. By providing `Sizing.content()` for both axes we tell it to make itself however large it needs to be to fit its children

2. In order to properly see the surface of our container, we must make it slightly larger than the button - we do this by giving it 10 pixels of padding on all sides

Leaving us with a final screen that looks like this:

![a screen with a centered button and a panel around it](../../assets/owo/getting-started/step-3.png)


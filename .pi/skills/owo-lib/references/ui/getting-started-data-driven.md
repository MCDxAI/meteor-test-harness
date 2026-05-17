### Data-driven

Before jumping in the XML-based approach, let's clear up some terminology. The XML file which we're about to make is called the **UI Model**, since it describes a UI. It contains your component tree and, optionally, **templates** which can be used for creating similar components with different parameters.

To create your first screen in this fashion, extend `BaseUIModelScreen`. Just like `BaseOwoScreen` it expects a type parameter describing which kind of root component you want to use. Since we're trying to create the same screen we did with the code-driven approach, we'll specify `FlowLayout`.

```java
public class MyScreen extends BaseUIModelScreen<FlowLayout> {

    public MyScreen() {
        super(FlowLayout.class, /* TODO */);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        // TODO
    }
}
```

Now the superclass constructor requires a `DataSource`. This is simply a way to describe where the UI Model should be loaded from - for this there are two standard options: 

- ~~`DataSource.file(...)` is used for development - you simply give it the file path to your UI model, relative to the game's run directory. With this data source, the file is re-loaded every time you open the screen, which enables the instant hotswapping. When building for production however, this is not an option and will crash at runtime.~~<br><br>
As of **0.11**, this is deprecated and should no longer be used. Instead, you can press [[Ctrl]]+[[f5]] while viewing the screen and select your UI model file in the menu that shows. Alternatively, you can also use the `/owo-ui-set-reload-path` command to associated a file with any given UI model

- `DataSource.asset(...)` loads the model from the current resourcepacks. It expects an identifier like `mymod:my_ui_model`, which would point to `assets/mymod/owo_ui/my_ui_model.xml`. This way the model is only refreshed when reloading resource packs, making for much better performance and allowing resourcepacks to override and customize your UI.

For this example, let's use the `assets` data source and place our `my_ui_model.xml` file in th `assets/mymod/owo_ui/` directory of your project, turning the constructor into this:

```java
public MyScreen() {
    super(FlowLayout.class, DataSource.asset(new Identifier("mymod", "my_ui_model")));
}
```

Now comes the meat of this exercise - creating the UI Model in XML. To begin, we'll create a file called `my_ui_model.xml` in the run directory, with the following content:

```xml
<owo-ui xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/wisp-forest/owo-lib/1.20/owo-ui.xsd">
    <components>

    </components>
</owo-ui>
```

If you're using IntelliJ, you now have to place your cursor on the URL in `xsi:noNamespaceSchemaLocation`, press [[Alt]]+[[Enter]] and select `Fetch external resource`. This will make it download the XML schema, giving you rich autocomplete and error checking right inside the IDE.

Great, you can now begin building your UI. Let's first declare the root component. We'll use the `flow-layout`, just as declared on our class, and say that its `direction` is `vertical`:
![flow layout in xml](../../assets/owo/getting-started/flow-layout.gif)

We're now again at the point were you can open your screen - once again it will just be completely blank. Let's add content then - for the sake of brevity this will now all be a single code block:

```xml
<owo-ui xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/wisp-forest/owo-lib/1.20/owo-ui.xsd">
    <components>
        <flow-layout direction="vertical">
            <children> <!--(1)-->
                <flow-layout direction="vertical"> <!--(2)-->
                    <children>
                        <button id="the-button"> <!--(3)-->
                            <text>A Button</text> <!--(6)-->
                        </button>
                    </children>

                    <horizontal-alignment>center</horizontal-alignment>
                    <vertical-alignment>center</vertical-alignment>

                    <!--(4)-->
                    <padding>
                        <all>10</all>
                    </padding>

                    <surface>
                        <panel dark="true"/>
                    </surface>
                </flow-layout>
            </children>

            <vertical-alignment>center</vertical-alignment>
            <horizontal-alignment>center</horizontal-alignment>

            <!--(5)-->
            <surface>
                <vanilla-translucent/>
            </surface>
        </flow-layout>
    </components>
</owo-ui>
```

1. This is where you declare the children of the flow layout - much like calling `.child(...)` in the previous example

2. Here, we declare the container for the button - this is the equivalent of `Containers.verticalFlow(...)`

3. Here goes the button itself. As you can see, we give it an ID - this will be important soon

4. We declare padding very similarly to how it's done it code - note however, that these declarations like `all` can chain. This means if you want 5 pixels on the bottom and 10 everywhere else, you could simply append `<bottom>5</bottom>` into the `padding` element

5. Surfaces work in much the same way as they do in code. The only difference is that they automatically chain - if you were to add `<panel/>` before the `<vanilla-translucent/>` you'd get a vanilla panel under a dark rectangle

6. This `<text>` element denotes what the button says - you could also use a `<text translatable="true">` here and have it be translated instead

We really encourage you to write this yourself instead of simply copying it - the autocomplete in your IDE should make it a very fast process and will also make you discover many of the other components and options available. Once you're done with this, you can open the screen again and see that it looks exactly like the end result of the previous example! But wait, there's one thing missing - the button does not do anything when clicked. 

Let's fix that by going back to our screen class' `build(...)` method. In here, you can query the button using the ID we just gave it. To do this, you call `childById(...)` on the root component. It is important to note here that IDs need not be unique within your hierarchy - this method simply returns the first matching component it comes across. For type safety reasons we also need to provide the type of component we're looking for, which, since we want the button, is `ButtonComponent.class`. After you have acquired the button this way, you can configure `onPress` like any other property:

```java
@Override
protected void build(FlowLayout rootComponent) {
    rootComponent.childById(ButtonComponent.class, "the-button").onPress(button -> {
        System.out.println("click");
    });
}
```

After reloading your game, the button will work as expected. 

## What's next
Before jumping right in building UIs, we recommend checkout out the Component Basics and Layout Basics pages as they explain some more basic concepts you should be familiar with.

![owo-ui-academy example screen](../../assets/owo/ui-academy.png)

owo-ui-academy is an interactive, in-game tutorial that teaches you the essential concepts of owo-ui.

#### Setup
Given that it is a developer tool, builds are not publicly hosted - you need to get them from [GitHub Actions](https://github.com/wisp-forest/owo-ui-academy/actions). Alternatively you may also clone the repository and run it, just like any other mod.

Once you have the mod installed, simply join a world and press the keybind helpfully noted in the little notification.

#### Using the Inspector
While playing around with owo-ui-academy, the inspector is your most important tool. It can visualize what's internally going on in the playground and also how the tutorial screens themselves are laid out. Apart from the standard inspector, you can also press [[Alt]]+[[Shift]] to bring it into *global mode*, where it will draw the debug overlay for every component on screen.

There are quite a few things to know about how components internally work and how their properties decide behavior. This page serves as a helpful reference for the most important concepts.


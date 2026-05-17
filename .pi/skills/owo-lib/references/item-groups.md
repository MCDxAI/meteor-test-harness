# Item Groups

## Table of Contents

    - [Adding tabs](#adding-tabs)
    - [Adding buttons](#adding-buttons)
    - [Configuring the Item Group further](#configuring-the-item-group-further)
      - [Using a custom texture](#using-a-custom-texture)
      - [Configuring stacking height](#configuring-stacking-height)
      - [Making the title static](#making-the-title-static)
    - [Initializing the group](#initializing-the-group)
    - [Using custom stack generators](#using-custom-stack-generators)
  - [API Reference](#api-reference)
  - [Table of Contents](#table-of-contents)
  - [Package: `io.wispforest.owo.itemgroup`](#package)
    - [`class` OwoItemGroup](#owoitemgroup)
      - [`interface` ButtonDefinition](#buttondefinition)
    - [`interface` Icon](#icon)
    - [`interface` ButtonDefinition](#buttondefinition)
  - [Package: `io.wispforest.owo.itemgroup.gui`](#package)
    - [`class` ItemGroupButton](#itemgroupbutton)
    - [`record` ItemGroupTab](#itemgrouptab)

The Item Group API allows creating creative mode tabs with custom textures, sub-tabs, and buttons (with defaults for linking to GitHub, Modrinth, etc.). Applicable for Minecraft 1.19.3+.

This guide is only applicable for Minecraft versions 1.19.3 and onwards. There were significant changes to the item group system which mandated changes in owo's approach to stay compatible.

Creating a basic owo item group is easy - begin by calling `OwoItemGroup.builder(...)` and supply it with:

 - The identifier to register your group with
 - A function that creates the icon of your group - this is called at a later stage during the initialization process and should use one of the `Icon.of(...)` overloads

```java
public static final OwoItemGroup GROUP = OwoItemGroup
    .builder(new Identifier("mod-id", "item_group"), () -> Icon.of(Mod.ITEM))
    // additional builder configuration goes between these lines
    .build();
```

### Adding tabs
In order to add a tab to your item group, extend your group builder configuration with the `intializer(...)` method. Inside it, call `group.addTab(...)` - this accepts four parameters:

| Parameter      | Description                                                                                                                                              |
|----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `icon`         | The icon of this tab. Look at the different methods available on the `Icon` class, as this supports more than just items                                 |
| `name`         | The name of this tab, used for the translation key                                                                                                       |
| `contentTag` ⠀ | The item tag used for populating the content of this tab. If you wish to populate your tabs in code using `OwoItemSettings.tab(...)`, this may be `null` |
| `primary`      | If this is `true`, the tab's name is displayed as-is, otherwise the name of the item group itself gets prepended                                         |

If you want or need more precise control over how the tab is populated, you can use the `group.addCustomTab(...)` function instead. Instead of a tag, it takes a `ContentSupplier` function that is called and provided the `ItemGroup.Entries` to append to when the creative inventory is initialized

### Adding buttons
Buttons work in much the same way as tabs, except that they usually require fewer parameters. Given that most of the time you would want to link to some external resource, `ItemGroupButton.link(...)` and the related methods like `ItemGroupButton.modrinth(...)` should be of interest. You can then pass this button directly into the `group.addButton(...)` method.

If you want your button to execute a custom action, simply call the constructor directly. It accepts the following parameters:

| Parameter | Description                                                         |
|-----------|---------------------------------------------------------------------|
| `group`   | The item group the button belongs to, usually just `group`          |
| `icon`    | The icon of this button, this works identically as it does for tabs |
| `name`    | The name of this button, used for the translation key               |
| `action`  | The action to run when this button is pressed                       |

### Configuring the Item Group further

#### Using a custom texture
In order to change the texture used to render your item group, insert `customTexture(...)` with the ID of the texture you want to use. This texture needs to follow a specific format in order to look correct, for this you can use the [template included in the testmod](https://github.com/wisp-forest/owo-lib/blob/1.19.3/src/testmod/resources/assets/uwu/textures/gui/group.png).

#### Configuring stacking height
The buttons to either side of your item group (that is, tabs on the left and buttons on the right), usually arrange themselves to stack up to a height of 4. Depending on how many tabs or buttons you have, it may make sense to change this. To do so, insert either `tabStackHeight(...)` or `buttonStackHeight(...)`.

#### Making the title static
You can insert `disableDynamicTitle()` to force the title of your item group to be static, instead of changing with the tab that is selected.

### Initializing the group
After all your items have been registered, it is very important that you call the `initialize()` method on your item group. This will run setup and create the group icon, both of which can cause trouble if executed earlier.

### Using custom stack generators
When using `OwoItemSettings` instead of the vanilla `Item.Settings` to configure your item's tabs, you may have noticed the `stackGenerator(...)` setter. Using this, you can change the function which appends your item to the item group. The default function will just call `getDefaultStack()` on your item, which may not always be sufficient. Particularly if you want variations of your item with different NBT data, changing this may prove useful

## API Reference

## Table of Contents

- [OwoItemGroup](#owoitemgroup) — `io.wispforest.owo.itemgroup`
- [ButtonDefinition](#buttondefinition) — `io.wispforest.owo.itemgroup`
- [ItemGroupButton](#itemgroupbutton) — `io.wispforest.owo.itemgroup.gui`
- [ItemGroupTab](#itemgrouptab) — `io.wispforest.owo.itemgroup.gui`

## Package: `io.wispforest.owo.itemgroup`

### `class` OwoItemGroup

> Extensions for  {@link CreativeModeTab} which support multiple sub-tabs within, as well as arbitrary buttons with defaults provided for links to places like GitHub, Modrinth, etc.
>
> Tabs can be populated by setting the {@link OwoItemSettingsExtension#tab(int)}. Furthermore, tags can be used for easily populating tabs from data
>
> The roots of this implementation originated in Biome Makeover, where it was written by Lemonszz
>

#### `interface` ButtonDefinition

> Defines a button's appearance and translation key
>
> Used by {@link ItemGroupButtonWidget}
>

**Fields:**

- `interface ButtonDefinition` — Defines a button's appearance and translation key <p> Used by {@link ItemGroupButtonWidget}

**Methods:**

- `Builder builder(Identifier id, Supplier<Icon> iconSupplier)`
- `void initialize()`
  > Executes {@link #initializer} and makes sure this item group is ready for use <p> Call this after all of your items have been registered to make sure your icons show up correctly
- `void addButton(ItemGroupButton button)`
  > Adds the specified button to the buttons on the right side of the creative menu @param button The button to add @see ItemGroupButton#link(CreativeModeTab, Icon, String, String) @see ItemGroupButton#curseforge(CreativeModeTab, String) @see ItemGroupButton#discord(CreativeModeTab, String)
- `void addTab(Icon icon, String name, @Nullable TagKey<Item> contentTag, Identifier texture, boolean primary)`
  > Adds a new tab to this group @param icon       The icon to use @param name       The name of the tab, used for the translation key @param contentTag The tag used for filling this tab @param texture    The texture to use for drawing the button @see Icon#of(ItemLike)
- `void addTab(Icon icon, String name, @Nullable TagKey<Item> contentTag, boolean primary)`
  > Adds a new tab to this group, using the default button texture @param icon       The icon to use @param name       The name of the tab, used for the translation key @param contentTag The tag used for filling this tab @see Icon#of(ItemLike)
- `void addCustomTab(Icon icon, String name, ItemGroupTab.ContentSupplier contentSupplier, Identifier texture, boolean primary)`
  > Adds a new tab to this group, using the default button texture @param icon            The icon to use @param name            The name of the tab, used for the translation key @param contentSupplier The function used for filling this tab @param texture         The texture to use for drawing the button @see Icon#of(ItemLike)
- `void addCustomTab(Icon icon, String name, ItemGroupTab.ContentSupplier contentSupplier, boolean primary)`
  > Adds a new tab to this group @param icon            The icon to use @param name            The name of the tab, used for the translation key @param contentSupplier The function used for filling this tab @see Icon#of(ItemLike)
- `void buildContents(ItemDisplayParameters context)`
- `void collectItemsFromRegistry(Output entries, int tab)`
- `void selectSingleTab(int tab, ItemDisplayParameters context)`
  > Select only {@code tab}, deselecting all other tabs, using {@code context} for re-population
- `void selectTab(int tab, ItemDisplayParameters context)`
  > Select {@code tab} in addition to other currently selected tabs, using {@code context} for re-population. <p> If this group does not allow multiple selection, behaves like {@link #selectSingleTab(int, ItemDisplayParameters)}
- `void deselectTab(int tab, ItemDisplayParameters context)`
  > Deselect {@code tab} if it is currently selected, using {@code context} for re-population. If this results in no tabs being selected, all tabs are automatically selected instead
- `void toggleTab(int tab, ItemDisplayParameters context)`
  > Shorthand for {@link #selectTab(int, ItemDisplayParameters)} or {@link #deselectTab(int, ItemDisplayParameters)}, depending on the tabs current state
- `IntSet selectedTabs()`
  > @return A set containing the indices of all currently selected tabs
- `boolean isTabSelected(int tab)`
  > @return {@code true} if {@code tab} is currently selected
- `boolean hasDynamicTitle()`
- `boolean shouldDisplaySingleTab()`
- `boolean canSelectMultipleTabs()`
- `Icon icon()`
- `boolean shouldDisplay()`
- `Builder initializer(Consumer<OwoItemGroup> initializer)`
- `Builder tabStackHeight(int tabStackHeight)`
- `Builder buttonStackHeight(int buttonStackHeight)`
- `Builder backgroundTexture(@Nullable Identifier backgroundTexture)`
- `Builder scrollerTextures(ScrollerTextures scrollerTextures)`
- `Builder tabTextures(TabTextures tabTextures)`
- `Builder disableDynamicTitle()`
- `Builder displaySingleTab()`
- `Builder withoutMultipleSelection()`
- `OwoItemGroup build()`
- `void accept(ItemStack stack, TabVisibility visibility)`
- `record ScrollerTextures(Identifier enabled, Identifier disabled)`
- `record TabTextures(Identifier topSelected, Identifier topSelectedFirstColumn, Identifier topUnselected, Identifier bottomSelected, Identifier bottomSelectedFirstColumn, Identifier bottomUnselected)`

### `interface` Icon

> An icon used for rendering on buttons in {@link OwoItemGroup}s. Default implementations provided for textures and item stacks
>

**Methods:**

- `static Icon of(ItemStack stack)`
  > Creates an icon that renders the given item stack
- `static Icon of(ItemLike item)`
  > Creates an icon that renders the given item
- `static Icon of(Identifier texture, int u, int v, int textureWidth, int textureHeight)`
  > Creates an icon that renders a 16×16 region from the given texture @param texture       The texture identifier @param u             The u coordinate of the region @param v             The v coordinate of the region @param textureWidth  The total width of the texture @param textureHeight The total height of the texture
- `static Icon of(Identifier texture, int textureSize, int frameDelay, boolean loop)`
  > Creates an animated icon from a spritesheet @param texture     The spritesheet texture @param textureSize The size of the texture (assumed square) @param frameDelay  The delay in milliseconds between frames @param loop        Whether the animation should loop

### `interface` ButtonDefinition

> Defines a button's appearance and translation key
>
> Used by {@link ItemGroupButtonWidget}
>

## Package: `io.wispforest.owo.itemgroup.gui`

### `class` ItemGroupButton

> A button placed to the right side of the creative inventory. Provides defaults for linking to sites, but can execute arbitrary actions
>

**Methods:**

- `ItemGroupButton github(CreativeModeTab group, String url)`
- `ItemGroupButton modrinth(CreativeModeTab group, String url)`
- `ItemGroupButton curseforge(CreativeModeTab group, String url)`
- `ItemGroupButton discord(CreativeModeTab group, String url)`
- `ItemGroupButton link(CreativeModeTab group, Icon icon, String name, String url)`
  > Creates a button that opens the given link when clicked @param icon The icon for this button to use @param name The name of this button, used for the translation key @param url  The url to open @return The created button
- `Identifier texture()`

### `record` ItemGroupTab

> Represents a tab inside an {@link OwoItemGroup} that contains all items in the passed {@code contentTag}. If you want to use {@link OwoItemSettingsExtension#tab(int)} to define the contents, use {@code null} as the tag
>

**Methods:**

- `Component tooltip()`

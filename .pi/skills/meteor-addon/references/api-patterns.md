# Meteor API Patterns

Common patterns for Meteor Client addon development.

## Addon Entry Point

Every addon extends `MeteorAddon` and is loaded via the `"meteor"` entrypoint in `fabric.mod.json`.

```java
package com.example.myaddon;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAddon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("My Addon");

    @Override
    public void onInitialize() {
        LOG.info("Initializing My Addon");

        // Register persistent systems
        Systems.add(new MyConfig());

        // Register GUI tab
        Tabs.add(new MyTab());

        // Register commands
        // Commands.add(new MyCommand());

        LOG.info("My Addon initialized successfully");
    }

    @Override
    public void onRegisterCategories() {
        // Register custom module categories if needed
        // Categories.register(new Category("My Category"));
    }

    @Override
    public String getPackage() {
        return "com.example.myaddon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("owner", "repo-name");
    }
}
```

## Systems Pattern

Systems are persistent singletons with automatic NBT serialization. They survive game restarts.

```java
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.nbt.NbtCompound;

public class MyConfig extends System<MyConfig> {
    // Use Meteor settings for auto-serializing config values
    public final Setting<String> apiKey = stringSetting()
        .name("api-key")
        .description("Your API key")
        .defaultValue("")
        .build();

    public final Setting<Boolean> enabled = boolSetting()
        .name("enabled")
        .description("Whether the addon is active")
        .defaultValue(true)
        .build();

    public MyConfig() {
        super("my-config");
    }

    // Access singleton
    public static MyConfig get() {
        return Systems.get(MyConfig.class);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("apiKey", apiKey.get());
        tag.putBoolean("enabled", enabled.get());
        return tag;
    }

    @Override
    public MyConfig fromTag(NbtCompound tag) {
        apiKey.set(tag.getString("apiKey"));
        enabled.set(tag.getBoolean("enabled"));
        return this;
    }
}
```

### Simple registration

```java
// In onInitialize():
Systems.add(new MyConfig());

// Anywhere else:
MyConfig config = MyConfig.get();
```

## Modules

Modules are toggleable features organized by categories.

```java
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;

public class MyModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> exampleBool = sgGeneral.add(new BoolSetting.Builder()
        .name("example")
        .description("An example setting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> exampleInt = sgGeneral.add(new IntSetting.Builder()
        .name("count")
        .description("How many.")
        .defaultValue(5)
        .min(1)
        .max(100)
        .build()
    );

    public MyModule() {
        super(Category.Combat, "my-module", "Description of the module.");
    }

    @Override
    public void onActivate() {
        // Called when module is enabled
    }

    @Override
    public void onDeactivate() {
        // Called when module is disabled
    }
}
```

Auto-discovery: Place modules in your addon package. Meteor scans the package returned by `getPackage()` for `Module` subclasses.

## Commands

```java
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

public class MyCommand extends Command {
    public MyCommand() {
        super("mycommand", "Description of the command.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            info("Hello from my command!");
            return SINGLE_SUCCESS;
        });
    }
}
```

Registration: `Commands.add(new MyCommand())` in `onInitialize()`.

For dynamic commands (added/removed at runtime), use `Commands.DISPATCHER` directly.

## Event Handling

```java
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.SomeEvent;
import meteordevelopment.orbit.EventHandler;

// Subscribe
MeteorClient.EVENT_BUS.subscribe(this);

// Unsubscribe
MeteorClient.EVENT_BUS.unsubscribe(this);

// Handle events
@EventHandler
private void onSomeEvent(SomeEvent event) {
    // Handle event
}
```

Common event packages:
- `meteordevelopment.meteorclient.events.entity` — Entity events
- `meteordevelopment.meteorclient.events.game` — Game lifecycle events
- `meteordevelopment.meteorclient.events.packets` — Packet events
- `meteordevelopment.meteorclient.events.render` — Render events
- `meteordevelopment.meteorclient.events.world` — World events

## HUD Elements

```java
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.Hud;

public class MyHudElement extends HudElement {
    public static final HudElementInfo<MyHudElement> INFO = new HudElementInfo<>(
        MyAddon.class, "my-element", "Description.", MyHudElement::new
    );

    private final Setting<Boolean> example = settings.getDefaultGroup().add(new BoolSetting.Builder()
        .name("example")
        .defaultValue(true)
        .build()
    );

    public MyHudElement() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(renderer.textWidth("Hello"), renderer.textHeight());
        renderer.text("Hello", getX(), getY(), Color.WHITE, true);
    }
}
```

## Settings Framework

Meteor provides 30+ setting types. All are constructed via the Builder pattern:

```java
// Common settings
BoolSetting.Builder    → .defaultValue(true/false)
IntSetting.Builder     → .defaultValue(0).min(0).max(100).sliderMax(100)
DoubleSetting.Builder  → .defaultValue(0.0).min(0.0).max(1.0).decimalPlaces(2)
StringSetting.Builder  → .defaultValue("text")
EnumSetting.Builder    → .defaultValue(MyEnum.VALUE)
ColorSetting.Builder   → .defaultValue(new SettingColor(255, 0, 0))

// Collection settings
StringListSetting.Builder     → .defaultValue(List.of("a", "b"))
BlockListSetting.Builder      → .defaultValue(new BlockList())
ItemListSetting.Builder       → .defaultValue(new ItemList())
EntityTypeListSetting.Builder → .defaultValue(new EntityTypeList())

// Special settings
KeybindSetting.Builder   → .defaultValue(Keybind.none())
BlockPosSetting.Builder  → .defaultValue(new BlockPos(0, 0, 0))
FontFaceSetting.Builder  → .defaultValue(FontFace.DEFAULT)
```

### Settings are reactive

```java
private final Setting<Integer> myValue = sgGeneral.add(new IntSetting.Builder()
    .name("my-value")
    .defaultValue(10)
    .onChanged(v -> {
        // Called when user changes the value
        LOG.info("Value changed to {}", v);
    })
    .build()
);
```

## Threading Patterns

### Background work with render-thread callbacks

```java
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import static meteordevelopment.meteorclient.MeteorClient.mc;

// Execute network/IO off the render thread
MeteorExecutor.execute(() -> {
    String result = httpClient.fetch(url);

    // Update GUI on render thread
    mc.execute(() -> {
        // Safe to interact with Minecraft GUI
        screen.updateResult(result);
    });
});
```

### Common mistake: blocking render thread

```java
// WRONG — will freeze the game
public void onActivate() {
    String data = httpClient.fetch("https://example.com"); // BLOCKS!
}

// CORRECT — async with cached results
private String cachedResult = "Loading...";

public void onActivate() {
    MeteorExecutor.execute(() -> {
        cachedResult = httpClient.fetch("https://example.com");
    });
}
```

## GUI Patterns

### Tab registration

```java
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.GuiTheme;
import net.minecraft.client.gui.screen.Screen;

public class MyTab extends Tab {
    public MyTab() {
        super("My Tab");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new MyTabScreen(theme, this);
    }

    private static class MyTabScreen extends TabScreen {
        public MyTabScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        public void initWidgets() {
            // Add widgets here
            add(theme.label("Hello!")).centerX();
        }
    }
}
```

### Widget lifecycle (CRITICAL)

The `theme` field is set by the framework after the widget is added to the tree:

```java
// CORRECT
public class MyWidget extends WVerticalList {
    private final Data data;

    public MyWidget(Data data) {
        this.data = data;
        // Do NOT call init() here!
    }

    @Override
    public void init() {
        // Framework calls this with theme available
        add(theme.label("text")).centerX();
    }
}

// WRONG — causes "theme is null" NPE
public MyWidget(GuiTheme theme, Data data) {
    this.theme = theme;  // NEVER do this
    init();              // NEVER do this
}
```

### Screen navigation

```java
// Open a screen from within the game
mc.setScreen(new MyScreen(GuiThemes.get()));

// Open a screen from background thread
mc.execute(() -> mc.setScreen(new MyScreen(GuiThemes.get())));
```

## StarScript Integration

Register custom functions accessible in HUD elements, chat macros, and Discord presence:

```java
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import org.meteordev.starscript.value.Value;
import org.meteordev.starscript.value.ValueMap;

// Register a namespace
ValueMap myMap = new ValueMap();
myMap.set("myFunction", (ss, args) -> Value.string("result"));
MeteorStarscript.ss.set("myAddon", myMap);

// Now usable anywhere StarScript is supported:
// {myAddon.myFunction()}
```

## Utility Classes

Meteor provides many utility classes — prefer these over writing your own:

- **EntityUtils**: Entity filtering, targeting, damage calculations
- **TargetUtils**: Target selection with priority sorting
- **ChatUtils**: Formatted chat messages (`info()`, `warning()`, `error()`, `prefix()`)
- **BlockUtils**: Block queries, placement, breaking
- **RenderUtils**: Rendering helpers, frustum culling
- **PlayerUtils**: Player state queries
- **WorldUtils**: World interaction helpers
- **FindUtils**: Finding blocks/entities in range

### Cross-platform URL opening

```java
import net.minecraft.util.Util;
// Opens URL in default browser
Util.getOperatingSystem().open(url);
```

This is Minecraft's utility — prefer it over Java's `Desktop.browse()`.

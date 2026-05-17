---
name: owo-lib
description: >
  Comprehensive documentation for oωo (owo-lib), a general utility, GUI, and config library
  for Fabric/Quilt Minecraft modding. Use when working with owo-lib features including:
  owo-ui declarative UI framework, owo-config annotation-driven configuration, automatic
  registration system, networking with Endec serialization, item group extensions, particle
  effects, data extensions (JSON5, rich translations, nested lang), and utility APIs.
  Covers all public APIs with wiki tutorials and JavaDoc references.
---

# oωo-lib (owo-lib)

oωo is a general-purpose utility, GUI, and configuration library for Fabric and Quilt Minecraft modding (NeoForge also supported). It provides modular, interrelated systems that reduce boilerplate while offering full control.

- **GitHub**: https://github.com/wisp-forest/owo-lib
- **Wiki**: https://docs.wispforest.io/owo/features
- **License**: MIT
- **Minecraft versions**: 1.17.x through 1.21.x+

## Setup Quick Reference

Add the oωo maven and dependency to `build.gradle`:

```groovy
repositories {
    maven { url = "https://maven.wispforest.io" }
}

dependencies {
    modImplementation "io.wispforest:owo-lib:${project.owo_version}"
    annotationProcessor modImplementation("io.wispforest:owo-lib:${project.owo_version}")
    include "io.wispforest:owo-sentinel:${project.owo_version}"
}
```

In `gradle.properties`:
```properties
# https://maven.wispforest.io/io/wispforest/owo-lib/
owo_version=...
```

owo-sentinel is a tiny companion mod that warns players if oωo is missing. Include it via Jar-in-Jar.

For NeoForge, multi-module setup, and system properties (debug mode, RenderDoc), see `references/setup-and-dev/` (start with `index.md`).

## Architecture Overview

oωo's modules are layered and interdependent:

```
registration          networking          particles
    │                     │                   │
    │                uses Endec ──────────────┘
    │                     │
    ▼                     ▼
utilities            item-groups
                          
config ──uses──► owo-ui (declarative UI framework)
```

- **owo-config** generates config screens using **owo-ui** (XML customization, hot-reload in dev)
- **Networking** uses **Endec** for format-agnostic serialization (binary for packets, JSON/NBT for storage)
- **Particles** networking uses the same Endec + OwoNetChannel infrastructure
- **Registration** is standalone — field-based auto-registration via container classes
- **Data extensions** (JSON5, nested lang, rich translations) are standalone data-loading features
- **Utilities** are independent helpers (ItemOps, LevelOps, TagInjector, etc.)

## Feature Index

| Feature | Reference | When to Load |
|---------|-----------|--------------|
| Setup & Dev | `references/setup-and-dev/` | Gradle setup, NeoForge config, debug properties, RenderDoc |
| UI Framework (owo-ui) | `references/ui/` | Building screens, components, layouts, XML templates, HUD, layers |
| Configuration (owo-config) | `references/config/` | Creating config classes, annotations, constraints, sync, ModMenu |
| Registration | `references/registration/` | Auto-registering items/blocks/entities via field containers |
| Networking | `references/networking/` | OwoNetChannel, packets (clientbound/serverbound), handshaking |
| Endec (Serialization) | `references/endec/` | Building custom endecs, struct serialization, Codec interop |
| Item Groups | `references/item-groups.md` | Creative tabs with sub-tabs, buttons, custom textures (1.19.3+) |
| Particles | `references/particles.md` | ClientParticles utilities, ParticleSystem networking API |
| Data Extensions | `references/data-extensions/` | JSON5 support, nested lang files, rich translations |
| Utilities | `references/utilities/` | ItemOps, LevelOps, TextOps, TagInjector, screen helpers, commands |

## Common Patterns

### 1. Automatic Registration

```java
// Define items in a container class
public class ItemInit implements ItemRegistryContainer {
    public static final Item MY_ITEM = new Item(new Item.Settings());
}

// Register in your mod initializer
public class MyMod implements ModInitializer {
    public static final String MOD_ID = "mymod";
    @Override
    public void onInitialize() {
        FieldRegistrationHandler.register(ItemInit.class, MOD_ID, false);
    }
}
```

### 2. Simple UI Screen

```java
public class MyScreen extends BaseOwoScreen<OwoUIAdapter<FlowLayout>> {
    @Override
    protected OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, FlowLayout::new);
    }

    @Override
    protected void build(OwoUIAdapter<FlowLayout> adapter) {
        adapter.rootComponent.child(
            Components.label(Text.literal("Hello oωo!"))
        );
    }
}
```

For data-driven XML layouts, extend `BaseUIModelScreen` and load from `assets/<modid>/owo-ui/<name>.xml`. The component inspector (press `B` in dev mode) enables live editing.

### 3. Config Class

```java
@Config(name = "my-config", wrapperName = "MyConfig")
public class MyConfigModel {
    public int maxItems = 64;
    public boolean enableFeature = true;

    @RangeConstraint(min = 0, max = 100)
    public int difficulty = 50;

    @Nest
    public GuiSettings gui = new GuiSettings();

    public static class GuiSettings {
        public boolean showTooltips = true;
    }
}
```

Build the project to generate `MyConfig`, then load it:
```java
public static final MyConfig CONFIG = MyConfig.createAndLoad();
```

Config screens are auto-generated via owo-ui. Register with ModMenu:
```java
ModMenuApi.registerConfigScreen("mymod", MyConfig::screen);
```

## File Index

```
references/
├── setup-and-dev/       # Gradle setup (Fabric/NeoForge), system properties, debug features, feature overview
├── ui/                  # owo-ui: components, layouts, XML templates, sizing, positioning, HUD, inspector, animations
├── config/              # owo-config: annotations, constraints, nesting, sync, ModMenu, option API
├── registration/        # Auto-registration: container classes, annotations, block items, non-registry targets
├── networking/          # OwoNetChannel: packet records, clientbound/serverbound, deferred registration, handshaking
├── endec/               # Endec framework: data model, building endecs, structs, Codec interop, format-agnostic I/O
├── item-groups.md       # OwoItemGroup: builder API, tabs, buttons, custom textures, stack generators
├── particles.md         # ClientParticles (client utilities) + ParticleSystem (networked server-triggered effects)
├── data-extensions/     # JSON5 loading, nested lang files, rich translations with Text Components
└── utilities/           # ItemOps, LevelOps, LootOps, TextOps, TagInjector, screen utils, block entity processes, commands
```

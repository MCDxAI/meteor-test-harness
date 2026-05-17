### Properties

#### `-Dowo.handshake.disable <true|false>`

Completely disables oωo's handshaking procedure, thereby allowing you to join servers which would otherwise be wrongly detected as incompatible. If the handshaking channel can still be opened, like if you don't have a proxy or are using the [oωo velocity plugin](https://github.com/BasiqueEvangelist/OwoVelocityPlugin), the server's setting is authoritative and even a client with handshake enabled will still be able to join. If the channel is unavailable for any reason, both the client *and* server need to have this flag set to `true` in order to connect successfully.

---

#### `-Dowo.debug <true|false>`

Enables oωo's debug mode. In this mode, oωo itself registers a number of additional commands helpful for debugging purposes and tweaks the game's behavior in some other ways to ease development. This state of this flag is part of the API surface as well, so expect other mods that use oωo to toggle their own debug features based on it.

---

#### `-Dowo.forceDisableDebug <true|false>`

Only available in development environments. Because debug mode is enabled by default in those, you can use this flag to toggle it off in case you need to test something without debug features enabled.

---

#### `-Dowo.sentinel.forceHeadless <true|false>`

Forces oωo-sentinel to always use its command-line-based mode instead of opening a window, even if a graphical desktop environment is detected

oωo includes a comprehensive wrapper for the [RenderDoc](https://renderdoc.org) API. RenderDoc is an open-source graphics debugger that helps you understand and trace bugs in rendering code. To set it up for use with oωo's API wrapper, follow the instructions for your OS:

## Linux
In order to load the RenderDoc dynamic library you need to run your game with the `LD_PRELOAD` environment variable pointing to your copy of `librenderdoc.so`. 

If you installed RenderDoc from a package manager this is usually located in `/usr/lib/`, if you simply extracted the official archive it'll be in the `lib` subdirectory. Once you have the full path to the library, assign it to the environment variable. 

If you're using IntelliJ, you can do this by editing your run configuration and adding something like `LD_PRELOAD=/home/user/renderdoc/lib/librenderdoc.so` to the "Environment Variables" field.

## Windows
To load the RenderDoc dynamic library on Windows you first need to locate renderdoc.dll. In the official ZIP distribution you can find it directly in the root of the archive. Once you have the full path to the library, assign it to the `-Dowo.renderdocPath` Java system property. 

In IntelliJ, you can do this by editing your run configuration and adding something like `-Dowo.renderdocPath="C:\Users\user\renderdoc\renderdoc.dll"` to the "VM Options" field (the second one from the top).

### In-game UI

If RenderDoc is detected while running in debug mode, owo's `/renderdoc` command will be made available. This command opens a screen that you can use to configure the RenderDoc overlay as well as to trigger captures and, most importantly, open the RenderDoc replay UI. This is especially useful as it means you can always have RenderDoc injected while developing and do a capture and analyze it on-demand.

![renderdoc configuration screen](../assets/owo/renderdoc-compat-screen.png)

### owo-ui integration

When running in debug mode with RenderDoc injected, owo-ui screens expose an additional hotkey you may use to create a capture of only the UI. This is quite convenient as it means the capture is significantly less bulky and only includes the draw calls you care about when debugging your screen.

To create a capture, press [[Ctrl]]+[[Alt]]+[[R]]


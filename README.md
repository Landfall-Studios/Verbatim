<p align="center">
  <img src="ICON.png" alt="Verbatim" width="128" />
</p>

<h1 align="center">Verbatim</h1>

<p align="center">
  A cross-platform server-side chat channel system with configurable channels, direct messaging, distance-based local chat, and Discord integration.
</p>

<p align="center">
  <a href="USAGE.md">Usage Guide</a>
</p>

## Architecture

Verbatim uses a **service locator with adapter pattern** to run the same business logic across multiple game platforms. All shared logic depends on platform-independent interfaces; platform entry points wire concrete implementations at startup.

### Service Locator

`Verbatim.java` holds static references to all platform services:

```
Verbatim.gameContext        -> GameContext
Verbatim.gameConfig         -> GameConfig
Verbatim.chatFormatter      -> ChatFormatter
Verbatim.channelFormatter   -> ChannelFormatter
Verbatim.permissionService  -> PermissionService
Verbatim.prefixService      -> PrefixService
Verbatim.LOGGER             -> Logger (SLF4J)
```

### Core Interfaces

All in `world.landfall.verbatim.context`:

| Interface | Purpose |
|-----------|---------|
| `GameContext` | Server operations, player retrieval, messaging, persistent data, permissions, component creation |
| `GamePlayer` | Player abstraction (username, display name, UUID, name components) |
| `GameComponent` | Styled text with color (named + arbitrary RGB), bold, italic, underline, click/hover events |
| `GameCommandSource` | Command executor abstraction with permission checks |
| `ChatFormatter` | Color code parsing (`&X` and `&#RRGGBB`), player name formatting (including favorite gradient), link detection |
| `GameConfig` | Configuration access (channels, Discord settings, join/leave messages) |
| `GameColor` | Enum of 16 standard colors with formatting codes and RGB values |
| `GameText` | Static helper (`text("Hello").withColor(GameColor.GREEN)`) |

### Service Base Classes

In `world.landfall.verbatim.util`:

| Class | Purpose |
|-------|---------|
| `PermissionService` | Abstract. LuckPerms -> native permissions -> permission level fallback chain |
| `PrefixService` | Abstract. Player prefix/group data from LuckPerms. Includes `NoPrefixService` no-op default |

### Special Channels

In `world.landfall.verbatim.specialchannels`:

| Class | Purpose |
|-------|---------|
| `ChannelFormatter` | Interface for local channel formatting and distance-based message obscuring |
| `FormattedMessageDetails` | Data class holding formatted message, effective range, roleplay flag, and distance-based delivery logic |

### Shared Business Logic

| Class | Purpose |
|-------|---------|
| `ChatEventHandler` | Processes login/logout/chat events, routes messages to channels/DMs, handles distance delivery, per-recipient favorite name rendering |
| `ChatChannelManager` | Channel config loading, player membership tracking, focus management, persistent state |
| `VerbatimCommandHandlers` | Platform-independent command implementations (`/channel`, `/msg`, `/nick`, etc.) |
| `NicknameService` | Nickname storage, validation, and formatting code filtering |
| `FormattingCodeUtils` | Utilities for stripping `&` formatting codes (including `&#RRGGBB` hex codes) |
| `DiscordBot` / `DiscordListener` | JDA-based Discord bridge |

## Platform Implementations

### Hytale (`hytale/`)

Built against the Hytale Server API (`com.hypixel.hytale.server`). Entry point: `HytaleEntryPoint extends JavaPlugin`.

| Class | Implements | Notes |
|-------|-----------|-------|
| `HytaleGameContextImpl` | `GameContext` | Uses `Universe` API. In-memory persistent data backed by JSON file |
| `HytaleGameComponentImpl` | `GameComponent` | Wraps `Message`. See platform limitations below |
| `HytaleGamePlayer` | `GamePlayer` | Wraps `PlayerRef` |
| `HytaleGameCommandSource` | `GameCommandSource` | Wraps `CommandContext` + `PlayerRef` |
| `HytaleChatFormatter` | `ChatFormatter` | Color/formatting via `Message` API. Links via `Message.link()` |
| `HytaleLocalChannelFormatter` | `ChannelFormatter` | Distance obscuring with `Message.join()` for prefix preservation |
| `HytalePermissionService` | `PermissionService` | LuckPerms (reflection) -> `PermissionsModule` -> permission level fallback |
| `HytalePrefixService` | `PrefixService` | LuckPerms (reflection) for prefix/group data |
| `HytaleGameConfig` | `GameConfig` | Delegates to `HytaleVerbatimConfig` (JSON-based) |
| `HytaleCommandRegistrar` | - | Command registration using `AbstractPlayerCommand` / `AbstractCommandCollection` |
| `HytaleChatEvents` | - | Bridges Hytale events to `ChatEventHandler` |
| `HytaleColorBridge` | - | `GameColor` -> `java.awt.Color` conversion |
| `HytaleLoggerAdapter` | `Logger` (SLF4J) | Adapts Hytale's native logger via reflection |
| `HytaleVerbatimConfig` | - | JSON config file management with defaults |

### NeoForge / Minecraft (`neoforge/`)

Built against NeoForge for Minecraft 1.21.1.

| Class | Implements | Notes |
|-------|-----------|-------|
| `NeoForgeGameContextImpl` | `GameContext` | Uses `MinecraftServer` API. NBT-based persistent data |
| `NeoForgeGameComponentImpl` | `GameComponent` | Wraps `MutableComponent`. Full hover/click event support |
| `NeoForgeGamePlayer` | `GamePlayer` | Wraps `ServerPlayer` |
| `NeoForgeChatFormatter` | `ChatFormatter` | Full Minecraft `Style` support including hover tooltips on links/names |
| `NeoForgeLocalChannelFormatter` | `ChannelFormatter` | Distance obscuring via `MutableComponent.getSiblings()` |
| `NeoForgePermissionService` | `PermissionService` | LuckPerms (reflection) -> vanilla OP level fallback |
| `NeoForgePrefixService` | `PrefixService` | LuckPerms (reflection) for prefix/group data |

## Hytale Platform Limitations

Features unavailable in Hytale's `Message` API compared to Minecraft's `MutableComponent`:

| Feature | Minecraft | Hytale | Impact |
|---------|-----------|--------|--------|
| Hover tooltips | Full support | Not in protocol | No hover text on player names, links, or prefixes |
| Click: suggest command | `ClickEvent.Action.SUGGEST_COMMAND` | Not available | Cannot click player names to suggest `/msg` |
| Click: run command | `ClickEvent.Action.RUN_COMMAND` | Not available | Cannot click to run commands |
| Click: copy to clipboard | `ClickEvent.Action.COPY_TO_CLIPBOARD` | Not available | - |
| Click: open URL | `ClickEvent.Action.OPEN_URL` | `Message.link(url)` | Supported |
| Strikethrough | `Style.withStrikethrough()` | Not in `Message` API | `&m` code ignored |
| Obfuscated | `Style.withObfuscated()` | Not in `Message` API | `&k` code ignored |
| Underline | `Style.withUnderlined()` | In `FormattedMessage` protocol but not exposed in `Message` API | `&n` code ignored |

### Behavioral Differences

- **Formatting codes and `&r`:** Only `&r` resets bold/italic/color. Color codes (`&a`, `&c`, etc.) change color without resetting bold or italic. This is intentional and differs from vanilla Minecraft behavior where color codes reset all formatting.
- **`broadcastMessage`:** The `bypassHiddenPlayers` flag is ignored on Hytale; all online players receive broadcasts.
- **LuckPerms access:** Both platforms use reflection to access LuckPerms. Hytale's `loadUser()` calls have a 2-second timeout; NeoForge's do not.
- **Persistent data:** Hytale uses an in-memory `ConcurrentHashMap` persisted to a JSON file on shutdown. NeoForge uses Minecraft's NBT `PersistentData` which saves incrementally.
- **Hytale command parsing:** Hytale's `ArgTypes.STRING` captures only a single word. Multi-word arguments (e.g., `/msg Player hello world`) are extracted from `CommandContext.getInputString()` via helper methods in `HytaleCommandRegistrar`.

## Building

Each module is an independent Gradle project with its own wrapper. Build from within each directory:

### Core (shared logic + tests)

```bash
cd core && ./gradlew build
```

### Hytale

Requires a local copy of `HytaleServer.jar`. Set the path in `hytale/gradle.properties`:

```properties
hytale_server_jar=/path/to/HytaleServer.jar
```

```bash
cd hytale && ./gradlew build   # Produces shadow jar in build/libs/
```

### NeoForge

```bash
cd neoforge && ./gradlew build   # Produces mod jar with core via JarJar
```

## Adding a New Platform

1. Create a new directory (e.g., `forge-1.20.1/`) with its own `build.gradle`, `settings.gradle` (with `includeBuild '../core'`), and Gradle wrapper.
2. Create `platform/<name>/` package under `src/main/java/world/landfall/verbatim/`.
3. Implement all interfaces: `GameContext`, `GamePlayer`, `GameCommandSource`, `GameComponent`, `ChatFormatter`, `ChannelFormatter`.
4. Extend `PermissionService` and `PrefixService` (or use `NoPrefixService`).
5. Implement `GameConfig` for your config format.
6. Create an entry point that wires all implementations into the `Verbatim` service locator.
7. Register event handlers that delegate to `ChatEventHandler`.
8. Register commands that delegate to `VerbatimCommandHandlers`.
9. Add the new module to the root `settings.gradle` for IDE discovery.

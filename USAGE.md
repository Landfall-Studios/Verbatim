# Verbatim - User Guide

Verbatim is a server-side chat plugin providing configurable chat channels, direct messaging, Discord integration, and more. It is designed for community and roleplay-focused servers.

## Features

*   **Customizable Chat Channels:**
    *   Multiple chat channels with unique names, display prefixes (e.g., `&a[G]`), and shortcuts (e.g., `g`).
    *   **Permissions:** Secure channels with permission nodes (integrates with LuckPerms if available, otherwise uses native permission systems).
    *   **Chat Range:** Per-channel chat ranges (e.g., for local channels) or global reach.
    *   **Color Customization:** Configurable colors for channel names, player names, separators, and messages.
    *   **Always-On Channels:** Channels that players are always in and cannot leave.
    *   **Mature Content Warnings:** Optional warning when players join channels marked as "mature."
    *   **Special Channel Types:**
        *   `local`: Distance-based message fading/obscuring with suffix-based behavior. (Inspired by [MassiveChat](https://www.massivecraft.com/documentation/massivechat-aliases-quick-commands/)!)
*   **Direct Messaging (DM):**
    *   Send private messages to other online players.
    *   Quick reply to the last incoming DM.
*   **Chat Prefixes & Shortcuts:**
    *   Use prefixes to quickly switch focus or send messages (e.g., `g: Hello everyone!`).
    *   `d:` prefix to focus/reply to the last DM.
*   **Discord Integration:**
    *   Bridge in-game global chat to a Discord channel and back.
    *   Customizable message prefix for Discord messages shown in-game.
*   **Persistent Player Settings:**
    *   Remembers joined channels and focused channel across sessions.
*   **Nicknames:**
    *   Set custom nicknames with optional color code support (permission-gated).

## Commands

### Channel Commands

*   `/channels` or `/channel list` - Lists all available chat channels and your status in them.
*   `/channel focus <channelName>` - Sets your active typing channel (auto-joins if needed).
*   `/channel join <channelName>` - Joins a channel to receive its messages.
*   `/channel leave [channelName]` - Leaves a channel. Omit name to leave your focused channel.
*   `/channel help` - Displays help with available commands and prefixes.

### Direct Message Commands

*   `/msg <player> [message]` or `/tell <player> [message]` - Send a DM or focus DM with a player.
*   `/r [message]` - Reply to or focus DM with the last player who messaged you.

### Other Commands

*   `/list` - Lists online players.
*   `/vlist` - Lists online players (Verbatim format).
*   `/nick [nickname]` - Set your nickname. Run without arguments to clear it.

### Admin Commands

*   `/chlist <player|channel>` - List channels for a player, or players in a channel.
*   `/chkick <player> <channel>` - Kick a player from a channel.

### Chat Prefixes

*   `g: Your message here` - Sends to the channel with shortcut "g" and focuses it.
*   `d: Your reply here` - Sends to and focuses DM with the last person who DM'd you.
*   `d:` (no message) - Focuses DM with the last person who DM'd you.

## Local Channel Suffixes

When using a `local` type channel, message suffixes change the range and style:

| Suffix | Action | Range | Example |
|--------|--------|-------|---------|
| *(none)* | says | 50 | `Hello there` |
| `!` | exclaims | 75 | `Watch out!` |
| `!!` | shouts | 100 | `Over here!!` |
| `?` | asks | 50 | `Anyone there?` |
| `*` | whispers | 10 | `Secret message*` |
| `$` | mutters | 3 | `Grumble grumble$` |
| `+` | roleplay action | 50 | `waves hello+` |
| `))` | OOC (out of character) | 50 | `brb 5 min))` |

Messages fade into obscured dots as listeners get further from the speaker, eventually becoming inaudible beyond the fade range.

## Configuration

### Minecraft (NeoForge)

Configuration is in `verbatim-server.toml` in the server's `config` directory.

### Hytale

Configuration is in `verbatim-config.json` in the plugin's data directory.

### Key Settings

*   `defaultChannelName` - The channel players default to on join.
*   `channels` - List of channel definitions (name, prefix, shortcut, permission, range, colors, etc.).
*   Discord settings: bot token, channel ID, message prefix, enable/disable.

## Permissions

Verbatim uses LuckPerms if available, falling back to native permission systems.

*   **Channel Access:** Set via the `permission` field per channel (e.g., `verbatim.channel.staff`).
*   **Chat Colors:** `verbatim.chat.color` - Allows `&` color codes in chat.
*   **Chat Formatting:** `verbatim.chat.format` - Allows `&l` (bold), `&o` (italic) in chat.
*   **Nicknames:** `verbatim.nick` - Allows `/nick` usage.
*   **Admin:** `verbatim.admin` - Admin commands (`/chlist`, `/chkick`).

### LuckPerms Prefix & Tooltip Setup

*   **Set a prefix:** `/lp user <username> meta setprefix <priority> "<prefix>"`
    *   Example: `/lp user Steve meta setprefix 100 "&c[Admin] "`
*   **Set a prefix tooltip:** `/lp user <username> meta set prefix_tooltip.0 "<text>"`
    *   Example: `/lp group admin meta set prefix_tooltip.0 "Server Administrator"`
    *   Supports `prefix_tooltip.0` through `prefix_tooltip.9` for multiple entries.

package world.landfall.verbatim;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.level.ServerPlayer;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GameComponentImpl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFormattingUtils {

    // Permission constants for chat color and formatting
    public static final String PERM_CHAT_COLOR = "verbatim.chatcolor";
    public static final String PERM_CHAT_FORMAT = "verbatim.chatformat";

    // URL detection pattern - matches http://, https://, and www. URLs
    private static final Pattern URL_PATTERN = Pattern.compile(
        "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+|www\\.[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Creates a component from text with URLs made clickable.
     *
     * @param text The text to process for URLs
     * @param baseStyle The base style to apply to non-URL text
     * @return A component with clickable URLs
     */
    public static MutableComponent makeLinksClickable(String text, Style baseStyle) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        MutableComponent result = Component.empty();
        Matcher matcher = URL_PATTERN.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            // Add text before the URL
            if (matcher.start() > lastEnd) {
                String beforeUrl = text.substring(lastEnd, matcher.start());
                result.append(Component.literal(beforeUrl).setStyle(baseStyle));
            }

            // Add the clickable URL
            String url = matcher.group();
            String clickUrl = url;

            // Ensure the URL has a protocol for the click event
            if (url.toLowerCase().startsWith("www.")) {
                clickUrl = "https://" + url;
            }

            MutableComponent urlComponent = Component.literal(url);
            urlComponent.setStyle(baseStyle
                .withColor(ChatFormatting.BLUE)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, clickUrl))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Click to open: " + clickUrl).withStyle(ChatFormatting.GRAY)))
            );
            result.append(urlComponent);

            lastEnd = matcher.end();
        }

        // Add any remaining text after the last URL
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            result.append(Component.literal(remaining).setStyle(baseStyle));
        }

        // If no URLs were found, just return the original text with base style
        if (lastEnd == 0) {
            return Component.literal(text).setStyle(baseStyle);
        }

        return result;
    }

    public static GameComponent parseColors(String text) {
        if (text == null || text.isEmpty()) {
            return GameComponentImpl.empty();
        }

        MutableComponent mainComponent = Component.literal("");
        // Split by color/formatting codes, ensuring the codes themselves are captured for processing.
        // This regex splits the string by looking ahead for an ampersand followed by a valid code character.
        String[] parts = text.split("(?i)(?=&[0-9a-fk-or])");

        Style currentStyle = Style.EMPTY;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&") && part.length() >= 2) {
                char code = part.charAt(1);
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                String textContent = part.substring(2);

                if (formatting != null) {
                    if (formatting.isColor()) {
                        currentStyle = Style.EMPTY.withColor(formatting); // Reset style for new color
                    } else if (formatting == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY; // Full reset
                    } else { // It's a formatting code (bold, italic, etc.)
                        currentStyle = applyStyle(currentStyle, formatting);
                    }
                }

                if (!textContent.isEmpty()) {
                    mainComponent.append(makeLinksClickable(textContent, currentStyle));
                }
            } else {
                // No color code at the beginning of this part, append with current style
                mainComponent.append(makeLinksClickable(part, currentStyle));
            }
        }
        return GameComponentImpl.wrap(mainComponent);
    }

    // Helper to apply specific formatting to a style
    private static Style applyStyle(Style baseStyle, ChatFormatting format) {
        if (format == ChatFormatting.BOLD) return baseStyle.withBold(true);
        if (format == ChatFormatting.ITALIC) return baseStyle.withItalic(true);
        if (format == ChatFormatting.UNDERLINE) return baseStyle.withUnderlined(true);
        if (format == ChatFormatting.STRIKETHROUGH) return baseStyle.withStrikethrough(true);
        if (format == ChatFormatting.OBFUSCATED) return baseStyle.withObfuscated(true);
        return baseStyle;
    }

    /**
     * Creates a player name component that shows the display name with hover text showing the username
     * when they differ. For DMs, always uses the username without hover.
     * Now includes LuckPerms prefix/suffix support and role tooltips.
     *
     * @param player The player whose name to display
     * @param colorPrefix The color/formatting prefix to apply (e.g., "&e" or "&c&l")
     * @param isDM Whether this is for a direct message (if true, always uses username)
     * @param nameStyle The style of name to display (null for legacy behavior - use displayName for channels, username for DMs)
     * @return A component with the appropriate name and hover text
     */
    public static GameComponent createPlayerNameComponent(ServerPlayer player, String colorPrefix, boolean isDM, world.landfall.verbatim.NameStyle nameStyle) {
        String username = Verbatim.gameContext.getPlayerUsername(player);
        String displayName = Verbatim.gameContext.getPlayerDisplayName(player); // Get raw display name
        String strippedDisplayName = stripFormattingCodes(displayName); // Strip codes for comparison and potential use

        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + username + " ");
        HoverEvent hoverEvent = null;

        String nameToShow;

        if (isDM) {
            // For DMs, always show the username, apply color prefix.
            nameToShow = username;
        } else if (nameStyle != null) {
            // Use the specified name style
            nameToShow = world.landfall.verbatim.util.NicknameService.getNameForStyle(player, nameStyle);

            // Set up hover event based on the name style
            if (nameStyle == world.landfall.verbatim.NameStyle.NICKNAME) {
                String nickname = world.landfall.verbatim.util.NicknameService.getNickname(player);
                if (nickname != null && !nickname.trim().isEmpty()) {
                    hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            Component.literal("Username: " + username).withStyle(ChatFormatting.GRAY));
                }
            } else if (nameStyle == world.landfall.verbatim.NameStyle.DISPLAY_NAME && !username.equals(strippedDisplayName)) {
                hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Username: " + username).withStyle(ChatFormatting.GRAY));
            }
        } else {
            // Legacy behavior: For channels, if display name (stripped) is different from username, use it and set hover.
            if (!username.equals(strippedDisplayName)) {
                nameToShow = strippedDisplayName;
                hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Username: " + username).withStyle(ChatFormatting.GRAY));
            } else {
                // Otherwise, just use the username.
                nameToShow = username;
            }
        }

        // Get LuckPerms prefix if available
        String luckPermsPrefix = "";
        final String prefixTooltipText;

        if (Verbatim.prefixService != null && Verbatim.prefixService.isLuckPermsAvailable()) {
            luckPermsPrefix = Verbatim.prefixService.getPlayerPrefix(player);

            // Only get prefix tooltip if there's a prefix
            if (!luckPermsPrefix.isEmpty()) {
                prefixTooltipText = Verbatim.prefixService.getPrefixTooltip(player);
            } else {
                prefixTooltipText = null;
            }
        } else {
            prefixTooltipText = null;
        }

        // Build the full name component
        MutableComponent fullNameComponent = Component.empty();

        // Add LuckPerms prefix if present with its own hover tooltip
        if (!luckPermsPrefix.isEmpty()) {
            MutableComponent prefixComponent = parseColors(luckPermsPrefix).toMinecraftMutable();

            // Apply prefix tooltip only to the prefix if it exists
            if (prefixTooltipText != null && !prefixTooltipText.isEmpty()) {
                Component tooltipComponent = parseColors(prefixTooltipText).toMinecraft();
                prefixComponent = prefixComponent.withStyle(style ->
                    style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltipComponent))
                );
            }

            fullNameComponent.append(prefixComponent);

            // Add a space after prefix if it doesn't end with one
            if (!luckPermsPrefix.endsWith(" ")) {
                fullNameComponent.append(Component.literal(" "));
            }
        }

        // Parse the name with its color prefix
        MutableComponent nameComponent = parseColors(colorPrefix + nameToShow).toMinecraftMutable();

        // Apply ClickEvent and original HoverEvent (if any) only to the name component
        final HoverEvent finalHoverEvent = hoverEvent; // effectively final for lambda
        nameComponent = nameComponent.withStyle(style -> {
            Style updatedStyle = style.withClickEvent(clickEvent);
            if (finalHoverEvent != null) {
                updatedStyle = updatedStyle.withHoverEvent(finalHoverEvent);
            }
            return updatedStyle;
        });

        fullNameComponent.append(nameComponent);

        return GameComponentImpl.wrap(fullNameComponent);
    }

    /**
     * Creates a player name component with legacy behavior (for backward compatibility).
     * Uses displayName for channels and username for DMs.
     *
     * @param player The player whose name to display
     * @param colorPrefix The color/formatting prefix to apply (e.g., "&e" or "&c&l")
     * @param isDM Whether this is for a direct message (if true, always uses username)
     * @return A component with the appropriate name and hover text
     */
    public static GameComponent createPlayerNameComponent(ServerPlayer player, String colorPrefix, boolean isDM) {
        return createPlayerNameComponent(player, colorPrefix, isDM, null);
    }

    /**
     * Creates a Discord-formatted player name string based on the specified name style.
     *
     * @param player The player whose name to format for Discord
     * @param nameStyle The style of name display to use
     * @return A string formatted for Discord messages
     */
    public static String createDiscordPlayerName(ServerPlayer player, NameStyle nameStyle) {
        String username = Verbatim.gameContext.getPlayerUsername(player);

        if (nameStyle == null) {
            // Default to username only for backwards compatibility
            return username;
        }

        switch (nameStyle) {
            case USERNAME:
                // Just the username, no parentheses
                return username;

            case DISPLAY_NAME:
                String displayName = Verbatim.gameContext.getPlayerDisplayName(player);
                // Strip Minecraft formatting codes (§ codes) from display name for Discord
                String cleanDisplayName = displayName.replaceAll("§[0-9a-fk-or]", "");

                if (!username.equals(cleanDisplayName)) {
                    return cleanDisplayName + " (" + username + ")";
                } else {
                    return username;
                }

            case NICKNAME:
                String nickname = world.landfall.verbatim.util.NicknameService.getNickname(player);
                if (nickname != null && !nickname.trim().isEmpty()) {
                    // Strip formatting codes from nickname
                    String cleanNickname = stripFormattingCodes(nickname);
                    return cleanNickname + " (" + username + ")";
                } else {
                    // No nickname set, just return username
                    return username;
                }

            default:
                return username;
        }
    }

    /**
     * Creates a Discord-formatted player name string with default style (username only).
     *
     * @param player The player whose name to format for Discord
     * @return A string formatted for Discord messages
     */
    public static String createDiscordPlayerName(ServerPlayer player) {
        return createDiscordPlayerName(player, NameStyle.USERNAME);
    }

    /**
     * Strips Minecraft formatting codes (both & and §) from a string.
     * @param text The text to strip codes from.
     * @return The text with formatting codes removed.
     */
    public static String stripFormattingCodes(String text) {
        if (text == null) {
            return null;
        }
        // Regex to remove & followed by a hex char, or § followed by a hex char.
        return text.replaceAll("(?i)[&§][0-9A-FK-OR]", "");
    }

    /**
     * Parses color and formatting codes while checking the sender's permissions. Unlike
     * {@link #parseColors(String)} this method will silently ignore color/format codes that
     * the player does not have permission to use.
     *
     * This should mainly be used when formatting entire messages where the sender should have
     * full control over colors (for example when relaying to Discord). For normal in-game chat,
     * prefer {@link #parsePlayerInputWithPermissions(String, String, ServerPlayer)} which keeps
     * the channel base color intact.
     */
    public static GameComponent parseColorsWithPermissions(String text, ServerPlayer player) {
        if (text == null || text.isEmpty()) {
            return GameComponentImpl.empty();
        }

        boolean hasColorPerm  = Verbatim.permissionService.hasPermission(player, PERM_CHAT_COLOR, 2);
        boolean hasFormatPerm = Verbatim.permissionService.hasPermission(player, PERM_CHAT_FORMAT, 2);

        MutableComponent main = Component.literal("");
        String[] parts = text.split("(?i)(?=&[0-9a-fk-or])");
        Style current = Style.EMPTY;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&") && part.length() >= 2) {
                char code = part.charAt(1);
                ChatFormatting fmt = ChatFormatting.getByCode(code);
                String content = part.substring(2);

                if (fmt != null) {
                    boolean allow = false;
                    if (fmt.isColor()) {
                        allow = hasColorPerm;
                    } else if (fmt == ChatFormatting.RESET) {
                        allow = hasColorPerm || hasFormatPerm;
                    } else { // bold, italic, underline etc.
                        allow = hasFormatPerm;
                    }

                    if (allow) {
                        if (fmt.isColor()) {
                            current = Style.EMPTY.withColor(fmt);
                        } else if (fmt == ChatFormatting.RESET) {
                            current = Style.EMPTY;
                        } else {
                            current = applyStyle(current, fmt);
                        }
                    }
                }

                if (!content.isEmpty()) {
                    main.append(makeLinksClickable(content, current));
                }
            } else {
                main.append(makeLinksClickable(part, current));
            }
        }
        return GameComponentImpl.wrap(main);
    }

    /**
     * Processes a player's chat input, applying permission checks to any color / format codes
     * they have used, while preserving the channel's base formatting (color).
     *
     * @param channelBaseColor The base color prefix of the channel, e.g. "&7" for gray.
     * @param playerInput      The raw message text entered by the player.
     * @param player           The player whose permissions should be checked.
     * @return Component with safe styles applied.
     */
    public static GameComponent parsePlayerInputWithPermissions(String channelBaseColor,
                                                            String playerInput,
                                                            ServerPlayer player) {
        if (playerInput == null || playerInput.isEmpty()) {
            return GameComponentImpl.empty();
        }

        // First apply the base channel color (always allowed)
        Component baseColorComponent = parseColors(channelBaseColor).toMinecraft();
        Style baseStyle = baseColorComponent.getStyle();
        MutableComponent result = Component.empty();

        boolean hasColorPerm  = Verbatim.permissionService.hasPermission(player, PERM_CHAT_COLOR, 2);
        boolean hasFormatPerm = Verbatim.permissionService.hasPermission(player, PERM_CHAT_FORMAT, 2);

        String[] parts = playerInput.split("(?i)(?=&[0-9a-fk-or])");
        Style current = baseStyle;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&") && part.length() >= 2) {
                char code = part.charAt(1);
                ChatFormatting fmt = ChatFormatting.getByCode(code);
                String content = part.substring(2);

                if (fmt != null) {
                    boolean allow = false;
                    if (fmt.isColor()) {
                        allow = hasColorPerm;
                    } else if (fmt == ChatFormatting.RESET) {
                        allow = hasColorPerm || hasFormatPerm;
                    } else {
                        allow = hasFormatPerm;
                    }

                    if (allow) {
                        if (fmt.isColor()) {
                            current = Style.EMPTY.withColor(fmt);
                        } else if (fmt == ChatFormatting.RESET) {
                            current = baseStyle; // reset to base channel style
                        } else {
                            current = applyStyle(current, fmt);
                        }
                    }
                }

                if (!content.isEmpty()) {
                    result.append(makeLinksClickable(content, current));
                }
            } else {
                result.append(makeLinksClickable(part, current));
            }
        }

        return GameComponentImpl.wrap(result);
    }
} 
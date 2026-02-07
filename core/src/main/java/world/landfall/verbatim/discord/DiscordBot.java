package world.landfall.verbatim.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.NameStyle;
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.util.FormattingCodeUtils;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DiscordBot {

    private static final Map<UUID, Color> SPECIAL_UUID_COLORS = new HashMap<>();

    static {
        // confect1on's UUIDs (Minecraft and Hytale) - purple
        SPECIAL_UUID_COLORS.put(UUID.fromString("7755ac32-2fba-4ef6-a85b-93c354267a91"), new Color(155, 89, 182));
        SPECIAL_UUID_COLORS.put(UUID.fromString("e1b47ab1-35fa-4406-81f2-fa25db3367e2"), new Color(155, 89, 182));
        SPECIAL_UUID_COLORS.put(UUID.fromString("886f738d-8d9a-4ba9-9148-af80e82dd744"), new Color(130, 35, 109));
    }

    private static JDA jdaInstance;
    private static String discordChannelId;
    private static boolean enabled;
    private static boolean useEmbedMode;
    private static NameStyle discordNameStyle;
    private static ScheduledExecutorService presenceScheduler;
    private static final Set<UUID> recentDisconnects = ConcurrentHashMap.newKeySet();

    public static void init() {
        enabled = Verbatim.gameConfig.isDiscordEnabled();
        useEmbedMode = Verbatim.gameConfig.isDiscordUseEmbedMode();
        String nameStyleConfig = Verbatim.gameConfig.getDiscordNameStyle();
        discordNameStyle = NameStyle.fromConfigValue(nameStyleConfig);
        if (!enabled) {
            Verbatim.LOGGER.info("[Verbatim Discord] Bot is disabled in config.");
            return;
        }

        String botToken = Verbatim.gameConfig.getDiscordBotToken();
        discordChannelId = Verbatim.gameConfig.getDiscordChannelId();

        if (botToken == null || botToken.isEmpty() || botToken.equals("YOUR_DISCORD_BOT_TOKEN_HERE")) {
            Verbatim.LOGGER.error("[Verbatim Discord] Bot token is not configured. Discord bot will not start.");
            return;
        }
        if (discordChannelId == null || discordChannelId.isEmpty() || discordChannelId.equals("YOUR_DISCORD_CHANNEL_ID_HERE")) {
            Verbatim.LOGGER.error("[Verbatim Discord] Discord channel ID is not configured. Discord bot will not start.");
            return;
        }

        try {
            jdaInstance = JDABuilder.createDefault(botToken)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .setMemberCachePolicy(MemberCachePolicy.NONE)
                    .addEventListeners(new DiscordListener())
                    .build();
            jdaInstance.awaitReady();
            Verbatim.LOGGER.info("[Verbatim Discord] Bot connected and ready!");

            jdaInstance.upsertCommand("list", "Lists online players on the Minecraft server.").queue();
            Verbatim.LOGGER.info("[Verbatim Discord] /list slash command registered/updated.");

            updatePlayerCountStatus();
            presenceScheduler = Executors.newSingleThreadScheduledExecutor();
            presenceScheduler.scheduleAtFixedRate(DiscordBot::updatePlayerCountStatus, 1, 1, TimeUnit.MINUTES);
            Verbatim.LOGGER.info("[Verbatim Discord] Presence update scheduler started.");

        } catch (InterruptedException e) {
            Verbatim.LOGGER.error("[Verbatim Discord] JDA initialization was interrupted.", e);
            Thread.currentThread().interrupt();
            jdaInstance = null;
        } catch (Exception e) {
            Verbatim.LOGGER.error("[Verbatim Discord] Failed to initialize JDA or log in.", e);
            jdaInstance = null;
        }
    }

    public static void shutdown() {
        recentDisconnects.clear();
        if (presenceScheduler != null && !presenceScheduler.isShutdown()) {
            presenceScheduler.shutdown();
            try {
                if (!presenceScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    presenceScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                presenceScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            Verbatim.LOGGER.info("[Verbatim Discord] Presence update scheduler shut down.");
        }
        if (jdaInstance != null) {
            Verbatim.LOGGER.info("[Verbatim Discord] Shutting down Discord bot...");
            jdaInstance.shutdown();
            try {
                if (!jdaInstance.awaitShutdown(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    Verbatim.LOGGER.warn("[Verbatim Discord] Bot did not shut down in 10 seconds, forcing shutdown.");
                    jdaInstance.shutdownNow();
                }
            } catch (InterruptedException e) {
                Verbatim.LOGGER.error("[Verbatim Discord] Interrupted while awaiting bot shutdown.", e);
                jdaInstance.shutdownNow();
                Thread.currentThread().interrupt();
            }
            Verbatim.LOGGER.info("[Verbatim Discord] Bot has been shut down.");
            jdaInstance = null;
        }
    }

    public static void sendToDiscord(String message) {
        if (jdaInstance == null || discordChannelId == null || !enabled) {
            return;
        }
        try {
            TextChannel channel = jdaInstance.getTextChannelById(discordChannelId);
            if (channel != null) {
                channel.sendMessage(message).queue();
                Verbatim.LOGGER.debug("[Verbatim Discord Generic] Relayed: {}", message);
            } else {
                Verbatim.LOGGER.warn("[Verbatim Discord] Configured Discord channel ID '{}' not found for generic message.", discordChannelId);
            }
        } catch (Exception e) {
            Verbatim.LOGGER.error("[Verbatim Discord] Could not send generic message to Discord.", e);
        }
    }

    private static Color generateColorFromUUID(UUID uuid) {
        if (SPECIAL_UUID_COLORS.containsKey(uuid)) {
            return SPECIAL_UUID_COLORS.get(uuid);
        }

        int hash = uuid.hashCode();
        float hue = (Math.abs(hash) % 360) / 360.0f;
        return Color.getHSBColor(hue, 0.7f, 0.85f);
    }

    private static String getPlayerAvatarUrl(GamePlayer player) {
        return Verbatim.gameContext.getPlayerAvatarUrl(player);
    }

    public static void sendPlayerChatMessageToDiscord(GamePlayer player, String messageContent) {
        if (!isEnabled()) {
            return;
        }

        try {
            TextChannel channel = jdaInstance.getTextChannelById(discordChannelId);
            if (channel == null) {
                Verbatim.LOGGER.warn("[Verbatim Discord] Configured Discord channel ID '{}' not found for player message.", discordChannelId);
                return;
            }

            String cleanMessageContent = FormattingCodeUtils.stripFormattingCodes(messageContent);
            String authorName = Verbatim.chatFormatter.createDiscordPlayerName(player, discordNameStyle);

            if (useEmbedMode) {
                String avatarUrl = getPlayerAvatarUrl(player);

                EmbedBuilder embed = new EmbedBuilder();
                embed.setAuthor(authorName, null, avatarUrl);
                embed.setDescription(cleanMessageContent);
                embed.setColor(generateColorFromUUID(player.getUUID()));

                channel.sendMessageEmbeds(embed.build()).queue();
                Verbatim.LOGGER.debug("[Game -> Discord Embed] Relayed for {}: {}", authorName, cleanMessageContent);
            } else {
                String plainTextMessage = authorName + ": " + cleanMessageContent;
                channel.sendMessage(plainTextMessage).queue();
                Verbatim.LOGGER.debug("[Game -> Discord Plain] Relayed for {}: {}", authorName, cleanMessageContent);
            }
        } catch (Exception e) {
            Verbatim.LOGGER.error("[Verbatim Discord] Could not send player chat message to Discord for {}.", player.getUsername(), e);
        }
    }

    public static void sendPlayerConnectionStatusToDiscord(GamePlayer player, boolean joined) {
        if (!isEnabled()) {
            return;
        }

        UUID playerUuid = player.getUUID();
        if (joined) {
            recentDisconnects.remove(playerUuid);
        } else {
            // Deduplicate disconnect messages (can fire multiple times during shutdown)
            if (!recentDisconnects.add(playerUuid)) {
                return;
            }
        }

        try {
            TextChannel channel = jdaInstance.getTextChannelById(discordChannelId);
            if (channel == null) {
                Verbatim.LOGGER.warn("[Verbatim Discord] Configured Discord channel ID '{}' not found for connection status.", discordChannelId);
                return;
            }

            String effectiveName = Verbatim.chatFormatter.createDiscordPlayerName(player, discordNameStyle);

            if (useEmbedMode) {
                String avatarUrl = getPlayerAvatarUrl(player);

                EmbedBuilder embed = new EmbedBuilder();
                embed.setColor(joined ? new Color(0x4CAF50) : new Color(0xF44336));
                embed.setAuthor(effectiveName + (joined ? " has joined the server." : " has left the server."), null, avatarUrl);

                channel.sendMessageEmbeds(embed.build()).queue();
                Verbatim.LOGGER.debug("[Game -> Discord Embed] Connection Status: {} {}", effectiveName, (joined ? "joined" : "left"));
            } else {
                String statusEmoji = joined ? "+" : "-";
                String plainTextMessage = statusEmoji + " " + effectiveName + " has " + (joined ? "joined" : "left") + " the server.";
                channel.sendMessage(plainTextMessage).queue();
                Verbatim.LOGGER.debug("[Game -> Discord Plain] Connection Status: {} {}", effectiveName, (joined ? "joined" : "left"));
            }
            updatePlayerCountStatus();

        } catch (Exception e) {
            Verbatim.LOGGER.error("[Verbatim Discord] Could not send player connection status to Discord for {}.", player.getUsername(), e);
        }
    }

    public static String getDiscordMessagePrefix() {
        return Verbatim.gameConfig.getDiscordMessagePrefix();
    }

    public static String getDiscordMessageSeparator() {
        return Verbatim.gameConfig.getDiscordMessageSeparator();
    }

    public static boolean isEnabled() {
        return enabled && jdaInstance != null;
    }

    public static void updatePlayerCountStatus() {
        if (jdaInstance == null || !jdaInstance.getStatus().isInit()) {
            return;
        }

        String statusMessage;
        if (Verbatim.gameContext.isServerAvailable()) {
            int playerCount = Verbatim.gameContext.getOnlinePlayerCount();
            if (playerCount == 1) {
                statusMessage = "1 player online";
            } else {
                statusMessage = playerCount + " players online";
            }
        } else {
            statusMessage = "Server Offline";
        }

        try {
            jdaInstance.getPresence().setActivity(Activity.watching(statusMessage));
        } catch (Exception e) {
            Verbatim.LOGGER.warn("[Verbatim Discord] Could not update bot presence: {}", e.getMessage());
        }
    }
}

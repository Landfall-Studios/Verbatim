package world.landfall.verbatim.specialchannels;

import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GamePlayer;

import java.util.Optional;

/**
 * Platform-independent interface for special channel formatting operations.
 */
public interface ChannelFormatter {

    /**
     * Attempts to format a message for a local special channel.
     *
     * @param sender The player sending the message
     * @param channelConfig The channel configuration
     * @param originalMessageContent The original message content
     * @return Optional containing formatted message details if this is a local channel, empty otherwise
     */
    Optional<FormattedMessageDetails> formatLocalMessage(
            GamePlayer sender,
            ChatChannelManager.ChannelConfig channelConfig,
            String originalMessageContent);

    /**
     * Creates a distance-obscured version of a message.
     *
     * @param originalMessage The fully formatted original message
     * @param distanceSquared The squared distance between sender and recipient
     * @param effectiveRange The effective range of the channel
     * @param isRoleplayMessage Whether this is a roleplay message
     * @param channelMessageColorString The channel's message color string (e.g., "&7")
     * @return The obscured message component
     */
    GameComponent createDistanceObscuredMessage(
            GameComponent originalMessage,
            double distanceSquared,
            int effectiveRange,
            boolean isRoleplayMessage,
            String channelMessageColorString);
}

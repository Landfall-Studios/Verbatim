package world.landfall.verbatim.test;

import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.specialchannels.ChannelFormatter;
import world.landfall.verbatim.specialchannels.FormattedMessageDetails;

import java.util.Optional;

/**
 * Mock ChannelFormatter for unit testing.
 * Returns simple passthrough formatting.
 */
public class MockChannelFormatter implements ChannelFormatter {

    @Override
    public Optional<FormattedMessageDetails> formatLocalMessage(
            GamePlayer sender,
            ChatChannelManager.ChannelConfig channelConfig,
            String originalMessageContent) {
        // Check if this is a local channel
        if (channelConfig.specialChannelType.map("local"::equals).orElse(false)) {
            GameComponent message = new MockGameComponent(
                sender.getDisplayName() + ": " + originalMessageContent
            );
            return Optional.of(new FormattedMessageDetails(message, channelConfig.range, false, "&f"));
        }
        return Optional.empty();
    }

    @Override
    public GameComponent createDistanceObscuredMessage(
            GameComponent originalMessage,
            double distanceSquared,
            int effectiveRange,
            boolean isRoleplayMessage,
            String channelMessageColorString) {
        // For testing, just return the original
        return originalMessage.copy();
    }
}

package world.landfall.verbatim.specialchannels;

import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GameComponent;

/**
 * Data class to hold the results of special channel message formatting.
 * Platform-independent - uses GameComponent instead of Minecraft MutableComponent.
 */
public class FormattedMessageDetails {
    private final GameComponent formattedMessage;
    public final int effectiveRange;
    private final boolean isRoleplayMessage;
    private final String channelMessageColorForObscuring; // e.g., "&7", used if !isRoleplayMessage

    public FormattedMessageDetails(GameComponent formattedMessage, int effectiveRange, boolean isRoleplayMessage, String channelMessageColorForObscuring) {
        this.formattedMessage = formattedMessage;
        this.effectiveRange = effectiveRange;
        this.isRoleplayMessage = isRoleplayMessage;
        this.channelMessageColorForObscuring = channelMessageColorForObscuring;
    }

    /**
     * Gets the formatted message.
     */
    public GameComponent getFormattedMessage() {
        return formattedMessage;
    }

    /**
     * Gets the appropriate message component for a recipient at the given distance.
     * For special local channels (non-roleplay), this may return an obscured version based on distance.
     */
    public GameComponent getMessageForDistance(double distanceSquared) {
        if (effectiveRange < 0) return formattedMessage.copy(); // Global messages, no obscuring

        double distance = Math.sqrt(distanceSquared);
        if (distance <= effectiveRange) return formattedMessage.copy(); // Within clear range

        // Calculate fade distance using the same logic as LocalChannelFormatter
        double fadeDistance;
        if (effectiveRange <= 15) {
            fadeDistance = effectiveRange * 2.0;
        } else {
            fadeDistance = Math.min(30, effectiveRange * 0.6);
        }

        if (distance <= effectiveRange + fadeDistance) {
            return Verbatim.channelFormatter.createDistanceObscuredMessage(
                formattedMessage,
                distanceSquared,
                effectiveRange,
                isRoleplayMessage,
                channelMessageColorForObscuring
            );
        }

        return null; // Too far to receive message
    }
}

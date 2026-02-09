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

    /**
     * Calculates the obscure percentage for distance-based message fading.
     * Returns a value between 0.0 (fully clear) and 1.0 (fully obscured).
     *
     * @param distance           actual distance between sender and recipient
     * @param effectiveRange     the clear-hearing range of the channel
     * @param maxFadeDistance    cap on fade distance (e.g., 15 for Minecraft, 30 for Paper/Hytale)
     * @param smallRangeMultiplier multiplier when effectiveRange &lt;= 15 (e.g., 1.5 or 2.0)
     * @param largeRangeFactor   multiplier when effectiveRange &gt; 15 (e.g., 0.3 or 0.6)
     * @return clamped obscure percentage [0.0, 1.0]
     */
    public static double obscurePercentage(double distance, int effectiveRange,
                                           int maxFadeDistance, double smallRangeMultiplier,
                                           double largeRangeFactor) {
        double fadeDistance;
        if (effectiveRange <= 15) {
            fadeDistance = effectiveRange * smallRangeMultiplier;
        } else {
            fadeDistance = Math.min(maxFadeDistance, effectiveRange * largeRangeFactor);
        }
        double percentage = (distance - effectiveRange) / fadeDistance;
        return Math.min(1.0, Math.max(0.0, percentage));
    }
}

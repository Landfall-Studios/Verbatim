package world.landfall.verbatim.specialchannels;

/**
 * Parsed result of a local channel message suffix.
 * Extracts the suffix-based behavior (shouts, whispers, OOC, roleplay, etc.)
 * from a raw message string. This is pure string logic with no platform dependencies.
 */
public final class LocalMessageSuffix {
    private final int effectiveRange;
    private final String actionText;
    private final String trimmedMessage;
    private final boolean isRoleplay;
    private final boolean isOOC;

    private LocalMessageSuffix(int effectiveRange, String actionText, String trimmedMessage, boolean isRoleplay, boolean isOOC) {
        this.effectiveRange = effectiveRange;
        this.actionText = actionText;
        this.trimmedMessage = trimmedMessage;
        this.isRoleplay = isRoleplay;
        this.isOOC = isOOC;
    }

    public int effectiveRange() { return effectiveRange; }
    public String actionText() { return actionText; }
    public String trimmedMessage() { return trimmedMessage; }
    public boolean isRoleplay() { return isRoleplay; }
    public boolean isOOC() { return isOOC; }

    /**
     * Parses a message's trailing suffix to determine local channel behavior.
     *
     * <ul>
     *   <li>{@code !!} — shouts (range 100), suffix kept</li>
     *   <li>{@code !}  — exclaims (range 75), suffix kept</li>
     *   <li>{@code *}  — whispers (range 10), suffix stripped</li>
     *   <li>{@code ?}  — asks (range 50), suffix kept</li>
     *   <li>{@code $}  — mutters (range 3), suffix stripped</li>
     *   <li>{@code +}  — roleplay (range 50), suffix stripped</li>
     *   <li>{@code ))} — OOC (range 50), suffix stripped</li>
     *   <li>(default)  — says (range 50)</li>
     * </ul>
     */
    public static LocalMessageSuffix parse(String originalMessage) {
        if (originalMessage.endsWith("!!")) {
            return new LocalMessageSuffix(100, "shouts:", originalMessage, false, false);
        } else if (originalMessage.endsWith("!")) {
            return new LocalMessageSuffix(75, "exclaims:", originalMessage, false, false);
        } else if (originalMessage.endsWith("*")) {
            return new LocalMessageSuffix(10, "whispers:",
                    originalMessage.substring(0, originalMessage.length() - 1), false, false);
        } else if (originalMessage.endsWith("?")) {
            return new LocalMessageSuffix(50, "asks:", originalMessage, false, false);
        } else if (originalMessage.endsWith("$")) {
            return new LocalMessageSuffix(3, "mutters:",
                    originalMessage.substring(0, originalMessage.length() - 1), false, false);
        } else if (originalMessage.endsWith("+")) {
            return new LocalMessageSuffix(50, "",
                    originalMessage.substring(0, originalMessage.length() - 1), true, false);
        } else if (originalMessage.endsWith("))")) {
            return new LocalMessageSuffix(50, "",
                    originalMessage.substring(0, originalMessage.length() - 2), false, true);
        }
        return new LocalMessageSuffix(50, "says:", originalMessage, false, false);
    }
}

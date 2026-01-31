package world.landfall.verbatim.util;

/**
 * Pure string utility for formatting code operations.
 * No platform dependencies.
 */
public final class FormattingCodeUtils {

    private FormattingCodeUtils() {}

    /**
     * Strips Minecraft formatting codes (both & and section) from a string.
     */
    public static String stripFormattingCodes(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("(?i)[&\u00a7][0-9A-FK-OR]", "");
    }
}

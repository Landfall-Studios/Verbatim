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
        return text.replaceAll("(?i)[&§]#[0-9a-f]{6}|[&§][0-9A-FK-OR]", "");
    }
}

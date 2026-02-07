package world.landfall.verbatim.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FormattingCodeUtils.
 */
class FormattingCodeUtilsTest {

    @Test
    void stripFormattingCodesRemovesAmpersandCodes() {
        String input = "&aGreen &bAqua &cRed";
        String result = FormattingCodeUtils.stripFormattingCodes(input);
        assertEquals("Green Aqua Red", result);
    }

    @Test
    void stripFormattingCodesRemovesSectionCodes() {
        String input = "\u00a7aGreen \u00a7bAqua \u00a7cRed";
        String result = FormattingCodeUtils.stripFormattingCodes(input);
        assertEquals("Green Aqua Red", result);
    }

    @Test
    void stripFormattingCodesRemovesMixedCodes() {
        String input = "&aGreen \u00a7bAqua &cRed";
        String result = FormattingCodeUtils.stripFormattingCodes(input);
        assertEquals("Green Aqua Red", result);
    }

    @Test
    void stripFormattingCodesRemovesFormatCodes() {
        String input = "&lBold &oItalic &nUnderline &mStrike &kObfuscated &rReset";
        String result = FormattingCodeUtils.stripFormattingCodes(input);
        assertEquals("Bold Italic Underline Strike Obfuscated Reset", result);
    }

    @Test
    void stripFormattingCodesPreservesNormalText() {
        String input = "Hello World!";
        String result = FormattingCodeUtils.stripFormattingCodes(input);
        assertEquals("Hello World!", result);
    }

    @Test
    void stripFormattingCodesHandlesNull() {
        assertNull(FormattingCodeUtils.stripFormattingCodes(null));
    }

    @Test
    void stripFormattingCodesHandlesEmpty() {
        assertEquals("", FormattingCodeUtils.stripFormattingCodes(""));
    }

    @Test
    void stripFormattingCodesIsCaseInsensitive() {
        String input = "&AGreen &BBlue";
        String result = FormattingCodeUtils.stripFormattingCodes(input);
        assertEquals("Green Blue", result);
    }

    @Test
    void stripFormattingCodesRemovesHexColorCodes() {
        // Note: This only strips the basic &X codes, not full hex
        String input = "&0Black &1Blue &2Green &3Cyan &4Red &5Purple &6Gold &7Gray";
        String result = FormattingCodeUtils.stripFormattingCodes(input);
        assertEquals("Black Blue Green Cyan Red Purple Gold Gray", result);
    }

    @Test
    void stripFormattingCodesPreservesOtherAmpersands() {
        String input = "Tom & Jerry";
        String result = FormattingCodeUtils.stripFormattingCodes(input);
        assertEquals("Tom & Jerry", result);
    }
}

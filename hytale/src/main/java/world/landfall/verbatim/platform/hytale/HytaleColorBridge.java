package world.landfall.verbatim.platform.hytale;

import world.landfall.verbatim.context.GameColor;

import java.awt.Color;

/**
 * Conversion utility between GameColor and java.awt.Color for use with Hytale's Message API.
 *
 * Hytale's Message.color() accepts java.awt.Color, so we convert GameColor RGB values
 * to java.awt.Color instances.
 */
public final class HytaleColorBridge {

    private HytaleColorBridge() {}

    /**
     * Converts a GameColor to a java.awt.Color for use with Message.color().
     * Uses the RGB value stored in GameColor.
     */
    public static Color toHytaleColor(GameColor color) {
        return new Color(color.getRgb());
    }
}

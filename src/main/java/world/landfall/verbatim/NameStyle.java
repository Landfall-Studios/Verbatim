package world.landfall.verbatim;

public enum NameStyle {
    DISPLAY_NAME("displayName"),
    USERNAME("username"),
    NICKNAME("nickname");

    private final String configValue;

    NameStyle(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigValue() {
        return configValue;
    }

    public static NameStyle fromConfigValue(String value) {
        if (value == null || value.isEmpty()) {
            return DISPLAY_NAME; // Default
        }

        for (NameStyle style : values()) {
            if (style.configValue.equalsIgnoreCase(value)) {
                return style;
            }
        }

        return DISPLAY_NAME; // Fallback to default
    }
}
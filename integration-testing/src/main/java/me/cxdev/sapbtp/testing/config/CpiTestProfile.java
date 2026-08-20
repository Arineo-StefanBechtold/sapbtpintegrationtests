package me.cxdev.sapbtp.testing.config;

import java.util.Locale;

public enum CpiTestProfile {
    LOCAL,
    STAGING,
    PRODUCTION;

    public static CpiTestProfile from(String value) {
        if (value == null || value.isBlank()) {
            return LOCAL;
        }
        return CpiTestProfile.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public String fileSuffix() {
        return name().toLowerCase(Locale.ROOT);
    }
}

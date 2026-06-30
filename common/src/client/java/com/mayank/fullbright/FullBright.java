package com.mayank.fullbright;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullBright {
    public static final String MOD_ID = "fullbright";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean isEnabled() {
        return LightingManager.isActive();
    }

    public static void setEnabled(boolean value) {
        LightingManager.setActive(value);
        LightingManager.save();
    }

    public static void toggle() {
        LightingManager.toggleActive();
        LightingManager.save();
    }
}
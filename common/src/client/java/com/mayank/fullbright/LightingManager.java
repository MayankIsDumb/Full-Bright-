package com.mayank.fullbright;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.stream.JsonReader;

import net.minecraft.client.Minecraft;

public class LightingManager {
    private static final String FILENAME = "config/fullbright.json";
    private static final String ENABLED = "enabled";
    private static final String RETAIN_ON_STATE = "retain_on_state";
    private static final String ON_STATE = "on_state";
    private static final String NO_FOG = "noFog";

    // Keybinds
    private static final String KEY_TOGGLE = "keyToggle";
    private static final String KEY_NOFOG = "keyNoFog";
    private static final String KEY_CONFIG = "keyConfig";
    private static final String BRIGHT_INTENSITY = "brightIntensity";

    private static boolean isActive = false;
    public static boolean rememberActive = false;
    public static boolean noFog = false;

    // Intensity values
    public static int brightIntensity = 15;  // 0-15 light level

    // Key codes (GLFW key constants)
    public static int keyToggle = 71;  // GLFW_KEY_G
    public static int keyNoFog = 72;   // GLFW_KEY_H
    public static int keyConfig = 74;  // GLFW_KEY_J

    // Key held state for toggle detection
    public static boolean toggleKeyHeld = false;
    public static boolean noFogKeyHeld = false;

    public static boolean isActive() {
        return isActive;
    }

    public static void setActive(boolean value) {
        isActive = value;
        applyGamma();
        if (Minecraft.getInstance().levelRenderer != null) {
            Minecraft.getInstance().levelRenderer.allChanged();
        }
    }

    public static void applyGamma() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        if (isActive) {
            // OverrideIllegalMixin bypasses clamping so we can set >1.0
            mc.options.gamma().set((double) brightIntensity);
        } else {
            mc.options.gamma().set(1.0);
        }
    }

    public static void toggleActive() {
        setActive(!isActive);
    }

    public static void toggleNoFog() {
        noFog = !noFog;
    }

    public static void save() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("\t\"").append(ENABLED).append("\": ").append(isActive).append(",\n");
        json.append("\t\"").append(NO_FOG).append("\": ").append(noFog).append(",\n");
        json.append("\t\"").append(RETAIN_ON_STATE).append("\": ").append(rememberActive).append(",\n");
        json.append("\t\"").append(ON_STATE).append("\": ").append(isActive).append(",\n");
        json.append("\t\"").append(BRIGHT_INTENSITY).append("\": ").append(brightIntensity).append(",\n");
        json.append("\t\"").append(KEY_TOGGLE).append("\": ").append(keyToggle).append(",\n");
        json.append("\t\"").append(KEY_NOFOG).append("\": ").append(keyNoFog).append(",\n");
        json.append("\t\"").append(KEY_CONFIG).append("\": ").append(keyConfig).append("\n");
        json.append("}");

        try (FileWriter writer = new FileWriter(FILENAME)) {
            writer.write(json.toString());
        } catch (IOException e) {
            FullBright.LOGGER.error("Failed to write config: {}", e.getMessage());
        }
    }

    public static void load() {
        File configFile = new File(FILENAME);
        if (!configFile.exists()) {
            FullBright.LOGGER.info("No config file found, creating default config");
            save();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonReader parser = new JsonReader(reader);
            boolean setActiveState = false;

            parser.beginObject();

            while (parser.hasNext()) {
                String name = parser.nextName();
                switch (name) {
                    case ENABLED:
                    case ON_STATE:
                        setActiveState = parser.nextBoolean();
                        break;
                    case NO_FOG:
                        noFog = parser.nextBoolean();
                        break;
                    case RETAIN_ON_STATE:
                        rememberActive = parser.nextBoolean();
                        break;
                    case BRIGHT_INTENSITY:
                        brightIntensity = parser.nextInt();
                        break;
                    case KEY_TOGGLE:
                        keyToggle = parser.nextInt();
                        break;
                    case KEY_NOFOG:
                        keyNoFog = parser.nextInt();
                        break;
                    case KEY_CONFIG:
                        keyConfig = parser.nextInt();
                        break;
                    default:
                        parser.skipValue();
                        break;
                }
            }

            parser.close();

            if (rememberActive) isActive = setActiveState;
        } catch (Exception e) {
            FullBright.LOGGER.warn("Error parsing config, regenerating: {}", e.getMessage());
            save();
        }
    }
}

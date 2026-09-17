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
    public static boolean rememberActive = true;
    public static boolean noFog = false;

    // Gamma the player had before the first toggle-on (vanilla default is
    // 0.5). Restored on toggle-off so OFF never leaves max brightness behind.
    private static Double previousGamma = null;

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
        refreshLevelRenderer();
    }

    /**
     * Version-tolerant chunk/level refresh.
     * 1.21.x / 26.1.x have LevelRenderer#allChanged().
     * 26.3 removed it — and NEITHER 26.3 fallback is safe:
     * - resetLevelRenderData() releases all buffers and nulls ViewArea
     *   without rebuilding it, so the next frame crashes with an NPE in
     *   LevelRenderer#repositionCamera.
     * - invalidateCompiledGeometry() rebuilds ViewArea but leaves the
     *   extractor's SectionUpdateTracker (which still believes every
     *   section is compiled) in place, so no section is ever re-queued
     *   and the world stays blank forever. Vanilla only calls it together
     *   with a fresh SectionUpdateTracker.
     * No refresh is needed on 26.3 anyway: gamma is consumed per-frame by
     * LightmapRenderStateExtractor and fog by FogRenderer#setupFog, so
     * both toggles apply instantly without touching chunk data.
     * Reflection keeps common code compiling against all versions.
     */
    public static void refreshLevelRenderer() {
        try {
            Object renderer = Minecraft.getInstance().levelRenderer;
            if (renderer == null) return;
            try {
                renderer.getClass().getMethod("allChanged").invoke(renderer);
            } catch (NoSuchMethodException ignored) {
                // 26.3+: nothing to do (see above).
            }
        } catch (Exception ignored) {
        }
    }

    public static void applyGamma() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        if (isActive) {
            // Remember the player's own brightness once, so toggle-off can
            // restore it exactly (vanilla default is 0.5, not 1.0).
            if (previousGamma == null) {
                previousGamma = (Double) mc.options.gamma().get();
            }
            // OverrideIllegalMixin bypasses clamping so we can set >1.0
            mc.options.gamma().set((double) brightIntensity);
        } else {
            Double restore = previousGamma != null ? previousGamma : 0.5;
            previousGamma = null;
            mc.options.gamma().set(restore);
        }
    }

    public static void toggleActive() {
        setActive(!isActive);
    }

    public static void toggleNoFog() {
        noFog = !noFog;
    }

    public static void save() {
        // Always persist enabled state — user expects fullbright to survive relaunch
        // and not turn off when any game setting changes.
        rememberActive = true;
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("\t\"").append(ENABLED).append("\": ").append(isActive).append(",\n");
        json.append("\t\"").append(NO_FOG).append("\": ").append(noFog).append(",\n");
        json.append("\t\"").append(RETAIN_ON_STATE).append("\": ").append(true).append(",\n");
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

            // Always restore — old configs with retain_on_state=false would otherwise lose state on relaunch.
            isActive = setActiveState;
            // Apply after load if options are already available; otherwise tick enforcer will do it.
            try { applyGamma(); } catch (Exception ignored) {}
        } catch (Exception e) {
            FullBright.LOGGER.warn("Error parsing config, regenerating: {}", e.getMessage());
            save();
        }
    }

    /** Called every tick — re-enforces gamma if something (options screen, vanilla load) overwrote it. */
    public static void enforceGammaIfNeeded() {
        if (!isActive) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options == null) return;
            Object cur = mc.options.gamma().get();
            double curGamma = cur instanceof Double d ? d : ((Number) cur).doubleValue();
            if (curGamma != (double) brightIntensity) {
                mc.options.gamma().set((double) brightIntensity);
            }
        } catch (Exception ignored) {}
    }
}

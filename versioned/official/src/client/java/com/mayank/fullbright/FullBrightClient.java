package com.mayank.fullbright;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class FullBrightClient implements ClientModInitializer {
    public static KeyMapping CONFIG_KEY;

    // Track last pressed state to detect key down events
    private static boolean lastTogglePressed = false;
    private static boolean lastNoFogPressed = false;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("fullbright", "keys"));

    @Override
    public void onInitializeClient() {
        // Initialize lighting config (loads key codes from config file)
        LightingManager.load();

        // Only register Config key - toggle/nofog handled manually via GLFW
        CONFIG_KEY = new KeyMapping(
            "key.fullbright.config",
            InputConstants.Type.KEYSYM,
            LightingManager.keyConfig,
            CATEGORY
        );
        KeyMappingHelper.registerKeyMapping(CONFIG_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Don't process keybinds when a screen is open (chat, sign, book, etc.)
            if (client.screen != null) {
                lastTogglePressed = false;
                lastNoFogPressed = false;
                return;
            }

            long window = client.getWindow().handle();

            // Use current key codes from LightingManager (updated when config changes)
            int currentToggleKey = LightingManager.keyToggle;
            int currentNoFogKey = LightingManager.keyNoFog;

            // Check toggle key - detect key down (not hold)
            boolean togglePressed = GLFW.glfwGetKey(window, currentToggleKey) == GLFW.GLFW_PRESS;
            if (togglePressed && !lastTogglePressed) {
                FullBright.toggle();
                if (client.player != null) {
                    client.player.sendOverlayMessage(
                        net.minecraft.network.chat.Component.literal(
                            "Fullbright: " + (LightingManager.isActive() ? "ON" : "OFF")
                        )
                    );
                }
            }
            lastTogglePressed = togglePressed;

            // Check no-fog key - detect key down (not hold)
            boolean noFogPressed = GLFW.glfwGetKey(window, currentNoFogKey) == GLFW.GLFW_PRESS;
            if (noFogPressed && !lastNoFogPressed) {
                LightingManager.toggleNoFog();
                if (client.player != null) {
                    client.player.sendOverlayMessage(
                        net.minecraft.network.chat.Component.literal(
                            "No Fog: " + (LightingManager.noFog ? "ON" : "OFF")
                        )
                    );
                }
            }
            lastNoFogPressed = noFogPressed;

            // Check config key (registered via KeyMapping for proper conflict detection)
            while (CONFIG_KEY.consumeClick()) {
                client.setScreen(new ConfigScreen(client.screen));
            }
        });
    }
}

package com.mayank.fullbright;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;

public class FullBrightClient implements ClientModInitializer {
    public static KeyMapping CONFIG_KEY;

    // Track last pressed state to detect key down events
    private static boolean lastTogglePressed = false;
    private static boolean lastNoFogPressed = false;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("fullbright", "keys"));

    // MC 26.3 removed GLFW (uses SDL now). Old GLFW defaults -> new SDL scancodes.
    // GLFW G=71 H=72 J=74 -> SDL G=10 H=11 J=13 (see InputConstants KEY_G/KEY_H/KEY_J).
    private static void migrateOldGlfwKeyCodes() {
        if (LightingManager.keyToggle == 71) LightingManager.keyToggle = InputConstants.KEY_G;
        if (LightingManager.keyNoFog == 72) LightingManager.keyNoFog = InputConstants.KEY_H;
        if (LightingManager.keyConfig == 74) LightingManager.keyConfig = InputConstants.KEY_J;
    }

    @Override
    public void onInitializeClient() {
        // Initialize lighting config (loads key codes from config file)
        LightingManager.load();
        migrateOldGlfwKeyCodes();
        // Ensure gamma is applied after load (fixes relaunch-off)
        try { LightingManager.applyGamma(); } catch (Exception ignored) {}

        // Only register Config key - toggle/nofog handled manually via InputConstants (SDL, no GLFW in 26.3)
        CONFIG_KEY = new KeyMapping(
            "key.fullbright.config",
            InputConstants.Type.KEYBOARD,
            LightingManager.keyConfig,
            CATEGORY
        );
        KeyMappingHelper.registerKeyMapping(CONFIG_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Re-apply gamma every tick if active — prevents vanilla Options screen / resource reload from resetting it.
            LightingManager.enforceGammaIfNeeded();

            // Don't process keybinds when a screen is open or chat is focused (typing H/J/G must not toggle)
            boolean hasScreen = false;
            try { hasScreen = client.gui != null && client.gui.screen() != null; } catch (Exception ignored) {}
            boolean chatFocused = false;
            try { chatFocused = client.gui != null && client.gui.hud != null && client.gui.hud.getChat() != null && client.gui.hud.getChat().isChatFocused(); } catch (Exception ignored) {}
            if (hasScreen || chatFocused) {
                lastTogglePressed = false;
                lastNoFogPressed = false;
                return;
            }

            // Use current key codes from LightingManager (updated when config changes)
            int currentToggleKey = LightingManager.keyToggle;
            int currentNoFogKey = LightingManager.keyNoFog;

            // Check toggle key - detect key down (not hold). InputConstants.isKeyDown uses SDL in 26.3.
            boolean togglePressed = InputConstants.isKeyDown(currentToggleKey);
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
            boolean noFogPressed = InputConstants.isKeyDown(currentNoFogKey);
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
                client.setScreenAndShow(new ConfigScreen(client.gui.screen()));
            }
        });
    }
}

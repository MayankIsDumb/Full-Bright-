package com.mayank.fullbright;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private Button fullBrightButton;
    private Button noFogButton;
    private Button toggleKeyButton;
    private Button noFogKeyButton;

    private boolean bindingToggle = false;
    private boolean bindingNoFog = false;
    private int bindCooldown = 0;

    // Brightness slider positions
    private int sliderStartX, sliderEndX, sliderY;
    private boolean draggingSlider = false;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Afkz studios Fullbright"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4;

        sliderStartX = centerX - 100;
        sliderEndX = centerX + 100;
        sliderY = startY + 95;

        this.addRenderableWidget(Button.builder(
            Component.literal("Afkz studios Fullbright"),
            btn -> { }
        ).pos(centerX - 80, startY).size(160, 20).build());

        fullBrightButton = this.addRenderableWidget(Button.builder(
            Component.literal("Fullbright: " + (LightingManager.isActive() ? "ON" : "OFF")),
            btn -> {
                LightingManager.toggleActive();
                fullBrightButton.setMessage(Component.literal("Fullbright: " + (LightingManager.isActive() ? "ON" : "OFF")));
                if (LightingManager.isActive() && this.minecraft.levelRenderer != null) {
                    this.minecraft.levelRenderer.allChanged();
                }
            }
        ).pos(centerX - 80, startY + 25).size(160, 20).build());

        noFogButton = this.addRenderableWidget(Button.builder(
            Component.literal("No Fog: " + (LightingManager.noFog ? "ON" : "OFF")),
            btn -> {
                LightingManager.toggleNoFog();
                noFogButton.setMessage(Component.literal("No Fog: " + (LightingManager.noFog ? "ON" : "OFF")));
            }
        ).pos(centerX - 80, startY + 50).size(160, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Brightness: " + LightingManager.brightIntensity),
            btn -> { }
        ).pos(centerX - 80, startY + 75).size(160, 20).build());

        toggleKeyButton = this.addRenderableWidget(Button.builder(
            Component.literal("Toggle Key: " + GLFW.glfwGetKeyName(LightingManager.keyToggle, 0)),
            btn -> {
                bindingToggle = true;
                bindingNoFog = false;
                toggleKeyButton.setMessage(Component.literal("Press a key..."));
            }
        ).pos(centerX - 80, startY + 120).size(160, 20).build());

        noFogKeyButton = this.addRenderableWidget(Button.builder(
            Component.literal("No Fog Key: " + GLFW.glfwGetKeyName(LightingManager.keyNoFog, 0)),
            btn -> {
                bindingNoFog = true;
                bindingToggle = false;
                noFogKeyButton.setMessage(Component.literal("Press a key..."));
            }
        ).pos(centerX - 80, startY + 145).size(160, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xC0101010);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        guiGraphics.fill(sliderStartX, sliderY, sliderEndX, sliderY + 10, 0xFF404040);
        int filledWidth = (int) ((LightingManager.brightIntensity / 15.0) * (sliderEndX - sliderStartX));
        guiGraphics.fill(sliderStartX, sliderY, sliderStartX + filledWidth, sliderY + 10, 0xFF00AAFF);
        int handleX = sliderStartX + filledWidth - 2;
        guiGraphics.fill(handleX, sliderY - 2, handleX + 4, sliderY + 12, 0xFFFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (draggingSlider || (mouseX >= sliderStartX && mouseX <= sliderEndX && mouseY >= sliderY && mouseY <= sliderY + 10)) {
            draggingSlider = true;
            double ratio = (mouseX - sliderStartX) / (double) (sliderEndX - sliderStartX);
            LightingManager.brightIntensity = (int) Math.round(Math.max(0, Math.min(15, ratio * 15)));
            if (LightingManager.isActive()) LightingManager.applyGamma();
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingSlider = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= sliderStartX && mouseX <= sliderEndX && mouseY >= sliderY - 5 && mouseY <= sliderY + 15) {
            LightingManager.brightIntensity = Math.max(0, Math.min(15, LightingManager.brightIntensity + (int) Math.signum(verticalAmount)));
            if (LightingManager.isActive()) LightingManager.applyGamma();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (bindCooldown > 0) return true;

        if (bindingToggle) {
            LightingManager.keyToggle = event.key();
            bindingToggle = false;
            toggleKeyButton.setMessage(Component.literal("Toggle Key: " + GLFW.glfwGetKeyName(event.key(), 0)));
            return true;
        }

        if (bindingNoFog) {
            LightingManager.keyNoFog = event.key();
            bindingNoFog = false;
            noFogKeyButton.setMessage(Component.literal("No Fog Key: " + GLFW.glfwGetKeyName(event.key(), 0)));
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        if (bindCooldown > 0) bindCooldown--;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void removed() {
        LightingManager.save();
    }
}

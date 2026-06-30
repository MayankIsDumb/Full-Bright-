package com.mayank.fullbright.mixin;

import com.mayank.fullbright.LightingManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Inject(method = "setupFog", at = @At("RETURN"))
    private static void onSetupFog(
            Camera camera,
            FogRenderer.FogMode fogMode,
            float renderDistance,
            boolean isFoggy,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (!LightingManager.noFog) return;

        FogRenderer.setupNoFog();
    }
}

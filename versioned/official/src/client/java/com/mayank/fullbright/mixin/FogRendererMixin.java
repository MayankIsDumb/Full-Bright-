package com.mayank.fullbright.mixin;

import com.mayank.fullbright.LightingManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Inject(
        method = "setupFog",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onSetupFog(
            Camera camera,
            int renderDistanceInChunks,
            DeltaTracker deltaTracker,
            float darkenWorldAmount,
            ClientLevel level,
            CallbackInfoReturnable<FogData> cir
    ) {
        if (!LightingManager.noFog) return;

        FogData fog = cir.getReturnValue();
        if (fog == null) return;

        fog.renderDistanceStart = Float.MAX_VALUE;
        fog.renderDistanceEnd = Float.MAX_VALUE;
        fog.environmentalStart = Float.MAX_VALUE;
        fog.environmentalEnd = Float.MAX_VALUE;

        cir.setReturnValue(fog);
    }
}

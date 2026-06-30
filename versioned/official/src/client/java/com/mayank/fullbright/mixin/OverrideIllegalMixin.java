package com.mayank.fullbright.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionInstance.class)
public class OverrideIllegalMixin<T> {
    @Shadow
    T value;

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private void overrideSet(T newValue, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.options != null) {
            OptionInstance<?> option = mc.options.gamma();

            if ((Object) this == option) {
                this.value = newValue;
                ci.cancel();
            }
        }
    }
}

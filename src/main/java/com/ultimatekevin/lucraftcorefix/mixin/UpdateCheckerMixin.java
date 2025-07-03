// src/main/java/com/ultimatekevin/lucraftcorefix/mixin/UpdateCheckerMixin.java
package com.ultimatekevin.lucraftcorefix.mixin;

import lucraft.mods.lucraftcore.util.updatechecker.UpdateChecker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UpdateChecker.class, remap = false)
public abstract class UpdateCheckerMixin {
    @Inject(method = "<init>", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        ci.cancel();
    }
}
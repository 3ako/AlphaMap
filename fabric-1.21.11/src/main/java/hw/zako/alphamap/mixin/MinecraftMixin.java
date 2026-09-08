package hw.zako.alphamap.mixin;

import hw.zako.alphamap.MapInput;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void alphamap$holdUse(CallbackInfo callback) {
        if (MapInput.cursorActive()) callback.cancel();
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void alphamap$holdAttack(boolean leftClick, CallbackInfo callback) {
        if (MapInput.cursorActive()) callback.cancel();
    }
}

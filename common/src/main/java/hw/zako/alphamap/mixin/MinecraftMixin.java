package hw.zako.alphamap.mixin;

import hw.zako.alphamap.AlphaMapClient;
import hw.zako.alphamap.MapInput;
import hw.zako.alphamap.MapSketch;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void alphamap$holdUse(CallbackInfo callback) {
        if (MapInput.cursorActive()) callback.cancel();
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void alphamap$holdSwing(CallbackInfoReturnable<Boolean> callback) {
        if (MapInput.cursorActive()) callback.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void alphamap$holdAttack(boolean leftClick, CallbackInfo callback) {
        if (MapInput.cursorActive()) callback.cancel();
    }

    @Inject(method = "pauseGame", at = @At("HEAD"), cancellable = true)
    private void alphamap$closeTheMap(boolean pauseOnly, CallbackInfo callback) {
        if (!AlphaMapClient.pinned()) return;

        MapSketch.clear();
        MapInput.reset((Minecraft) (Object) this);
        callback.cancel();
    }
}

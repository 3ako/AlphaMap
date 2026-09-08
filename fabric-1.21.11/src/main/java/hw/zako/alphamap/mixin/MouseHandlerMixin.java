package hw.zako.alphamap.mixin;

import hw.zako.alphamap.MapInput;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void alphamap$zoomTheMap(long window, double xOffset, double yOffset, CallbackInfo callback) {
        if (!MapInput.cursorActive()) return;

        MapInput.scroll(yOffset != 0 ? yOffset : xOffset);
        callback.cancel();
    }

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void alphamap$keepCursorFree(CallbackInfo callback) {
        if (MapInput.cursorActive()) callback.cancel();
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void alphamap$holdCamera(double partialTick, CallbackInfo callback) {
        if (MapInput.cursorActive()) callback.cancel();
    }
}

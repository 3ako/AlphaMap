package hw.zako.alphamap.mixin;

import hw.zako.alphamap.ChunkMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {

    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void alphamap$forgetColumn(BlockPos pos, BlockState oldState, BlockState newState, int flags,
                                       CallbackInfo callback) {
        ChunkMap.blockChanged((ClientLevel) (Object) this, pos);
    }
}

package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class BlockLook {

    private final Identifier WATER = Identifier.withDefaultNamespace("block/water_still");

    public @Nullable Identifier topSprite(BlockState state) {
        if (state.getFluidState().is(FluidTags.WATER)) return WATER;

        List<BlockStateModelPart> parts = new ArrayList<>();
        Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state)
                .collectParts(RandomSource.create(42L), parts);
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(Direction.UP)) return quad.materialInfo().sprite().contents().name();
            for (BakedQuad quad : part.getQuads(null)) {
                if (quad.direction() == Direction.UP) return quad.materialInfo().sprite().contents().name();
            }
        }
        return null;
    }

    public int tint(BlockState state, ClientLevel level, BlockPos pos) {
        BlockState tinted = state.getFluidState().is(FluidTags.WATER) ? Blocks.WATER.defaultBlockState() : state;
        BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(tinted, 0);
        return source == null ? -1 : source.colorInWorld(tinted, level, pos);
    }
}

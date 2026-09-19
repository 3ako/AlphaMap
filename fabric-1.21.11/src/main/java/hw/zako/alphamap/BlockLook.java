package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class BlockLook {

    private final Identifier WATER = Identifier.withDefaultNamespace("block/water_still");

    public @Nullable Identifier topSprite(BlockState state) {
        if (state.getFluidState().is(FluidTags.WATER)) return WATER;

        var model = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(state);
        for (BlockModelPart part : model.collectParts(RandomSource.create(42L))) {
            for (BakedQuad quad : part.getQuads(Direction.UP)) return quad.sprite().contents().name();
            for (BakedQuad quad : part.getQuads(null)) {
                if (quad.direction() == Direction.UP) return quad.sprite().contents().name();
            }
        }
        return null;
    }

    public int tint(BlockState state, ClientLevel level, BlockPos pos) {
        BlockState tinted = state.getFluidState().is(FluidTags.WATER) ? Blocks.WATER.defaultBlockState() : state;
        return Minecraft.getInstance().getBlockColors().getColor(tinted, level, pos, 0);
    }
}

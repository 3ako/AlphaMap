package hw.zako.alphamap;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class MobListScreen extends MobListScreenBase {

    public MobListScreen(@Nullable Screen parent) {
        super(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        paint(new Canvas(graphics));
    }
}

package hw.zako.alphamap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class MinimapMoveScreen extends MinimapMoveScreenBase {

    public MinimapMoveScreen(@Nullable Screen parent) {
        super(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        paint(new Canvas(graphics), delta);
    }
}

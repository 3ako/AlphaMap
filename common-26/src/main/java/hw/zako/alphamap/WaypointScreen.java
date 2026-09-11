package hw.zako.alphamap;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.Nullable;

public final class WaypointScreen extends WaypointScreenBase {

    private WaypointScreen(int index) {
        super(index);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        paint(new Canvas(graphics));
    }

    public static @Nullable WaypointScreen of(int index) {
        return exists(index) ? new WaypointScreen(index) : null;
    }
}

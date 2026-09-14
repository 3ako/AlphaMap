package hw.zako.alphamap;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class WaypointScreen extends WaypointScreenBase {

    private WaypointScreen(int index, @Nullable Screen parent) {
        super(index, parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        paint(new Canvas(graphics));
    }

    public static @Nullable WaypointScreen of(int index) {
        return of(index, null);
    }

    public static @Nullable WaypointScreen of(int index, @Nullable Screen parent) {
        return exists(index) ? new WaypointScreen(index, parent) : null;
    }
}

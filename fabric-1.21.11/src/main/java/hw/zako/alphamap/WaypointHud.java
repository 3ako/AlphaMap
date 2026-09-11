package hw.zako.alphamap;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class WaypointHud implements HudElement {

    WaypointOverlay overlay;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        overlay.draw(new Canvas(graphics));
    }
}

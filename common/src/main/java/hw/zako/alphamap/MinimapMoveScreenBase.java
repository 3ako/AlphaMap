package hw.zako.alphamap;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public abstract class MinimapMoveScreenBase extends Screen {

    private static final int WIDTH = 200;
    private static final int HEIGHT = 20;

    private final @Nullable Screen parent;

    private boolean dragging;
    private double grabX;
    private double grabY;

    protected MinimapMoveScreenBase(@Nullable Screen parent) {
        super(Component.translatable("alphamap.minimap.move.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds((width - WIDTH) / 2, height - HEIGHT - 12, WIDTH, HEIGHT)
                .build());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;

        MinimapOverlay minimap = AlphaMapClient.minimap();
        int size = MapSettings.get().minimapSize();
        int left = minimap.left(width);
        int top = minimap.top(height);
        if (event.x() < left || event.x() > left + size
                || event.y() < top || event.y() > top + size) return false;

        dragging = true;
        grabX = event.x() - left;
        grabY = event.y() - top;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!dragging) return super.mouseDragged(event, dragX, dragY);

        int size = MapSettings.get().minimapSize();
        MapSettings.get().minimapPosition(
                share(event.x() - grabX, width - size),
                share(event.y() - grabY, height - size));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!dragging) return super.mouseReleased(event);

        dragging = false;
        MapSettings.get().clampAndSave();
        return true;
    }

    protected void paint(Canvas canvas, float partialTick) {
        AlphaMapClient.minimap().preview(canvas, partialTick);
        canvas.centered(font, title, width / 2, 16, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        MapSettings.get().clampAndSave();
        Vanilla.setScreen(minecraft, parent);
    }

    private static double share(double position, int room) {
        return room <= 0 ? 0.0 : position / room;
    }
}

package hw.zako.alphamap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class WaypointScreen extends Screen {

    private static final int WIDTH = 220;
    private static final int HEIGHT = 20;
    private static final int SWATCH = 22;

    private final int index;

    private EditBox name;
    private int colour;

    public WaypointScreen(int index) {
        super(Component.translatable("alphamap.waypoint.title"));
        this.index = index;
        this.colour = Waypoints.all().get(index).colour();
    }

    @Override
    protected void init() {
        Waypoint waypoint = Waypoints.all().get(index);
        int x = (width - WIDTH) / 2;
        int y = height / 3;

        name = new EditBox(font, x, y, WIDTH, HEIGHT, Component.translatable("alphamap.waypoint.name"));
        name.setMaxLength(32);
        name.setValue(waypoint.name());
        name.setResponder(text -> Waypoints.replace(index, current().renamed(text)));
        addRenderableWidget(name);
        setInitialFocus(name);

        int palette = Waypoints.PALETTE.length;
        int paletteWidth = palette * SWATCH;
        int paletteX = (width - paletteWidth) / 2;
        for (int i = 0; i < palette; i++) {
            int chosen = Waypoints.PALETTE[i];
            addRenderableWidget(Button.builder(Component.empty(), button -> {
                colour = chosen;
                Waypoints.replace(index, current().coloured(chosen));
            }).bounds(paletteX + i * SWATCH, y + 28, SWATCH - 2, SWATCH - 2).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("alphamap.waypoint.delete"), button -> {
            Waypoints.remove(index);
            onClose();
        }).bounds(x, y + 60, WIDTH, HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(x, y + 84, WIDTH, HEIGHT).build());
    }

    private Waypoint current() {
        return Waypoints.all().get(index);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        int palette = Waypoints.PALETTE.length;
        int paletteX = (width - palette * SWATCH) / 2;
        int y = height / 3 + 28;
        for (int i = 0; i < palette; i++) {
            int swatch = Waypoints.PALETTE[i];
            int left = paletteX + i * SWATCH;
            graphics.fill(left + 3, y + 3, left + SWATCH - 5, y + SWATCH - 5, 0xFF000000 | swatch);
            if (swatch == colour) {
                graphics.fill(left + 1, y + 1, left + SWATCH - 3, y + 2, 0xFFFFFFFF);
                graphics.fill(left + 1, y + SWATCH - 4, left + SWATCH - 3, y + SWATCH - 3, 0xFFFFFFFF);
            }
        }
        graphics.drawCenteredString(font, title, width / 2, height / 3 - 24, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }

    @Nullable
    public static WaypointScreen of(int index) {
        return index >= 0 && index < Waypoints.all().size() ? new WaypointScreen(index) : null;
    }
}

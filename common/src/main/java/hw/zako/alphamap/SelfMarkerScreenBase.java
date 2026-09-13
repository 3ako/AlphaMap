package hw.zako.alphamap;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public abstract class SelfMarkerScreenBase extends Screen {

    private static final int WIDTH = 220;
    private static final int HEIGHT = 20;
    private static final int GAP = 24;
    private static final int SWATCH = 24;
    private static final int PREVIEW = 34;

    private final @Nullable Screen parent;

    protected SelfMarkerScreenBase(@Nullable Screen parent) {
        super(Component.translatable("alphamap.self.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MapSettings settings = MapSettings.get();
        int x = (width - WIDTH) / 2;
        int y = height / 4;

        addRenderableWidget(Button.builder(shape(settings), button -> {
            SelfShape[] shapes = SelfShape.values();
            settings.selfShape(shapes[(settings.selfShape().ordinal() + 1) % shapes.length]);
            settings.clampAndSave();
            button.setMessage(shape(settings));
        }).bounds(x, y, WIDTH, HEIGHT).build());

        addRenderableWidget(new SettingsSlider(x, y + GAP, WIDTH, HEIGHT,
                scaleToSlider(settings.selfScale())) {
            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("alphamap.self.size",
                        Math.round(sliderToScale(value) * 100) + "%"));
            }

            @Override
            protected void applyValue() {
                settings.selfScale(sliderToScale(value));
                settings.clampAndSave();
            }
        });

        int palette = Waypoints.PALETTE.length;
        int paletteX = (width - palette * SWATCH) / 2;
        for (int i = 0; i < palette; i++) {
            int chosen = Waypoints.PALETTE[i];
            addRenderableWidget(Button.builder(Component.empty(), button -> {
                settings.selfColour(chosen);
                settings.clampAndSave();
            }).bounds(paletteX + i * SWATCH, y + GAP * 2 + 8, SWATCH - 2, SWATCH - 2).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(x, y + GAP * 4 + PREVIEW, WIDTH, HEIGHT).build());
    }

    protected void paint(Canvas canvas) {
        MapSettings settings = MapSettings.get();
        int y = height / 4;

        int palette = Waypoints.PALETTE.length;
        int paletteX = (width - palette * SWATCH) / 2;
        int row = y + GAP * 2 + 8;
        for (int i = 0; i < palette; i++) {
            int swatch = Waypoints.PALETTE[i];
            int left = paletteX + i * SWATCH;
            canvas.fill(left + 3, row + 3, left + SWATCH - 5, row + SWATCH - 5, 0xFF000000 | swatch);
            if (swatch == settings.selfColour()) {
                canvas.fill(left + 1, row + 1, left + SWATCH - 3, row + 2, 0xFFFFFFFF);
                canvas.fill(left + 1, row + SWATCH - 4, left + SWATCH - 3, row + SWATCH - 3, 0xFFFFFFFF);
            }
        }

        int middle = y + GAP * 3 + 8 + PREVIEW / 2;
        canvas.fill(width / 2 - PREVIEW, middle - PREVIEW / 2,
                width / 2 + PREVIEW, middle + PREVIEW / 2, 0x60000000);
        SelfMark.draw(canvas, settings.selfShape(), width / 2, middle,
                settings.selfScale(), settings.selfColour(), 0xFF000000, 0.0f);

        canvas.centered(font, title, width / 2, y - 24, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        Vanilla.setScreen(minecraft, parent);
    }

    private static Component shape(MapSettings settings) {
        return Component.translatable(
                "alphamap.self.shape." + settings.selfShape().name().toLowerCase());
    }

    private static double scaleToSlider(double scale) {
        return (Math.clamp(scale, MapSettings.MIN_SCALE, MapSettings.MAX_SCALE) - MapSettings.MIN_SCALE)
                / (MapSettings.MAX_SCALE - MapSettings.MIN_SCALE);
    }

    private static double sliderToScale(double slider) {
        return MapSettings.MIN_SCALE + slider * (MapSettings.MAX_SCALE - MapSettings.MIN_SCALE);
    }
}

package hw.zako.alphamap;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class MapSettingsScreen extends Screen {

    private static final int WIDTH = 170;
    private static final int HEIGHT = 20;
    private static final int GAP = 24;
    private static final int COLUMN_GAP = 6;

    private static final double LABEL_ZOOM_MAX = 9.0;

    private final @Nullable Screen parent;

    public MapSettingsScreen(@Nullable Screen parent) {
        super(Component.translatable("alphamap.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MapSettings settings = MapSettings.get();
        int left = (width - WIDTH * 2 - COLUMN_GAP) / 2;
        int right = left + WIDTH + COLUMN_GAP;
        int top = height / 4;

        addRenderableWidget(new SettingsSlider(left, top, WIDTH, HEIGHT, settings.opacity()) {
            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("alphamap.settings.opacity",
                        Math.round(value * 100) + "%"));
            }

            @Override
            protected void applyValue() {
                settings.opacity(value);
                settings.clampAndSave();
            }
        });

        addRenderableWidget(new SettingsSlider(left, top + GAP, WIDTH, HEIGHT, settings.gridOpacity()) {
            @Override
            protected void updateMessage() {
                setMessage(value == 0
                        ? Component.translatable("alphamap.settings.grid.off")
                        : Component.translatable("alphamap.settings.grid",
                                Math.round(value * 100) + "%"));
            }

            @Override
            protected void applyValue() {
                settings.gridOpacity(value);
                settings.clampAndSave();
            }
        });

        addRenderableWidget(new SettingsSlider(left, top + GAP * 2, WIDTH, HEIGHT,
                labelZoomToSlider(settings.labelZoom())) {
            @Override
            protected void updateMessage() {
                double zoom = sliderToLabelZoom(value);
                setMessage(zoom >= LABEL_ZOOM_MAX
                        ? Component.translatable("alphamap.settings.labels.never")
                        : Component.translatable("alphamap.settings.labels",
                                String.format("%.1f", zoom)));
            }

            @Override
            protected void applyValue() {
                settings.labelZoom(sliderToLabelZoom(value));
                settings.clampAndSave();
            }
        });

        addRenderableWidget(new SettingsSlider(left, top + GAP * 3, WIDTH, HEIGHT,
                scaleToSlider(settings.markerScale())) {
            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("alphamap.settings.markers",
                        Math.round(sliderToScale(value) * 100) + "%"));
            }

            @Override
            protected void applyValue() {
                settings.markerScale(sliderToScale(value));
                settings.clampAndSave();
            }
        });

        addRenderableWidget(new SettingsSlider(left, top + GAP * 4, WIDTH, HEIGHT,
                scaleToSlider(settings.worldMarkerScale())) {
            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("alphamap.settings.markers.world",
                        Math.round(sliderToScale(value) * 100) + "%"));
            }

            @Override
            protected void applyValue() {
                settings.worldMarkerScale(sliderToScale(value));
                settings.clampAndSave();
            }
        });

        addRenderableWidget(Button.builder(Component.translatable("alphamap.settings.world"),
                        button -> Vanilla.setScreen(minecraft, MarkerListScreen.world(this)))
                .bounds(right, top, WIDTH, HEIGHT)
                .build());

        addRenderableWidget(Button.builder(death(settings), button -> {
            settings.deathPoint(!settings.deathPoint());
            settings.clampAndSave();
            button.setMessage(death(settings));
        }).bounds(right, top + GAP, WIDTH, HEIGHT).build());

        addRenderableWidget(Button.builder(compass(settings), button -> {
            settings.compass(!settings.compass());
            settings.clampAndSave();
            button.setMessage(compass(settings));
        }).bounds(right, top + GAP * 2, WIDTH, HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("alphamap.settings.minimap"),
                        button -> Vanilla.setScreen(minecraft, new MinimapSettingsScreen(this)))
                .bounds(right, top + GAP * 3, WIDTH, HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("alphamap.settings.self"),
                        button -> Vanilla.setScreen(minecraft, new SelfMarkerScreen(this)))
                .bounds(right, top + GAP * 4, WIDTH, HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds((width - WIDTH) / 2, top + GAP * 6, WIDTH, HEIGHT)
                .build());
    }

    @Override
    public void onClose() {
        Vanilla.setScreen(minecraft, parent);
    }

    private static Component death(MapSettings settings) {
        return Component.translatable(settings.deathPoint()
                ? "alphamap.settings.death.on"
                : "alphamap.settings.death.off");
    }

    private static Component compass(MapSettings settings) {
        return Component.translatable(settings.compass()
                ? "alphamap.settings.compass.on"
                : "alphamap.settings.compass.off");
    }

    private static double scaleToSlider(double scale) {
        return (Math.clamp(scale, MapSettings.MIN_SCALE, MapSettings.MAX_SCALE) - MapSettings.MIN_SCALE)
                / (MapSettings.MAX_SCALE - MapSettings.MIN_SCALE);
    }

    private static double sliderToScale(double slider) {
        return MapSettings.MIN_SCALE + slider * (MapSettings.MAX_SCALE - MapSettings.MIN_SCALE);
    }

    private static double labelZoomToSlider(double zoom) {
        return (Math.clamp(zoom, 1.0, LABEL_ZOOM_MAX) - 1.0) / (LABEL_ZOOM_MAX - 1.0);
    }

    private static double sliderToLabelZoom(double slider) {
        return 1.0 + slider * (LABEL_ZOOM_MAX - 1.0);
    }
}

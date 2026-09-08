package hw.zako.alphamap;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class MapSettingsScreen extends Screen {

    private static final int WIDTH = 220;
    private static final int HEIGHT = 20;
    private static final int GAP = 24;

    private static final double LABEL_ZOOM_MAX = 9.0;

    private final @Nullable Screen parent;

    public MapSettingsScreen(@Nullable Screen parent) {
        super(Component.translatable("alphamap.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MapSettings settings = MapSettings.get();
        int x = (width - WIDTH) / 2;
        int y = height / 3;

        addRenderableWidget(new Slider(x, y, settings.opacity()) {
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

        addRenderableWidget(new Slider(x, y + GAP, settings.gridOpacity()) {
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

        addRenderableWidget(new Slider(x, y + GAP * 2, labelZoomToSlider(settings.labelZoom())) {
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

        addRenderableWidget(new Slider(x, y + GAP * 3, scaleToSlider(settings.markerScale())) {
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

        addRenderableWidget(new Slider(x, y + GAP * 4, scaleToSlider(settings.worldMarkerScale())) {
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

        addRenderableWidget(Button.builder(beds(settings), button -> {
            settings.worldBeds(!settings.worldBeds());
            settings.clampAndSave();
            button.setMessage(beds(settings));
        }).bounds(x, y + GAP * 5, WIDTH, HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(x, y + GAP * 7, WIDTH, HEIGHT)
                .build());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private static Component beds(MapSettings settings) {
        return Component.translatable(settings.worldBeds()
                ? "alphamap.settings.beds.on"
                : "alphamap.settings.beds.off");
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

    private abstract static class Slider extends AbstractSliderButton {

        Slider(int x, int y, double value) {
            super(x, y, WIDTH, HEIGHT, Component.empty(), value);
            updateMessage();
        }
    }
}

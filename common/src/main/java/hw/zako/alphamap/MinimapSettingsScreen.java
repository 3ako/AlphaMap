package hw.zako.alphamap;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class MinimapSettingsScreen extends Screen {

    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;
    private static final int GAP = 24;
    private static final int COLUMN_GAP = 6;

    private static final int SIZE_STEP = 8;
    private static final int BLOCKS_STEP = 16;

    private final @Nullable Screen parent;

    public MinimapSettingsScreen(@Nullable Screen parent) {
        super(Component.translatable("alphamap.minimap.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MapSettings settings = MapSettings.get();
        int left = (width - WIDTH * 2 - COLUMN_GAP) / 2;
        int right = left + WIDTH + COLUMN_GAP;
        int top = height / 4;

        toggle(left, top, settings::minimap, settings::minimap,
                "alphamap.minimap.on", "alphamap.minimap.off");

        addRenderableWidget(Button.builder(shape(settings), button -> {
            settings.minimapShape(settings.minimapShape() == MinimapShape.CIRCLE
                    ? MinimapShape.SQUARE
                    : MinimapShape.CIRCLE);
            settings.clampAndSave();
            button.setMessage(shape(settings));
        }).bounds(left, top + GAP, WIDTH, HEIGHT).build());

        toggle(left, top + GAP * 2, settings::minimapNorth, settings::minimapNorth,
                "alphamap.minimap.north.on", "alphamap.minimap.north.off");
        toggle(left, top + GAP * 3, settings::minimapCoordinates, settings::minimapCoordinates,
                "alphamap.minimap.coordinates.on", "alphamap.minimap.coordinates.off");

        addRenderableWidget(new SettingsSlider(left, top + GAP * 4, WIDTH, HEIGHT,
                fraction(settings.minimapSize(), MapSettings.MIN_MINIMAP_SIZE, MapSettings.MAX_MINIMAP_SIZE)) {
            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("alphamap.minimap.size", size(value)));
            }

            @Override
            protected void applyValue() {
                settings.minimapSize(size(value));
                settings.clampAndSave();
            }
        });

        addRenderableWidget(new SettingsSlider(left, top + GAP * 5, WIDTH, HEIGHT,
                fraction(settings.minimapBlocks(), MapSettings.MIN_MINIMAP_BLOCKS, MapSettings.MAX_MINIMAP_BLOCKS)) {
            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("alphamap.minimap.blocks", blocks(value)));
            }

            @Override
            protected void applyValue() {
                settings.minimapBlocks(blocks(value));
                settings.clampAndSave();
            }
        });

        addRenderableWidget(new SettingsSlider(left, top + GAP * 6, WIDTH, HEIGHT,
                scaleToSlider(settings.minimapMarkerScale())) {
            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("alphamap.minimap.marker.size",
                        Math.round(sliderToScale(value) * 100) + "%"));
            }

            @Override
            protected void applyValue() {
                settings.minimapMarkerScale(sliderToScale(value));
                settings.clampAndSave();
            }
        });

        addRenderableWidget(Button.builder(Component.translatable("alphamap.minimap.mobs"),
                        button -> Vanilla.setScreen(minecraft, new MobListScreen(this)))
                .bounds(right, top, WIDTH, HEIGHT)
                .build());

        toggle(right, top + GAP, settings::compassOutside, settings::compassOutside,
                "alphamap.minimap.compass.outside", "alphamap.minimap.compass.inside");

        toggle(right, top + GAP * 2, settings::minimapCaves, settings::minimapCaves,
                "alphamap.minimap.caves.on", "alphamap.minimap.caves.off");

        toggle(right, top + GAP * 3, settings::minimapWaypoints, settings::minimapWaypoints,
                "alphamap.minimap.waypoints.on", "alphamap.minimap.waypoints.off");

        toggle(right, top + GAP * 4, settings::minimapMarkers, settings::minimapMarkers,
                "alphamap.minimap.markers.on", "alphamap.minimap.markers.off");

        addRenderableWidget(Button.builder(Component.translatable("alphamap.markers.list"),
                        button -> Vanilla.setScreen(minecraft, MarkerListScreen.minimap(this)))
                .bounds(right, top + GAP * 5, WIDTH, HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("alphamap.minimap.move"),
                        button -> Vanilla.setScreen(minecraft, new MinimapMoveScreen(this)))
                .bounds(right, top + GAP * 6, WIDTH, HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds((width - WIDTH) / 2, top + GAP * 7, WIDTH, HEIGHT)
                .build());
    }

    @Override
    public void onClose() {
        Vanilla.setScreen(minecraft, parent);
    }

    private void kind(int x, int y, BooleanSupplier shown, Consumer<Boolean> setShown,
                      BooleanSupplier heads, Consumer<Boolean> setHeads, String key) {
        addRenderableWidget(Button.builder(kindLabel(shown, heads, key), button -> {
            if (!shown.getAsBoolean()) {
                setShown.accept(true);
                setHeads.accept(false);
            } else if (!heads.getAsBoolean()) {
                setHeads.accept(true);
            } else {
                setShown.accept(false);
            }
            MapSettings.get().clampAndSave();
            button.setMessage(kindLabel(shown, heads, key));
        }).bounds(x, y, WIDTH, HEIGHT).build());
    }

    private Component kindLabel(BooleanSupplier shown, BooleanSupplier heads, String key) {
        String state = !shown.getAsBoolean() ? "off" : heads.getAsBoolean() ? "heads" : "dots";
        return Component.translatable("alphamap.minimap." + key + "." + state);
    }

    private void toggle(int x, int y, BooleanSupplier reader, Consumer<Boolean> writer,
                        String on, String off) {
        addRenderableWidget(Button.builder(label(reader.getAsBoolean(), on, off), button -> {
            writer.accept(!reader.getAsBoolean());
            MapSettings.get().clampAndSave();
            button.setMessage(label(reader.getAsBoolean(), on, off));
        }).bounds(x, y, WIDTH, HEIGHT).build());
    }

    private static Component label(boolean value, String on, String off) {
        return Component.translatable(value ? on : off);
    }

    private static Component shape(MapSettings settings) {
        return Component.translatable(settings.minimapShape() == MinimapShape.CIRCLE
                ? "alphamap.minimap.shape.circle"
                : "alphamap.minimap.shape.square");
    }

    private static double scaleToSlider(double scale) {
        return (Math.clamp(scale, MapSettings.MIN_SCALE, MapSettings.MAX_SCALE) - MapSettings.MIN_SCALE)
                / (MapSettings.MAX_SCALE - MapSettings.MIN_SCALE);
    }

    private static double sliderToScale(double slider) {
        return MapSettings.MIN_SCALE + slider * (MapSettings.MAX_SCALE - MapSettings.MIN_SCALE);
    }

    private static int size(double slider) {
        return stepped(slider, MapSettings.MIN_MINIMAP_SIZE, MapSettings.MAX_MINIMAP_SIZE, SIZE_STEP);
    }

    private static int blocks(double slider) {
        return stepped(slider, MapSettings.MIN_MINIMAP_BLOCKS, MapSettings.MAX_MINIMAP_BLOCKS, BLOCKS_STEP);
    }

    private static int stepped(double slider, int min, int max, int step) {
        return min + (int) Math.round(slider * ((max - min) / step)) * step;
    }

    private static double fraction(int value, int min, int max) {
        return (Math.clamp(value, min, max) - min) / (double) (max - min);
    }
}

package hw.zako.alphamap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@FieldDefaults(level = AccessLevel.PRIVATE)
public final class MapSettings {

    public static final double MIN_SCALE = 0.5;
    public static final double MAX_SCALE = 3.0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @NonFinal
    private static MapSettings loaded = new MapSettings();

    @NonFinal
    double opacity = 0.9;

    @NonFinal
    boolean grid = true;

    @NonFinal
    int gridBlocks = 250;

    @NonFinal
    double gridOpacity = 0.35;

    @NonFinal
    double labelZoom = 2.0;

    @NonFinal
    boolean worldBeds = true;

    @NonFinal
    double markerScale = 1.0;

    @NonFinal
    double worldMarkerScale = 1.0;

    public static MapSettings get() {
        return loaded;
    }

    public static MapSettings load() {
        MapSettings settings = read(file());
        settings.clampAndSave();
        loaded = settings;
        return settings;
    }

    public void opacity(double value) {
        opacity = value;
    }

    public void labelZoom(double value) {
        labelZoom = value;
    }

    public void clampAndSave() {
        opacity = Math.clamp(opacity, 0.0, 1.0);
        gridOpacity = Math.clamp(gridOpacity, 0.0, 1.0);
        gridBlocks = Math.max(0, gridBlocks);
        labelZoom = Math.max(1.0, labelZoom);
        markerScale = Math.clamp(markerScale, MIN_SCALE, MAX_SCALE);
        worldMarkerScale = Math.clamp(worldMarkerScale, MIN_SCALE, MAX_SCALE);
        write(file(), this);
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("alphamap.json");
    }

    public double opacity() {
        return opacity;
    }

    public double labelZoom() {
        return labelZoom;
    }

    public double markerScale() {
        return markerScale;
    }

    public void markerScale(double value) {
        markerScale = value;
    }

    public double worldMarkerScale() {
        return worldMarkerScale;
    }

    public void worldMarkerScale(double value) {
        worldMarkerScale = value;
    }

    public int markerPixels() {
        return (int) Math.round(16 * markerScale);
    }

    public int worldMarkerPixels() {
        return (int) Math.round(16 * worldMarkerScale);
    }

    public boolean worldBeds() {
        return worldBeds;
    }

    public void worldBeds(boolean value) {
        worldBeds = value;
    }

    public boolean grid() {
        return grid && gridBlocks > 0 && gridOpacity > 0;
    }

    public int gridBlocks() {
        return gridBlocks;
    }

    public double gridOpacity() {
        return gridOpacity;
    }

    public void gridOpacity(double value) {
        gridOpacity = value;
    }

    public int gridAlpha() {
        return (int) Math.round(gridOpacity * 255) << 24;
    }

    public int alpha() {
        return (int) Math.round(opacity * 255) << 24;
    }

    private static MapSettings read(Path file) {
        if (!Files.exists(file)) return new MapSettings();
        try (var reader = Files.newBufferedReader(file)) {
            MapSettings settings = GSON.fromJson(reader, MapSettings.class);
            return settings != null ? settings : new MapSettings();
        } catch (IOException | RuntimeException broken) {
            return new MapSettings();
        }
    }

    private static void write(Path file, MapSettings settings) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(settings));
        } catch (IOException unwritable) {
        }
    }
}

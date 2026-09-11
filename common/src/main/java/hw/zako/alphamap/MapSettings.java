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
import java.util.HashSet;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE)
public final class MapSettings {

    public static final double MIN_SCALE = 0.5;
    public static final double MAX_SCALE = 3.0;

    public static final int MIN_MINIMAP_SIZE = 64;
    public static final int MAX_MINIMAP_SIZE = 256;
    public static final int MIN_MINIMAP_BLOCKS = 32;
    public static final int MAX_MINIMAP_BLOCKS = 512;

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

    // Остался ради старых конфигов: из него собирается первый список меток в мире.
    @NonFinal
    boolean worldBeds = true;

    // В мире по умолчанию висят только кровати, поэтому там храним показанное, а не скрытое.
    @NonFinal
    Set<String> worldShown;

    @NonFinal
    double markerScale = 1.0;

    @NonFinal
    double worldMarkerScale = 1.0;

    @NonFinal
    boolean minimap = true;

    @NonFinal
    MinimapShape minimapShape = MinimapShape.CIRCLE;

    @NonFinal
    boolean minimapNorth = true;

    @NonFinal
    boolean minimapPlayers = true;

    @NonFinal
    boolean minimapHostiles = true;

    @NonFinal
    boolean minimapPassives = false;

    @NonFinal
    boolean minimapWaypoints = true;

    @NonFinal
    boolean minimapMarkers = true;

    @NonFinal
    double minimapMarkerScale = 1.0;

    // Храним скрытое, а не показанное: типы, которых мы ещё не знаем, тогда видны сразу.
    @NonFinal
    Set<String> minimapHidden = new HashSet<>();

    @NonFinal
    boolean deathPoint = true;

    @NonFinal
    boolean compass = false;

    @NonFinal
    boolean minimapCoordinates = true;

    @NonFinal
    int minimapSize = 128;

    @NonFinal
    int minimapBlocks = 96;

    // Доли экрана, а не пиксели: миникарта остаётся на месте при смене разрешения и gui scale.
    @NonFinal
    double minimapX = 0.98;

    @NonFinal
    double minimapY = 0.02;

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
        if (minimapShape == null) minimapShape = MinimapShape.CIRCLE;
        if (minimapHidden == null) minimapHidden = new HashSet<>();
        if (worldShown == null) {
            worldShown = new HashSet<>();
            if (worldBeds) worldShown.add("bed");
        }
        minimapMarkerScale = Math.clamp(minimapMarkerScale, MIN_SCALE, MAX_SCALE);
        minimapSize = Math.clamp(minimapSize, MIN_MINIMAP_SIZE, MAX_MINIMAP_SIZE);
        minimapBlocks = Math.clamp(minimapBlocks, MIN_MINIMAP_BLOCKS, MAX_MINIMAP_BLOCKS);
        minimapX = Math.clamp(minimapX, 0.0, 1.0);
        minimapY = Math.clamp(minimapY, 0.0, 1.0);
        write(file(), this);
    }

    public boolean minimap() {
        return minimap;
    }

    public void minimap(boolean value) {
        minimap = value;
    }

    public MinimapShape minimapShape() {
        return minimapShape;
    }

    public void minimapShape(MinimapShape value) {
        minimapShape = value;
    }

    public boolean minimapNorth() {
        return minimapNorth;
    }

    public void minimapNorth(boolean value) {
        minimapNorth = value;
    }

    public boolean minimapPlayers() {
        return minimapPlayers;
    }

    public void minimapPlayers(boolean value) {
        minimapPlayers = value;
    }

    public boolean minimapHostiles() {
        return minimapHostiles;
    }

    public void minimapHostiles(boolean value) {
        minimapHostiles = value;
    }

    public boolean minimapPassives() {
        return minimapPassives;
    }

    public void minimapPassives(boolean value) {
        minimapPassives = value;
    }

    public boolean minimapWaypoints() {
        return minimapWaypoints;
    }

    public void minimapWaypoints(boolean value) {
        minimapWaypoints = value;
    }

    public boolean minimapMarkers() {
        return minimapMarkers;
    }

    public void minimapMarkers(boolean value) {
        minimapMarkers = value;
    }

    public boolean compass() {
        return compass;
    }

    public void compass(boolean value) {
        compass = value;
    }

    public boolean deathPoint() {
        return deathPoint;
    }

    public void deathPoint(boolean value) {
        deathPoint = value;
    }

    public double minimapMarkerScale() {
        return minimapMarkerScale;
    }

    public void minimapMarkerScale(double value) {
        minimapMarkerScale = value;
    }

    public int minimapMarkerPixels() {
        return (int) Math.round(16 * minimapMarkerScale);
    }

    public Set<String> minimapHidden() {
        return minimapHidden;
    }

    public Set<String> worldShown() {
        return worldShown != null ? worldShown : Set.of();
    }

    public boolean worldMarkerShown(String icon) {
        return worldShown != null && worldShown.contains(icon);
    }

    public void worldMarkerShown(String icon, boolean shown) {
        if (worldShown == null) worldShown = new HashSet<>();
        if (shown) worldShown.add(icon);
        else worldShown.remove(icon);
    }

    public boolean minimapMarkerShown(String icon) {
        return !minimapHidden.contains(icon);
    }

    public void minimapMarkerShown(String icon, boolean shown) {
        if (shown) minimapHidden.remove(icon);
        else minimapHidden.add(icon);
    }

    public boolean minimapCoordinates() {
        return minimapCoordinates;
    }

    public void minimapCoordinates(boolean value) {
        minimapCoordinates = value;
    }

    public int minimapSize() {
        return minimapSize;
    }

    public void minimapSize(int value) {
        minimapSize = value;
    }

    public int minimapBlocks() {
        return minimapBlocks;
    }

    public void minimapBlocks(int value) {
        minimapBlocks = value;
    }

    public double minimapX() {
        return minimapX;
    }

    public double minimapY() {
        return minimapY;
    }

    public void minimapPosition(double x, double y) {
        minimapX = Math.clamp(x, 0.0, 1.0);
        minimapY = Math.clamp(y, 0.0, 1.0);
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

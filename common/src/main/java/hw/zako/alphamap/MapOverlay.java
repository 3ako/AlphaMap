package hw.zako.alphamap;

import hw.zako.alphamap.protocol.AtlasGeometry;
import hw.zako.alphamap.protocol.ServerMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class MapOverlay {

    private static final float FILL = 0.9f;

    private static final int TOOL_SIZE = 16;
    private static final int TOOL_GAP = 4;
    private static final int HIT_RADIUS = 10;

    private static final int TOOL_ON = 0xFF62E8FF;
    private static final int TOOL_OFF = 0xFF9A9A9A;
    private static final int SKETCH = 0xFFFF3B30;

    private static final int GRID = 0xFFFFFF;

    private static final int NAMED_LABEL = 0x62E8FF;
    private static final int LABEL = 0xFFFFFF;
    private static final int PLAYER = 0xFF4040;
    private static final int CURSOR = 0xFFFFFFFF;
    private static final int OPAQUE = 0xFF000000;
    private static final int OUTLINE = 0x000000;

    AtlasClient atlas;
    MapSettings settings;

    public void draw(Canvas canvas) {
        if (!AlphaMapClient.mapOpen()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        AtlasGeometry geometry = atlas.geometry();
        if (geometry == null) {
            message(canvas, client, atlas.status());
            return;
        }

        int side = (int) (Math.min(canvas.width(), canvas.height()) * FILL);
        int left = (canvas.width() - side) / 2;
        int top = (canvas.height() - side) / 2;

        MapInput.update(client, true);
        MapInput.viewport(left, top, side, geometry.visiblePixels());
        MapInput.mouse(client);

        int alpha = settings.alpha();

        double zoom = MapInput.zoom();
        double visible = geometry.visiblePixels();
        float scale = (float) (side * zoom / visible);
        float mapX = (float) (left + side / 2.0
                - (geometry.visibleOriginPixel() + MapInput.focusX() * visible) * scale);
        float mapY = (float) (top + side / 2.0
                - (geometry.visibleOriginPixel() + MapInput.focusY() * visible) * scale);

        canvas.scissorOn(left, top, left + side, top + side);

        canvas.push(mapX, mapY, scale);

        for (int tileZ = 0; tileZ < geometry.tilesPerSide(); tileZ++) {
            for (int tileX = 0; tileX < geometry.tilesPerSide(); tileX++) {
                Identifier texture = atlas.texture(tileX, tileZ);
                if (texture == null) continue;

                canvas.blit(texture,
                        tileX * geometry.tilePixels(), tileZ * geometry.tilePixels(),
                        geometry.tilePixels(), alpha | 0xFFFFFF);
            }
        }
        canvas.pop();

        grid(canvas, client, geometry, settings, mapX, mapY, scale, left, top, side);

        sketch(canvas, mapX, mapY, scale);
        waypoints(canvas, client, geometry, settings, mapX, mapY, scale, alpha);

        boolean labels = zoom >= settings.labelZoom();
        for (ServerMessage.Marker marker : atlas.markers()) {
            marker(canvas, client, geometry, settings, marker, mapX, mapY, scale, alpha, labels);
        }
        death(canvas, client, geometry, settings, mapX, mapY, scale);
        player(canvas, client, geometry, mapX, mapY, scale, alpha);
        canvas.scissorOff();

        if (settings.compass()) Compass.square(canvas, client.font, left, top, side);
        coordinates(canvas, client, geometry, settings, mapX, mapY, scale, left, top, side);
        tools(canvas, left, top);
        if (MapInput.cursorActive()) {
            act(client, geometry, settings, mapX, mapY, scale, left, top, side);
            cursor(canvas, client);
        }
    }

    private static void act(Minecraft client, AtlasGeometry geometry, MapSettings settings,
                            float mapX, float mapY, float scale, int left, int top, int side) {
        double px = MapInput.pointerX();
        double py = MapInput.pointerY();

        if ((MapInput.pressed() || MapInput.usePressed()) && overTools(px, py, left, top)) {
            if (MapInput.pressed()) pickTool(px, left, top);
            return;
        }
        if (px < left || px > left + side || py < top || py > top + side) return;

        double atlasX = (px - mapX) / scale;
        double atlasY = (py - mapY) / scale;

        if (MapInput.usePressed()) {
            int hit = waypointAt(geometry, settings, px, py, mapX, mapY, scale);
            if (hit >= 0) {
                Waypoints.remove(hit);
                return;
            }

            double blockX = geometry.originX() + atlasX * geometry.blocksPerPixel();
            double blockZ = geometry.originZ() + atlasY * geometry.blocksPerPixel();
            Waypoints.add(new Waypoint(Math.round(blockX) + 0.5, client.player.getY(),
                    Math.round(blockZ) + 0.5, Waypoints.defaultName(), Waypoints.PALETTE[0]));
            return;
        }

        switch (MapInput.tool()) {
            case PENCIL -> {
                if (MapInput.pressed()) MapSketch.start();
                if (MapInput.holding()) MapSketch.extend(atlasX, atlasY);
                if (MapInput.letGo()) MapSketch.finish();
            }
            case PAN -> {
                if (!MapInput.pressed()) return;

                int hit = waypointAt(geometry, settings, px, py, mapX, mapY, scale);
                if (hit >= 0) Vanilla.setScreen(client, WaypointScreen.of(hit));
            }
        }
    }

    private static int waypointAt(AtlasGeometry geometry, MapSettings settings,
                                  double px, double py, float mapX, float mapY, float scale) {
        double reach = Math.max(HIT_RADIUS, settings.markerPixels() / 2.0 + 4);
        double best = reach * reach;
        int hit = -1;

        var all = Waypoints.all();
        for (int i = 0; i < all.size(); i++) {
            Waypoint waypoint = all.get(i);
            double dx = mapX + geometry.pixelX(waypoint.x()) * scale - px;
            double dy = mapY + geometry.pixelZ(waypoint.z()) * scale - py;
            double distance = dx * dx + dy * dy;
            if (distance > best) continue;

            best = distance;
            hit = i;
        }
        return hit;
    }

    private static boolean overTools(double px, double py, int left, int top) {
        return py >= top + TOOL_GAP && py <= top + TOOL_GAP + TOOL_SIZE
                && px >= left + TOOL_GAP
                && px <= left + TOOL_GAP + MapTool.values().length * (TOOL_SIZE + 2) + TOOL_SIZE + 2;
    }

    private static void pickTool(double px, int left, int top) {
        int slot = (int) ((px - left - TOOL_GAP) / (TOOL_SIZE + 2));
        MapTool[] tools = MapTool.values();
        if (slot >= 0 && slot < tools.length) {
            MapInput.tool(tools[slot]);
            return;
        }
        if (slot == tools.length) MapSketch.clear();
    }

    private static void coordinates(Canvas canvas, Minecraft client, AtlasGeometry geometry,
                                    MapSettings settings, float mapX, float mapY, float scale,
                                    int left, int top, int side) {
        String text = "%d, %d, %d".formatted(
                Math.round(client.player.getX()),
                Math.round(client.player.getY()),
                Math.round(client.player.getZ()));

        if (MapInput.cursorActive()) {
            double px = MapInput.pointerX();
            double py = MapInput.pointerY();
            if (px >= left && px <= left + side && py >= top && py <= top + side) {
                long blockX = Math.round(geometry.originX() + (px - mapX) / scale * geometry.blocksPerPixel());
                long blockZ = Math.round(geometry.originZ() + (py - mapY) / scale * geometry.blocksPerPixel());
                text += "   •   " + blockX + ", " + blockZ + cell(geometry, settings, blockX, blockZ);
            }
        }

        int width = client.font.width(text);
        int centre = left + side / 2;
        int lineTop = top + side + 4;
        canvas.fill(centre - width / 2 - 4, lineTop - 2, centre + width / 2 + 4, lineTop + 11, 0x90000000);
        canvas.centered(client.font, text, centre, lineTop, 0xFFFFFFFF);
    }

    private static String cell(AtlasGeometry geometry, MapSettings settings, long blockX, long blockZ) {
        if (!settings.grid()) return "";

        int step = settings.gridBlocks();
        int radius = geometry.radius();
        int first = -((radius + step - 1) / step) * step;

        long column = Math.floorDiv(blockX - first, step);
        long row = Math.floorDiv(blockZ - first, step);
        if (column < 0 || row < 0) return "";
        return "  " + column((int) column) + (row + 1);
    }

    private static String icon(MapTool tool) {
        return switch (tool) {
            case PAN -> "hand";
            case PENCIL -> "pencil";
        };
    }

    private static void tools(Canvas canvas, int left, int top) {
        if (!MapInput.cursorActive()) return;

        MapTool[] tools = MapTool.values();
        for (int i = 0; i < tools.length; i++) {
            int x = left + TOOL_GAP + i * (TOOL_SIZE + 2);
            int y = top + TOOL_GAP;
            boolean active = MapInput.tool() == tools[i];

            canvas.fill(x, y, x + TOOL_SIZE, y + TOOL_SIZE, active ? 0xE0303030 : 0xB0101010);
            canvas.fill(x, y, x + TOOL_SIZE, y + 1, active ? TOOL_ON : TOOL_OFF);
            glyph(canvas, icon(tools[i]), x, y, active ? 0xFFFFFFFF : 0xB0FFFFFF);
        }

        int x = left + TOOL_GAP + tools.length * (TOOL_SIZE + 2);
        int y = top + TOOL_GAP;
        canvas.fill(x, y, x + TOOL_SIZE, y + TOOL_SIZE, 0xB0101010);
        glyph(canvas, "trash", x, y, MapSketch.isEmpty() ? 0x70FFFFFF : 0xFFFF6B6B);
    }

    private static void glyph(Canvas canvas, String icon, int x, int y, int tint) {
        int inset = 2;
        int size = TOOL_SIZE - inset * 2;
        canvas.blit(Identifier.fromNamespaceAndPath("alphamap", "textures/tool/" + icon + ".png"),
                x + inset, y + inset, size, tint);
    }

    private static void sketch(Canvas canvas, float mapX, float mapY, float scale) {
        for (var stroke : MapSketch.strokes()) {
            for (int i = 1; i < stroke.size(); i++) {
                double[] from = stroke.get(i - 1);
                double[] to = stroke.get(i);
                line(canvas,
                        mapX + (float) from[0] * scale, mapY + (float) from[1] * scale,
                        mapX + (float) to[0] * scale, mapY + (float) to[1] * scale);
            }
        }
    }

    private static void line(Canvas canvas, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        int steps = (int) Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));
        for (int i = 0; i <= steps; i++) {
            int x = Math.round(x1 + dx * i / steps);
            int y = Math.round(y1 + dy * i / steps);
            canvas.fill(x - 1, y - 1, x + 2, y + 2, SKETCH);
        }
    }

    private static void waypoints(Canvas canvas, Minecraft client, AtlasGeometry geometry,
                                  MapSettings settings, float mapX, float mapY, float scale, int alpha) {
        int half = Math.max(2, settings.markerPixels() / 4);
        for (Waypoint waypoint : Waypoints.all()) {
            int x = (int) (mapX + geometry.pixelX(waypoint.x()) * scale);
            int y = (int) (mapY + geometry.pixelZ(waypoint.z()) * scale);
            int colour = alpha | waypoint.colour();

            canvas.fill(x - half, y - half, x + half, y + half, alpha | OUTLINE);
            canvas.fill(x - half + 1, y - half + 1, x + half - 1, y + half - 1, colour);

            if (!waypoint.name().isBlank()) {
                canvas.centered(client.font, waypoint.name(), x, y + half + 2, colour);
            }
        }
    }

    private static void grid(Canvas canvas, Minecraft client, AtlasGeometry geometry,
                             MapSettings settings, float mapX, float mapY, float scale,
                             int left, int top, int side) {
        if (!settings.grid()) return;

        int step = settings.gridBlocks();
        int alpha = settings.gridAlpha();
        int radius = geometry.radius();

        int first = -((radius + step - 1) / step) * step;
        int cells = (int) Math.ceil((2.0 * radius) / step);
        float cellPixels = (float) (step / (double) geometry.blocksPerPixel() * scale);

        for (int i = 0; i <= cells; i++) {
            int block = first + i * step;
            int x = (int) (mapX + geometry.pixelX(block) * scale);
            int y = (int) (mapY + geometry.pixelZ(block) * scale);
            canvas.fill(x, top, x + 1, top + side, GRID | alpha);
            canvas.fill(left, y, left + side, y + 1, GRID | alpha);
        }

        if (cellPixels < client.font.width("A1") + 6) return;

        for (int column = 0; column < cells; column++) {
            int x = (int) (mapX + geometry.pixelX(first + column * step) * scale);
            if (x + cellPixels < left || x > left + side) continue;

            for (int row = 0; row < cells; row++) {
                int y = (int) (mapY + geometry.pixelZ(first + row * step) * scale);
                if (y + cellPixels < top || y > top + side) continue;

                canvas.text(client.font, column(column) + (row + 1),
                        x + 3, y + 3, GRID | alpha);
            }
        }
    }

    private static String column(int index) {
        StringBuilder name = new StringBuilder();
        for (int n = index; n >= 0; n = n / 26 - 1) {
            name.insert(0, (char) ('A' + n % 26));
        }
        return name.toString();
    }

    private static void cursor(Canvas canvas, Minecraft client) {
        int x = (int) MapInput.cursorX(client);
        int y = (int) MapInput.cursorY(client);

        canvas.fill(x - 6, y - 1, x + 7, y + 2, OUTLINE | OPAQUE);
        canvas.fill(x - 1, y - 6, x + 2, y + 7, OUTLINE | OPAQUE);
        canvas.fill(x - 5, y, x + 6, y + 1, CURSOR);
        canvas.fill(x, y - 5, x + 1, y + 6, CURSOR);
    }

    private static void marker(Canvas canvas, Minecraft client, AtlasGeometry geometry,
                               MapSettings settings, ServerMessage.Marker marker,
                               float mapX, float mapY, float scale, int alpha, boolean label) {
        int x = (int) (mapX + geometry.pixelX(marker.x()) * scale);
        int y = (int) (mapY + geometry.pixelZ(marker.z()) * scale);

        Identifier icon = MarkerIcons.of(marker.icon());
        int size = settings.markerPixels();
        int half = size / 2;
        canvas.blit(icon, x - half, y - half, size, alpha | 0xFFFFFF);

        boolean named = !marker.label().isEmpty();
        if (!named && !label) return;

        Component text = named
                ? Component.literal(marker.label())
                : Component.translatable("alphamap.marker." + marker.icon());
        canvas.centered(client.font, text, x, y + half + 2,
                alpha | (named ? NAMED_LABEL : LABEL));
    }

    private static void death(Canvas canvas, Minecraft client, AtlasGeometry geometry,
                              MapSettings settings, float mapX, float mapY, float scale) {
        BlockPos place = DeathPoint.of(client, settings);
        if (place == null) return;

        DeathPoint.draw(canvas,
                (int) (mapX + geometry.pixelX(place.getX() + 0.5) * scale),
                (int) (mapY + geometry.pixelZ(place.getZ() + 0.5) * scale),
                5);
    }

    private static void player(Canvas canvas, Minecraft client, AtlasGeometry geometry,
                               float mapX, float mapY, float scale, int alpha) {
        int x = (int) (mapX + geometry.pixelX(client.player.getX()) * scale);
        int y = (int) (mapY + geometry.pixelZ(client.player.getZ()) * scale);

        canvas.fill(x - 4, y - 1, x + 5, y + 2, OUTLINE | alpha);
        canvas.fill(x - 1, y - 4, x + 2, y + 5, OUTLINE | alpha);
        canvas.fill(x - 3, y, x + 4, y + 1, PLAYER | alpha);
        canvas.fill(x, y - 3, x + 1, y + 4, PLAYER | alpha);
    }

    private static void message(Canvas canvas, Minecraft client, @Nullable Component text) {
        if (text == null) return;
        canvas.centered(client.font, text,
                canvas.width() / 2, canvas.height() / 2, 0xFFFFFFFF);
    }
}

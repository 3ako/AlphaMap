package hw.zako.alphamap;

import hw.zako.alphamap.protocol.AtlasGeometry;
import hw.zako.alphamap.protocol.ServerMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class MapOverlay implements HudElement {

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
    private static final int UNKNOWN_MARKER = 0xE8C24A;
    private static final int CURSOR = 0xFFFFFFFF;
    private static final int OPAQUE = 0xFF000000;
    private static final int OUTLINE = 0x000000;

    AtlasClient atlas;
    MapSettings settings;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        if (!AlphaMapClient.mapOpen()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        AtlasGeometry geometry = atlas.geometry();
        if (geometry == null) {
            message(graphics, client, atlas.status());
            return;
        }

        int side = (int) (Math.min(graphics.guiWidth(), graphics.guiHeight()) * FILL);
        int left = (graphics.guiWidth() - side) / 2;
        int top = (graphics.guiHeight() - side) / 2;

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

        graphics.enableScissor(left, top, left + side, top + side);

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(mapX, mapY);
        pose.scale(scale, scale);

        for (int tileZ = 0; tileZ < geometry.tilesPerSide(); tileZ++) {
            for (int tileX = 0; tileX < geometry.tilesPerSide(); tileX++) {
                Identifier texture = atlas.texture(tileX, tileZ);
                if (texture == null) continue;

                graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                        tileX * geometry.tilePixels(), tileZ * geometry.tilePixels(),
                        0.0f, 0.0f,
                        geometry.tilePixels(), geometry.tilePixels(),
                        geometry.tilePixels(), geometry.tilePixels(),
                        alpha | 0xFFFFFF);
            }
        }
        pose.popMatrix();

        grid(graphics, client, geometry, settings, mapX, mapY, scale, left, top, side);

        sketch(graphics, mapX, mapY, scale);
        waypoints(graphics, client, geometry, settings, mapX, mapY, scale, alpha);

        boolean labels = zoom >= settings.labelZoom();
        for (ServerMessage.Marker marker : atlas.markers()) {
            marker(graphics, client, geometry, settings, marker, mapX, mapY, scale, alpha, labels);
        }
        player(graphics, client, geometry, mapX, mapY, scale, alpha);
        graphics.disableScissor();

        coordinates(graphics, client, geometry, settings, mapX, mapY, scale, left, top, side);
        tools(graphics, left, top);
        if (MapInput.cursorActive()) {
            act(client, geometry, settings, mapX, mapY, scale, left, top, side);
            cursor(graphics, client);
        }
    }

    private static void act(Minecraft client, AtlasGeometry geometry, MapSettings settings,
                            float mapX, float mapY, float scale, int left, int top, int side) {
        double px = MapInput.pointerX();
        double py = MapInput.pointerY();

        if (MapInput.pressed() && overTools(px, py, left, top)) {
            pickTool(px, left, top);
            return;
        }
        if (px < left || px > left + side || py < top || py > top + side) return;

        double atlasX = (px - mapX) / scale;
        double atlasY = (py - mapY) / scale;

        switch (MapInput.tool()) {
            case PENCIL -> {
                if (MapInput.pressed()) MapSketch.start();
                if (MapInput.holding()) MapSketch.extend(atlasX, atlasY);
                if (MapInput.letGo()) MapSketch.finish();
            }
            case MARKER -> {
                if (!MapInput.pressed()) return;

                int hit = waypointAt(geometry, settings, px, py, mapX, mapY, scale);
                if (hit >= 0) {
                    client.setScreen(WaypointScreen.of(hit));
                    return;
                }
                double blockX = geometry.originX() + atlasX * geometry.blocksPerPixel();
                double blockZ = geometry.originZ() + atlasY * geometry.blocksPerPixel();
                Waypoints.add(new Waypoint(Math.round(blockX) + 0.5, client.player.getY(),
                        Math.round(blockZ) + 0.5, Waypoints.defaultName(), Waypoints.PALETTE[0]));
            }
            case PAN -> {
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

    private static void coordinates(GuiGraphics graphics, Minecraft client, AtlasGeometry geometry,
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
        graphics.fill(centre - width / 2 - 4, lineTop - 2, centre + width / 2 + 4, lineTop + 11, 0x90000000);
        graphics.drawCenteredString(client.font, text, centre, lineTop, 0xFFFFFFFF);
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
            case MARKER -> "pin";
            case PENCIL -> "pencil";
        };
    }

    private static void tools(GuiGraphics graphics, int left, int top) {
        if (!MapInput.cursorActive()) return;

        MapTool[] tools = MapTool.values();
        for (int i = 0; i < tools.length; i++) {
            int x = left + TOOL_GAP + i * (TOOL_SIZE + 2);
            int y = top + TOOL_GAP;
            boolean active = MapInput.tool() == tools[i];

            graphics.fill(x, y, x + TOOL_SIZE, y + TOOL_SIZE, active ? 0xE0303030 : 0xB0101010);
            graphics.fill(x, y, x + TOOL_SIZE, y + 1, active ? TOOL_ON : TOOL_OFF);
            glyph(graphics, icon(tools[i]), x, y, active ? 0xFFFFFFFF : 0xB0FFFFFF);
        }

        int x = left + TOOL_GAP + tools.length * (TOOL_SIZE + 2);
        int y = top + TOOL_GAP;
        graphics.fill(x, y, x + TOOL_SIZE, y + TOOL_SIZE, 0xB0101010);
        glyph(graphics, "trash", x, y, MapSketch.isEmpty() ? 0x70FFFFFF : 0xFFFF6B6B);
    }

    private static void glyph(GuiGraphics graphics, String icon, int x, int y, int tint) {
        int inset = 2;
        int size = TOOL_SIZE - inset * 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED,
                Identifier.fromNamespaceAndPath("alphamap", "textures/tool/" + icon + ".png"),
                x + inset, y + inset, 0.0f, 0.0f, size, size, size, size, tint);
    }

    private static void sketch(GuiGraphics graphics, float mapX, float mapY, float scale) {
        for (var stroke : MapSketch.strokes()) {
            for (int i = 1; i < stroke.size(); i++) {
                double[] from = stroke.get(i - 1);
                double[] to = stroke.get(i);
                line(graphics,
                        mapX + (float) from[0] * scale, mapY + (float) from[1] * scale,
                        mapX + (float) to[0] * scale, mapY + (float) to[1] * scale);
            }
        }
    }

    private static void line(GuiGraphics graphics, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        int steps = (int) Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));
        for (int i = 0; i <= steps; i++) {
            int x = Math.round(x1 + dx * i / steps);
            int y = Math.round(y1 + dy * i / steps);
            graphics.fill(x - 1, y - 1, x + 2, y + 2, SKETCH);
        }
    }

    private static void waypoints(GuiGraphics graphics, Minecraft client, AtlasGeometry geometry,
                                  MapSettings settings, float mapX, float mapY, float scale, int alpha) {
        int half = Math.max(2, settings.markerPixels() / 4);
        for (Waypoint waypoint : Waypoints.all()) {
            int x = (int) (mapX + geometry.pixelX(waypoint.x()) * scale);
            int y = (int) (mapY + geometry.pixelZ(waypoint.z()) * scale);
            int colour = alpha | waypoint.colour();

            graphics.fill(x - half, y - half, x + half, y + half, alpha | OUTLINE);
            graphics.fill(x - half + 1, y - half + 1, x + half - 1, y + half - 1, colour);

            if (!waypoint.name().isBlank()) {
                graphics.drawCenteredString(client.font, waypoint.name(), x, y + half + 2, colour);
            }
        }
    }

    private static void grid(GuiGraphics graphics, Minecraft client, AtlasGeometry geometry,
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
            graphics.fill(x, top, x + 1, top + side, GRID | alpha);
            graphics.fill(left, y, left + side, y + 1, GRID | alpha);
        }

        if (cellPixels < client.font.width("A1") + 6) return;

        for (int column = 0; column < cells; column++) {
            int x = (int) (mapX + geometry.pixelX(first + column * step) * scale);
            if (x + cellPixels < left || x > left + side) continue;

            for (int row = 0; row < cells; row++) {
                int y = (int) (mapY + geometry.pixelZ(first + row * step) * scale);
                if (y + cellPixels < top || y > top + side) continue;

                graphics.drawString(client.font, column(column) + (row + 1),
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

    private static void cursor(GuiGraphics graphics, Minecraft client) {
        int x = (int) MapInput.cursorX(client);
        int y = (int) MapInput.cursorY(client);

        graphics.fill(x - 6, y - 1, x + 7, y + 2, OUTLINE | OPAQUE);
        graphics.fill(x - 1, y - 6, x + 2, y + 7, OUTLINE | OPAQUE);
        graphics.fill(x - 5, y, x + 6, y + 1, CURSOR);
        graphics.fill(x, y - 5, x + 1, y + 6, CURSOR);
    }

    private static void marker(GuiGraphics graphics, Minecraft client, AtlasGeometry geometry,
                               MapSettings settings, ServerMessage.Marker marker,
                               float mapX, float mapY, float scale, int alpha, boolean label) {
        int x = (int) (mapX + geometry.pixelX(marker.x()) * scale);
        int y = (int) (mapY + geometry.pixelZ(marker.z()) * scale);

        Identifier icon = MarkerIcons.of(marker.icon());
        if (icon == null) {
            diamond(graphics, x, y, alpha);
            return;
        }

        int size = settings.markerPixels();
        int half = size / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon,
                x - half, y - half, 0.0f, 0.0f,
                size, size, size, size,
                alpha | 0xFFFFFF);

        boolean named = !marker.label().isEmpty();
        if (!named && !label) return;

        Component text = named
                ? Component.literal(marker.label())
                : Component.translatable("alphamap.marker." + marker.icon());
        graphics.drawCenteredString(client.font, text, x, y + half + 2,
                alpha | (named ? NAMED_LABEL : LABEL));
    }

    private static void diamond(GuiGraphics graphics, int x, int y, int alpha) {
        for (int row = -3; row <= 3; row++) {
            int width = 3 - Math.abs(row);
            graphics.fill(x - width - 1, y + row, x + width + 2, y + row + 1, OUTLINE | alpha);
        }
        for (int row = -2; row <= 2; row++) {
            int width = 2 - Math.abs(row);
            graphics.fill(x - width, y + row, x + width + 1, y + row + 1, UNKNOWN_MARKER | alpha);
        }
    }

    private static void player(GuiGraphics graphics, Minecraft client, AtlasGeometry geometry,
                               float mapX, float mapY, float scale, int alpha) {
        int x = (int) (mapX + geometry.pixelX(client.player.getX()) * scale);
        int y = (int) (mapY + geometry.pixelZ(client.player.getZ()) * scale);

        graphics.fill(x - 4, y - 1, x + 5, y + 2, OUTLINE | alpha);
        graphics.fill(x - 1, y - 4, x + 2, y + 5, OUTLINE | alpha);
        graphics.fill(x - 3, y, x + 4, y + 1, PLAYER | alpha);
        graphics.fill(x, y - 3, x + 1, y + 4, PLAYER | alpha);
    }

    private static void message(GuiGraphics graphics, Minecraft client, @Nullable Component text) {
        if (text == null) return;
        graphics.drawCenteredString(client.font, text,
                graphics.guiWidth() / 2, graphics.guiHeight() / 2, 0xFFFFFFFF);
    }
}

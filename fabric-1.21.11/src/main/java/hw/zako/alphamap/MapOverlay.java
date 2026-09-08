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

    private static final int MARKER_PIXELS = 16;

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
        if (!AlphaMapClient.OPEN_MAP.isDown()) return;

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
        MapInput.viewport(left, top, side);
        MapInput.drag(client);

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

        boolean labels = zoom >= settings.labelZoom();
        for (ServerMessage.Marker marker : atlas.markers()) {
            marker(graphics, client, geometry, marker, mapX, mapY, scale, alpha, labels);
        }
        player(graphics, client, geometry, mapX, mapY, scale, alpha);
        graphics.disableScissor();

        if (MapInput.cursorActive()) cursor(graphics, client);
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
                               ServerMessage.Marker marker, float mapX, float mapY, float scale,
                               int alpha, boolean label) {
        int x = (int) (mapX + geometry.pixelX(marker.x()) * scale);
        int y = (int) (mapY + geometry.pixelZ(marker.z()) * scale);

        Identifier icon = MarkerIcons.of(marker.icon());
        if (icon == null) {
            diamond(graphics, x, y, alpha);
            return;
        }

        int half = MARKER_PIXELS / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon,
                x - half, y - half, 0.0f, 0.0f,
                MARKER_PIXELS, MARKER_PIXELS, MARKER_PIXELS, MARKER_PIXELS,
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

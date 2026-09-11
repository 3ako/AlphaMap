package hw.zako.alphamap;

import hw.zako.alphamap.protocol.AtlasGeometry;
import hw.zako.alphamap.protocol.ServerMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class MinimapOverlay {

    private static final int ARROW = 0xFFFFFFFF;
    private static final int ARROW_OUTLINE = 0xFF000000;
    private static final int DOT_OUTLINE = 0xC0000000;

    private static final int PLAYER_DOT = 0xFF6BE8FF;
    private static final int HOSTILE_DOT = 0xFFFF4040;
    private static final int PASSIVE_DOT = 0xFF7BE07B;

    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_BACKING = 0x90000000;

    private static final int FRAME = 0xB0FFFFFF;

    private static final int EDGE_GAP = 3;
    private static final int DOT = 2;

    AtlasClient atlas;
    MapSettings settings;

    MinimapImage image = new MinimapImage();

    public int left(int width) {
        int size = settings.minimapSize();
        return (int) Math.round(settings.minimapX() * Math.max(0, width - size));
    }

    public int top(int height) {
        int size = settings.minimapSize();
        return (int) Math.round(settings.minimapY() * Math.max(0, height - size));
    }

    public void release() {
        image.release();
    }

    public void draw(Canvas canvas, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (!settings.minimap()
                || Vanilla.hudHidden(client)
                || Vanilla.screen(client) != null
                || AlphaMapClient.mapOpen()) return;

        paint(canvas, client, partialTick);
    }

    public void preview(Canvas canvas, float partialTick) {
        if (atlas.ready()) {
            paint(canvas, Minecraft.getInstance(), partialTick);
            return;
        }

        int size = settings.minimapSize();
        int left = left(canvas.width());
        int top = top(canvas.height());
        canvas.fill(left, top, left + size, top + 1, FRAME);
        canvas.fill(left, top + size - 1, left + size, top + size, FRAME);
        canvas.fill(left, top, left + 1, top + size, FRAME);
        canvas.fill(left + size - 1, top, left + size, top + size, FRAME);
    }

    private void paint(Canvas canvas, Minecraft client, float partialTick) {
        if (client.player == null || client.level == null) return;

        AtlasGeometry geometry = atlas.geometry();
        if (geometry == null) return;

        int size = settings.minimapSize();
        int left = left(canvas.width());
        int top = top(canvas.height());

        float facing = (float) Math.toRadians(client.player.getViewYRot(partialTick)) + (float) Math.PI;
        float turn = settings.minimapNorth() ? 0.0f : facing;

        Identifier texture = image.build(atlas, geometry,
                client.player.getX(), client.player.getZ(),
                size, settings.minimapBlocks(), turn, settings.minimapShape());
        if (texture == null) return;

        canvas.blit(texture, left, top, size, settings.alpha() | 0xFFFFFF);

        float centreX = left + size / 2.0f;
        float centreY = top + size / 2.0f;
        if (settings.compass()) {
            Compass.around(canvas, client.font, centreX, centreY, size / 2.0 - EDGE_GAP - 4, turn);
        }
        marks(canvas, client, centreX, centreY, size, turn);
        arrow(canvas, centreX, centreY, settings.minimapNorth() ? facing : 0.0f);

        if (settings.minimapCoordinates()) coordinates(canvas, client, left, top, size);
    }

    private void marks(Canvas canvas, Minecraft client, float centreX, float centreY,
                       int size, float turn) {
        double scale = size / (double) settings.minimapBlocks();
        double cos = Math.cos(-turn);
        double sin = Math.sin(-turn);
        double limit = size / 2.0 - EDGE_GAP;
        boolean circle = settings.minimapShape() == MinimapShape.CIRCLE;

        double grow = settings.minimapMarkerScale();
        int dot = Math.max(2, (int) Math.round(DOT * grow));
        int pin = Math.max(3, (int) Math.round((DOT + 1) * grow));

        if (settings.minimapMarkers()) {
            int icon = settings.minimapMarkerPixels();
            for (ServerMessage.Marker marker : atlas.markers()) {
                if (!settings.minimapMarkerShown(marker.icon())) continue;

                double dx = (marker.x() + 0.5 - client.player.getX()) * scale;
                double dz = (marker.z() + 0.5 - client.player.getZ()) * scale;
                double across = dx * cos - dz * sin;
                double down = dx * sin + dz * cos;

                double hug = hug(across, down, limit, circle);
                int x = (int) Math.round(centreX + across * hug);
                int y = (int) Math.round(centreY + down * hug);

                canvas.blit(MarkerIcons.of(marker.icon()),
                        x - icon / 2, y - icon / 2, icon, 0xFFFFFFFF);
            }
        }

        BlockPos death = DeathPoint.of(client, settings);
        if (death != null) {
            double dx = (death.getX() + 0.5 - client.player.getX()) * scale;
            double dz = (death.getZ() + 0.5 - client.player.getZ()) * scale;
            double across = dx * cos - dz * sin;
            double down = dx * sin + dz * cos;
            double hug = hug(across, down, limit, circle);

            DeathPoint.draw(canvas, (int) Math.round(centreX + across * hug),
                    (int) Math.round(centreY + down * hug), pin);
        }

        if (settings.minimapWaypoints()) {
            for (Waypoint waypoint : Waypoints.all()) {
                double dx = (waypoint.x() - client.player.getX()) * scale;
                double dz = (waypoint.z() - client.player.getZ()) * scale;
                double across = dx * cos - dz * sin;
                double down = dx * sin + dz * cos;
                double hug = hug(across, down, limit, circle);

                spot(canvas, (int) Math.round(centreX + across * hug),
                        (int) Math.round(centreY + down * hug),
                        0xFF000000 | waypoint.colour(), pin);
            }
        }

        for (MinimapEntities.Dot mark : MinimapEntities.shown()) {
            Entity entity = mark.entity();
            if (entity.isRemoved()) continue;

            double dx = (entity.getX() - client.player.getX()) * scale;
            double dz = (entity.getZ() - client.player.getZ()) * scale;
            double across = dx * cos - dz * sin;
            double down = dx * sin + dz * cos;
            if (hug(across, down, limit, circle) < 1.0) continue;

            spot(canvas, (int) Math.round(centreX + across),
                    (int) Math.round(centreY + down), colour(mark.kind()), dot);
        }
    }

    private static double hug(double across, double down, double limit, boolean circle) {
        double reach = circle
                ? Math.sqrt(across * across + down * down)
                : Math.max(Math.abs(across), Math.abs(down));
        return reach <= limit ? 1.0 : limit / reach;
    }

    private static void spot(Canvas canvas, int x, int y, int colour, int half) {
        canvas.fill(x - half, y - half, x + half, y + half, DOT_OUTLINE);
        canvas.fill(x - half + 1, y - half + 1, x + half - 1, y + half - 1, colour);
    }

    private static int colour(MinimapEntities.Kind kind) {
        return switch (kind) {
            case PLAYER -> PLAYER_DOT;
            case HOSTILE -> HOSTILE_DOT;
            case PASSIVE -> PASSIVE_DOT;
        };
    }

    private static void arrow(Canvas canvas, float centreX, float centreY, float turn) {
        canvas.push(centreX, centreY, 1.0f, turn);
        for (int row = -4; row <= 3; row++) {
            int half = (row + 4) / 2;
            canvas.fill(-half - 1, row, half + 2, row + 1, ARROW_OUTLINE);
        }
        for (int row = -3; row <= 2; row++) {
            int half = (row + 3) / 2;
            canvas.fill(-half, row, half + 1, row + 1, ARROW);
        }
        canvas.pop();
    }

    private void coordinates(Canvas canvas, Minecraft client, int left, int top, int size) {
        String text = "%d, %d, %d".formatted(
                Math.round(client.player.getX()),
                Math.round(client.player.getY()),
                Math.round(client.player.getZ()));

        int centre = left + size / 2;
        boolean below = top + size + 12 <= canvas.height();
        int line = below ? top + size + 2 : top - 11;
        int width = client.font.width(text);
        canvas.fill(centre - width / 2 - 3, line - 1, centre + width / 2 + 3, line + 10, TEXT_BACKING);
        canvas.centered(client.font, text, centre, line, TEXT);
    }
}

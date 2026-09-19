package hw.zako.alphamap;

import hw.zako.alphamap.protocol.AtlasGeometry;
import hw.zako.alphamap.protocol.ServerMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

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
    private static final int OUTSIDE_GAP = 6;
    private static final int DOT = 2;

    private static final long FIT_MILLIS = 250;
    private static final int FIT_MARGIN = 12;
    private static final double FIT_EASE_SECONDS = 0.15;

    AtlasClient atlas;
    MapSettings settings;

    MinimapImage image = new MinimapImage();
    ChunkMap chunks = ChunkMap.INSTANCE;

    @NonFinal
    int blocks = 1;
    @NonFinal
    int fitted;
    @NonFinal
    long fittedAt;
    @NonFinal
    int fittedRevision;
    @NonFinal
    long fittedFeet;
    @NonFinal
    double shown;
    @NonFinal
    long shownAt;

    @NonFinal
    long shownX;
    @NonFinal
    long shownY;
    @NonFinal
    long shownZ;
    @NonFinal
    @Nullable String shownText;

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

        double playerX = Mth.lerp(partialTick, client.player.xo, client.player.getX());
        double playerZ = Mth.lerp(partialTick, client.player.zo, client.player.getZ());

        BlockPos feet = client.player.blockPosition();
        boolean underground = settings.minimapCaves() && chunks.underground(client.level, feet);
        chunks.follow(client.level, underground, feet.getY());
        blocks = view(feet, underground);

        Identifier texture = image.build(atlas, geometry, chunks, playerX, playerZ,
                size, client.getWindow().getGuiScale(), blocks, turn, settings.minimapShape());
        if (texture == null) return;

        canvas.blit(texture, left, top, size, settings.alpha() | 0xFFFFFF);

        float centreX = left + size / 2.0f;
        float centreY = top + size / 2.0f;
        boolean outside = settings.compass() && settings.compassOutside();
        if (settings.compass()) {
            Compass.around(canvas, client.font, centreX, centreY,
                    outside ? size / 2.0 + OUTSIDE_GAP : size / 2.0 - EDGE_GAP - 4,
                    turn, outside && settings.minimapShape() == MinimapShape.SQUARE);
        }
        marks(canvas, client, image.centerX(), image.centerZ(), centreX, centreY, size, image.angle(), partialTick);
        arrow(canvas, centreX, centreY, settings.minimapNorth() ? facing : 0.0f);

        if (settings.minimapCoordinates()) coordinates(canvas, client, left, top, size, outside);
    }

    private int view(BlockPos feet, boolean underground) {
        int full = settings.minimapBlocks();
        long now = System.currentTimeMillis();
        if (!underground) {
            fitted = full;
        } else if (now - fittedAt >= FIT_MILLIS || fittedRevision != chunks.revision() || fittedFeet != feet.asLong()) {
            fittedAt = now;
            fittedRevision = chunks.revision();
            fittedFeet = feet.asLong();
            int reach = chunks.caveReach(feet.getX(), feet.getZ(), full / 2);
            fitted = reach == 0 ? full
                    : Math.clamp(reach * 2 + FIT_MARGIN, MapSettings.MIN_MINIMAP_BLOCKS, full);
        }
        int target = Math.max(MapSettings.MIN_MINIMAP_BLOCKS, fitted / settings.minimapZoom());

        double seconds = shownAt == 0 ? 1.0 : (now - shownAt) / 1000.0;
        shownAt = now;
        shown += (target - shown) * (1 - Math.exp(-seconds / FIT_EASE_SECONDS));
        if (Math.abs(target - shown) < 0.5) shown = target;
        return (int) Math.round(shown);
    }

    private void marks(Canvas canvas, Minecraft client, double playerX, double playerZ,
                       float centreX, float centreY, int size, float turn, float partialTick) {
        double scale = size / (double) blocks;
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

                double dx = (marker.x() + 0.5 - playerX) * scale;
                double dz = (marker.z() + 0.5 - playerZ) * scale;
                double across = dx * cos - dz * sin;
                double down = dx * sin + dz * cos;

                double hug = hug(across, down, limit, circle);
                canvas.push((float) (centreX + across * hug), (float) (centreY + down * hug), 1.0f);
                canvas.blit(MarkerIcons.of(marker.icon()), -icon / 2, -icon / 2, icon, 0xFFFFFFFF);
                canvas.pop();
            }
        }

        BlockPos death = DeathPoint.of(client, settings);
        if (death != null) {
            double dx = (death.getX() + 0.5 - playerX) * scale;
            double dz = (death.getZ() + 0.5 - playerZ) * scale;
            double across = dx * cos - dz * sin;
            double down = dx * sin + dz * cos;
            double hug = hug(across, down, limit, circle);

            canvas.push((float) (centreX + across * hug), (float) (centreY + down * hug), 1.0f);
            DeathPoint.draw(canvas, 0, 0, pin);
            canvas.pop();
        }

        if (settings.minimapWaypoints()) {
            for (Waypoint waypoint : Waypoints.all()) {
                double dx = (waypoint.x() - playerX) * scale;
                double dz = (waypoint.z() - playerZ) * scale;
                double across = dx * cos - dz * sin;
                double down = dx * sin + dz * cos;
                double hug = hug(across, down, limit, circle);

                canvas.push((float) (centreX + across * hug), (float) (centreY + down * hug), 1.0f);
                spot(canvas, 0, 0, 0xFF000000 | waypoint.colour(), pin);
                canvas.pop();
            }
        }

        for (MinimapEntities.Dot mark : MinimapEntities.shown()) {
            Entity entity = mark.entity();
            if (entity.isRemoved()) continue;

            double dx = (Mth.lerp(partialTick, entity.xo, entity.getX()) - playerX) * scale;
            double dz = (Mth.lerp(partialTick, entity.zo, entity.getZ()) - playerZ) * scale;
            double across = dx * cos - dz * sin;
            double down = dx * sin + dz * cos;
            if (hug(across, down, limit, circle) < 1.0) continue;

            canvas.push((float) (centreX + across), (float) (centreY + down), 1.0f);
            MobHeads.Head head = mark.head();
            if (head != null && mark.texture() != null) {
                int face = Math.max(6, dot * 3);
                canvas.blitRegion(mark.texture(), -face / 2, -face / 2, face, face,
                        head.u(), head.v(), head.width(), head.height(),
                        head.sheetWidth(), head.sheetHeight(), 0xFFFFFFFF);
            } else {
                spot(canvas, 0, 0, colour(mark.kind()), dot);
            }
            canvas.pop();
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

    private void coordinates(Canvas canvas, Minecraft client, int left, int top, int size,
                             boolean compassOutside) {
        long x = Math.round(client.player.getX());
        long y = Math.round(client.player.getY());
        long z = Math.round(client.player.getZ());
        if (x != shownX || y != shownY || z != shownZ || shownText == null) {
            shownX = x;
            shownY = y;
            shownZ = z;
            shownText = x + ", " + y + ", " + z;
        }
        String text = shownText;

        int step = compassOutside ? 11 : 0;
        int centre = left + size / 2;
        boolean below = top + size + 12 + step <= canvas.height();
        int line = below ? top + size + 2 + step : top - 11 - step;
        int width = client.font.width(text);
        canvas.fill(centre - width / 2 - 3, line - 1, centre + width / 2 + 3, line + 10, TEXT_BACKING);
        canvas.centered(client.font, text, centre, line, TEXT);
    }
}

package hw.zako.alphamap;

import hw.zako.alphamap.protocol.ServerMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class WaypointOverlay {

    private static final String BED = "bed";
    private static final int BED_COLOUR = 0xFF6B6B;
    private static final int MARKER_COLOUR = 0xFFFFFF;
    private static final int EDGE = 10;

    AtlasClient atlas;
    MapSettings settings;

    public void draw(Canvas canvas) {
        Minecraft client = Minecraft.getInstance();
        if (!atlas.ready() || client.player == null
                || Vanilla.hudHidden(client) || Vanilla.screen(client) != null) return;

        Camera camera = Vanilla.camera(client);
        if (camera == null) return;

        int size = settings.worldMarkerPixels();
        for (Waypoint waypoint : Waypoints.all()) {
            place(canvas, client, camera,
                    waypoint.x(), waypoint.y(), waypoint.z(),
                    waypoint.name(), waypoint.colour(), null, size);
        }

        for (ServerMessage.Marker marker : atlas.markers()) {
            if (!settings.worldMarkerShown(marker.icon())) continue;

            place(canvas, client, camera,
                    marker.x() + 0.5, client.player.getY(), marker.z() + 0.5,
                    marker.label(), BED.equals(marker.icon()) ? BED_COLOUR : MARKER_COLOUR,
                    marker.icon(), size);
        }
    }

    private static void place(Canvas canvas, Minecraft client, Camera camera,
                              double wx, double wy, double wz, String name, int colour,
                              @Nullable String icon, int size) {
        Vec3 point = new Vec3(wx, wy, wz);
        Vec3 screen = client.gameRenderer.projectPointToScreen(point);

        Vec3 offset = point.subtract(camera.position());
        Vector3fc forward = camera.forwardVector();
        if (offset.x * forward.x() + offset.y * forward.y() + offset.z * forward.z() <= 0) return;

        int width = canvas.width();
        int height = canvas.height();

        int down = (int) Math.round((0.5 - screen.y * 0.5) * height);
        if (down > height - EDGE) return;

        int x = Math.clamp(Math.round((screen.x * 0.5 + 0.5) * width), EDGE, width - EDGE);
        int y = Math.max(down, EDGE);

        int distance = (int) Math.round(Math.sqrt(
                Math.pow(wx - client.player.getX(), 2)
                        + Math.pow(wy - client.player.getY(), 2)
                        + Math.pow(wz - client.player.getZ(), 2)));

        int tint = 0xFF000000 | colour;
        Identifier texture = icon != null ? MarkerIcons.of(icon) : null;
        int half = texture != null ? size / 2 : Math.max(2, size / 3);
        int inset = Math.max(1, half / 3);

        if (texture != null) {
            canvas.blit(texture, x - half, y - half, size, 0xFFFFFFFF);
        } else {
            canvas.fill(x - half - 1, y - half - 1, x + half + 1, y + half + 1, 0xC0000000);
            canvas.fill(x - half, y - half, x + half, y + half, tint);
            canvas.fill(x - half + inset, y - half + inset,
                    x + half - inset, y + half - inset, 0xFF101010);
        }

        if (name != null && !name.isBlank()) {
            canvas.centered(client.font, name, x, y - half - 12, tint);
        }
        canvas.centered(client.font,
                Component.translatable("alphamap.waypoint.distance", distance),
                x, y + half + 3, 0xFFBBBBBB);
    }
}

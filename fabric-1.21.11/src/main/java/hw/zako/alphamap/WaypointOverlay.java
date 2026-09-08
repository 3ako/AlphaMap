package hw.zako.alphamap;

import hw.zako.alphamap.mixin.GameRendererAccessor;
import hw.zako.alphamap.protocol.ServerMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class WaypointOverlay implements HudElement {

    private static final String BED = "bed";
    private static final int BED_COLOUR = 0xFF6B6B;
    private static final int EDGE = 10;

    AtlasClient atlas;
    MapSettings settings;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui || client.screen != null) return;

        Camera camera = client.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) return;

        Vec3 eye = camera.position();
        Matrix4f view = new Matrix4f().rotation(new Quaternionf(camera.rotation()).conjugate());
        float fov = ((GameRendererAccessor) client.gameRenderer)
                .alphamap$getFov(camera, delta.getGameTimeDeltaPartialTick(true), true);
        Matrix4f projection = client.gameRenderer.getProjectionMatrix(fov);

        int size = settings.worldMarkerPixels();
        for (Waypoint waypoint : Waypoints.all()) {
            place(graphics, client, view, projection, eye,
                    waypoint.x(), waypoint.y(), waypoint.z(),
                    waypoint.name(), waypoint.colour(), null, size);
        }

        if (!settings.worldBeds()) return;
        for (ServerMessage.Marker marker : atlas.markers()) {
            if (!BED.equals(marker.icon())) continue;

            place(graphics, client, view, projection, eye,
                    marker.x() + 0.5, client.player.getY(), marker.z() + 0.5,
                    marker.label(), BED_COLOUR, BED, size);
        }
    }

    private static void place(GuiGraphics graphics, Minecraft client, Matrix4f view, Matrix4f projection,
                              Vec3 eye, double wx, double wy, double wz, String name, int colour,
                              @Nullable String icon, int size) {
        Vector4f clip = new Vector4f(
                (float) (wx - eye.x), (float) (wy - eye.y), (float) (wz - eye.z), 1.0f);
        view.transform(clip);
        projection.transform(clip);

        boolean behind = clip.w <= 0.0f;
        float ndcX = clip.x / Math.abs(clip.w);
        float ndcY = clip.y / Math.abs(clip.w);
        if (behind) ndcX = -ndcX;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        int x = Math.clamp(Math.round((ndcX * 0.5f + 0.5f) * width), EDGE, width - EDGE);
        int y = behind
                ? height - EDGE
                : Math.clamp(Math.round((0.5f - ndcY * 0.5f) * height), EDGE, height - EDGE);

        int distance = (int) Math.round(Math.sqrt(
                Math.pow(wx - client.player.getX(), 2)
                        + Math.pow(wy - client.player.getY(), 2)
                        + Math.pow(wz - client.player.getZ(), 2)));

        int tint = 0xFF000000 | colour;
        Identifier texture = icon != null ? MarkerIcons.of(icon) : null;
        int half = texture != null ? size / 2 : Math.max(2, size / 3);
        int inset = Math.max(1, half / 3);

        if (texture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                    x - half, y - half, 0.0f, 0.0f, size, size, size, size, 0xFFFFFFFF);
        } else {
            graphics.fill(x - half - 1, y - half - 1, x + half + 1, y + half + 1, 0xC0000000);
            graphics.fill(x - half, y - half, x + half, y + half, tint);
            graphics.fill(x - half + inset, y - half + inset,
                    x + half - inset, y + half - inset, 0xFF101010);
        }

        if (name != null && !name.isBlank()) {
            graphics.drawCenteredString(client.font, name, x, y - half - 12, tint);
        }
        graphics.drawCenteredString(client.font,
                Component.translatable("alphamap.waypoint.distance", distance),
                x, y + half + 3, 0xFFBBBBBB);
    }
}

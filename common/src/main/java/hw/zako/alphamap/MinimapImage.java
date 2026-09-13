package hw.zako.alphamap;

import com.mojang.blaze3d.platform.NativeImage;
import hw.zako.alphamap.protocol.AtlasGeometry;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

@FieldDefaults(level = AccessLevel.PRIVATE)
final class MinimapImage {

    private static final Identifier NAME = Identifier.fromNamespaceAndPath("alphamap", "minimap");

    private static final int VOID = 0x00000000;
    private static final int EDGE = 0xB0000000;
    private static final int OUTSIDE = 0x40000000;

    private static final double MOVED_PIXELS = 0.1;
    private static final float TURNED_RADIANS = 0.002f;
    private static final long REBUILD_INTERVAL_MILLIS = 20;

    @Nullable
    NativeImage image;
    @Nullable
    DynamicTexture texture;

    int size;

    int revision = -1;
    double centerX;
    double centerZ;
    double step;
    float angle;
    @Nullable
    MinimapShape shape;
    long builtAt;

    @Nullable
    Identifier build(AtlasClient atlas, AtlasGeometry geometry, double blockX, double blockZ,
                     int size, int blocks, float angle, MinimapShape shape) {
        double step = blocks / (double) geometry.blocksPerPixel() / size;
        double centerX = geometry.pixelX(blockX);
        double centerZ = geometry.pixelZ(blockZ);

        if (fresh(atlas.revision(), size, centerX, centerZ, step, angle, shape)) return NAME;

        hold(size);
        paint(atlas, geometry, centerX, centerZ, step, angle, shape);
        texture.upload();

        revision = atlas.revision();
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.step = step;
        this.angle = angle;
        this.shape = shape;
        builtAt = System.currentTimeMillis();
        return NAME;
    }

    void release() {
        if (texture == null) return;

        Minecraft.getInstance().getTextureManager().release(NAME);
        texture.close();
        texture = null;
        image = null;
        size = 0;
        revision = -1;
    }

    private boolean fresh(int revision, int size, double centerX, double centerZ,
                          double step, float angle, MinimapShape shape) {
        if (image == null || this.size != size || this.shape != shape || this.step != step) return false;

        if (System.currentTimeMillis() - builtAt < REBUILD_INTERVAL_MILLIS) return true;

        return this.revision == revision
                && Math.abs(this.angle - angle) < TURNED_RADIANS
                && Math.abs(this.centerX - centerX) < step * MOVED_PIXELS
                && Math.abs(this.centerZ - centerZ) < step * MOVED_PIXELS;
    }

    private void hold(int size) {
        if (image != null && this.size == size) return;

        release();
        image = new NativeImage(size, size, true);
        texture = new DynamicTexture(() -> "alphamap minimap", image);
        Minecraft.getInstance().getTextureManager().register(NAME, texture);
        this.size = size;
    }

    private void paint(AtlasClient atlas, AtlasGeometry geometry, double centerX, double centerZ,
                       double step, float angle, MinimapShape shape) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double alongX = cos * step;
        double alongZ = sin * step;
        double downX = -sin * step;
        double downZ = cos * step;

        double half = size / 2.0;
        double outer = (half - 0.5) * (half - 0.5);
        double inner = (half - 1.5) * (half - 1.5);

        int tilePixels = geometry.tilePixels();
        int sidePixels = geometry.sidePixels();

        NativeImage tile = null;
        int tileLeft = 0;
        int tileTop = 0;
        int tileRight = -1;
        int tileBottom = -1;

        for (int py = 0; py < size; py++) {
            double down = py + 0.5 - half;
            int from;
            int to;

            if (shape == MinimapShape.CIRCLE) {
                double rim = outer - down * down;
                if (rim <= 0) {
                    image.fillRect(0, py, size, 1, VOID);
                    continue;
                }

                double rimWidth = Math.sqrt(rim);
                int rimFrom = Math.max(0, (int) Math.ceil(half - rimWidth - 0.5));
                int rimTo = Math.min(size - 1, (int) (half + rimWidth - 0.5));

                double core = inner - down * down;
                if (core <= 0) {
                    run(0, rimFrom, py, VOID);
                    run(rimFrom, rimTo + 1, py, EDGE);
                    run(rimTo + 1, size, py, VOID);
                    continue;
                }

                double coreWidth = Math.sqrt(core);
                from = Math.max(rimFrom, (int) Math.ceil(half - coreWidth - 0.5));
                to = Math.min(rimTo, (int) (half + coreWidth - 0.5));

                run(0, rimFrom, py, VOID);
                run(rimFrom, from, py, EDGE);
                run(to + 1, rimTo + 1, py, EDGE);
                run(rimTo + 1, size, py, VOID);
            } else {
                if (py == 0 || py == size - 1) {
                    image.fillRect(0, py, size, 1, EDGE);
                    continue;
                }

                from = 1;
                to = size - 2;
                run(0, 1, py, EDGE);
                run(size - 1, size, py, EDGE);
            }

            double rowX = centerX + (from + 0.5 - half) * alongX + down * downX;
            double rowZ = centerZ + (from + 0.5 - half) * alongZ + down * downZ;

            for (int px = from; px <= to; px++, rowX += alongX, rowZ += alongZ) {
                int atlasX = (int) Math.floor(rowX);
                int atlasZ = (int) Math.floor(rowZ);
                if (atlasX < 0 || atlasZ < 0 || atlasX >= sidePixels || atlasZ >= sidePixels) {
                    image.setPixel(px, py, OUTSIDE);
                    continue;
                }

                if (atlasX < tileLeft || atlasX > tileRight || atlasZ < tileTop || atlasZ > tileBottom) {
                    int tileX = atlasX / tilePixels;
                    int tileZ = atlasZ / tilePixels;
                    tile = atlas.pixels(tileX, tileZ);
                    if (tile != null && (tile.getWidth() < tilePixels || tile.getHeight() < tilePixels)) {
                        tile = null;
                    }
                    tileLeft = tileX * tilePixels;
                    tileTop = tileZ * tilePixels;
                    tileRight = tileLeft + tilePixels - 1;
                    tileBottom = tileTop + tilePixels - 1;
                }

                image.setPixel(px, py, tile == null
                        ? OUTSIDE
                        : tile.getPixel(atlasX - tileLeft, atlasZ - tileTop));
            }
        }
    }

    private void run(int from, int to, int row, int colour) {
        if (to > from) image.fillRect(from, row, to - from, 1, colour);
    }
}

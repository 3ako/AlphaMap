package hw.zako.alphamap;

import com.mojang.blaze3d.platform.NativeImage;
import hw.zako.alphamap.protocol.AtlasGeometry;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Accessors(fluent = true)
final class MinimapImage {

    private static final Identifier NAME = Identifier.fromNamespaceAndPath("alphamap", "minimap");

    private static final int VOID = 0x00000000;
    private static final int EDGE = 0xB0000000;
    private static final int OUTSIDE = 0x40000000;

    private static final double MOVED_PIXELS = 1.0;
    private static final double DETAIL_PIXELS = 6.0;
    private static final int TEXELS = 16;
    private static final float TURNED_RADIANS = 0.005f;
    private static final long REBUILD_INTERVAL_MILLIS = 20;

    @Nullable
    NativeImage image;
    @Nullable
    DynamicTexture texture;

    int size;
    int edge = 1;

    @Nullable
    NativeImage tile;
    int tileLeft;
    int tileTop;
    int tileRight = -1;
    int tileBottom = -1;

    int revision = -1;
    int chunkRevision = -1;
    @Getter
    double centerX;
    @Getter
    double centerZ;
    double step;
    @Getter
    float angle;
    @Nullable
    MinimapShape shape;
    long builtAt;

    @Nullable
    Identifier build(AtlasClient atlas, AtlasGeometry geometry, ChunkMap chunks, double centerX, double centerZ,
                     int guiSize, int scale, int blocks, float angle, MinimapShape shape) {
        int size = guiSize * scale;
        double step = blocks / (double) size;
        edge = scale;

        if (fresh(atlas.revision(), chunks.revision(), size, centerX, centerZ, step, angle, shape)) return NAME;

        hold(size);
        paint(atlas, geometry, chunks, centerX, centerZ, step, angle, shape);
        texture.upload();

        revision = atlas.revision();
        chunkRevision = chunks.revision();
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
        chunkRevision = -1;
    }

    private boolean fresh(int revision, int chunkRevision, int size, double centerX, double centerZ,
                          double step, float angle, MinimapShape shape) {
        if (image == null || this.size != size || this.shape != shape || this.step != step) return false;

        if (System.currentTimeMillis() - builtAt < REBUILD_INTERVAL_MILLIS) return true;

        return this.revision == revision
                && this.chunkRevision == chunkRevision
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

    private void paint(AtlasClient atlas, AtlasGeometry geometry, ChunkMap chunks, double centerX, double centerZ,
                       double step, float angle, MinimapShape shape) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double alongX = cos * step;
        double alongZ = sin * step;
        double downX = -sin * step;
        double downZ = cos * step;

        double half = size / 2.0;
        double outer = (half - 0.5) * (half - 0.5);
        double inner = (half - 0.5 - edge) * (half - 0.5 - edge);

        tile = null;
        tileRight = -1;
        tileBottom = -1;

        boolean detail = 1 / step >= DETAIL_PIXELS;
        double lod = Math.log(TEXELS * step) / Math.log(2);
        int mip = Math.clamp((int) Math.floor(lod), 0, ChunkMap.MIP_LEVELS - 2);
        int blend = (int) Math.round(Math.clamp(lod - mip, 0.0, 1.0) * 256);

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
                if (py < edge || py >= size - edge) {
                    image.fillRect(0, py, size, 1, EDGE);
                    continue;
                }

                from = edge;
                to = size - 1 - edge;
                run(0, edge, py, EDGE);
                run(size - edge, size, py, EDGE);
            }

            double rowX = centerX + (from + 0.5 - half) * alongX + down * downX;
            double rowZ = centerZ + (from + 0.5 - half) * alongZ + down * downZ;

            int lastX = Integer.MIN_VALUE;
            int lastZ = Integer.MIN_VALUE;
            int near = ChunkMap.MISSING;
            int[] fine = null;
            int[] coarse = null;
            int modulate = 0;
            for (int px = from; px <= to; px++, rowX += alongX, rowZ += alongZ) {
                int blockX = (int) Math.floor(rowX);
                int blockZ = (int) Math.floor(rowZ);
                if (blockX != lastX || blockZ != lastZ) {
                    near = chunks.colour(blockX, blockZ);
                    fine = detail ? chunks.texels(mip) : null;
                    coarse = fine != null && blend > 0 ? chunks.texels(mip + 1) : null;
                    modulate = chunks.modulate();
                    lastX = blockX;
                    lastZ = blockZ;
                }
                if (fine != null) {
                    double u = rowX - blockX;
                    double v = rowZ - blockZ;
                    int texel = sample(fine, TEXELS >> mip, u, v);
                    if (coarse != null) texel = lerp(texel, sample(coarse, TEXELS >> (mip + 1), u, v), blend);
                    image.setPixel(px, py, tinted(texel, modulate));
                    continue;
                }
                if (near != ChunkMap.MISSING) {
                    image.setPixel(px, py, near);
                    continue;
                }

                image.setPixel(px, py, smooth(atlas, geometry,
                        geometry.pixelX(rowX) - 0.5, geometry.pixelZ(rowZ) - 0.5));
            }
        }
    }

    private int smooth(AtlasClient atlas, AtlasGeometry geometry, double atlasX, double atlasZ) {
        int x = (int) Math.floor(atlasX);
        int z = (int) Math.floor(atlasZ);
        double tx = atlasX - x;
        double tz = atlasZ - z;
        return mix(mix(pixel(atlas, geometry, x, z), pixel(atlas, geometry, x + 1, z), tx),
                mix(pixel(atlas, geometry, x, z + 1), pixel(atlas, geometry, x + 1, z + 1), tx), tz);
    }

    private int pixel(AtlasClient atlas, AtlasGeometry geometry, int atlasX, int atlasZ) {
        int sidePixels = geometry.sidePixels();
        if (atlasX < 0 || atlasZ < 0 || atlasX >= sidePixels || atlasZ >= sidePixels) return OUTSIDE;

        if (atlasX < tileLeft || atlasX > tileRight || atlasZ < tileTop || atlasZ > tileBottom) {
            int tilePixels = geometry.tilePixels();
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
        return tile == null ? OUTSIDE : tile.getPixel(atlasX - tileLeft, atlasZ - tileTop);
    }

    private static int sample(int[] texels, int side, double u, double v) {
        double x = u * side - 0.5;
        double y = v * side - 0.5;
        int left = (int) Math.floor(x);
        int top = (int) Math.floor(y);
        int tx = (int) ((x - left) * 256);
        int ty = (int) ((y - top) * 256);
        int right = Math.min(left + 1, side - 1);
        int bottom = Math.min(top + 1, side - 1);
        left = Math.max(left, 0);
        top = Math.max(top, 0);
        return lerp(lerp(texels[top * side + left], texels[top * side + right], tx),
                lerp(texels[bottom * side + left], texels[bottom * side + right], tx), ty);
    }

    private static int lerp(int a, int b, int weight) {
        int keep = 256 - weight;
        return ((a & 0xFF00FF) * keep + (b & 0xFF00FF) * weight) >>> 8 & 0xFF00FF
                | ((a & 0x00FF00) * keep + (b & 0x00FF00) * weight) >>> 8 & 0x00FF00;
    }

    private static int tinted(int rgb, int modulate) {
        return 0xFF000000
                | Math.min(255, (rgb >> 16 & 0xFF) * (modulate >> 16 & 0xFF) >> 7) << 16
                | Math.min(255, (rgb >> 8 & 0xFF) * (modulate >> 8 & 0xFF) >> 7) << 8
                | Math.min(255, (rgb & 0xFF) * (modulate & 0xFF) >> 7);
    }

    private static int mix(int a, int b, double t) {
        if (a == b || t <= 0) return a;
        if (t >= 1) return b;
        return channel(a, b, t, 24) | channel(a, b, t, 16) | channel(a, b, t, 8) | channel(a, b, t, 0);
    }

    private static int channel(int a, int b, double t, int shift) {
        int from = a >>> shift & 0xFF;
        int to = b >>> shift & 0xFF;
        return (int) (from + (to - from) * t + 0.5) << shift;
    }

    private void run(int from, int to, int row, int colour) {
        if (to > from) image.fillRect(from, row, to - from, 1, colour);
    }
}

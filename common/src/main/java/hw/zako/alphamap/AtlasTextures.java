package hw.zako.alphamap;

import com.mojang.blaze3d.platform.NativeImage;
import hw.zako.alphamap.protocol.AtlasGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@Accessors(fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
final class AtlasTextures {

    @Getter
    AtlasGeometry geometry;
    byte[][] hashes;
    byte[][] loaded;
    Identifier[] names;
    DynamicTexture[] holders;

    AtlasTextures(AtlasGeometry geometry, byte[][] hashes) {
        this.geometry = geometry;
        this.hashes = hashes;
        this.loaded = new byte[hashes.length][];
        this.names = new Identifier[hashes.length];
        this.holders = new DynamicTexture[hashes.length];
    }

    byte[] hash(int index) {
        return hashes[index];
    }

    int count() {
        return hashes.length;
    }

    boolean current(int index) {
        return loaded[index] != null && Arrays.equals(loaded[index], hashes[index]);
    }

    @Nullable
    Identifier texture(int tileX, int tileZ) {
        return names[geometry.index(tileX, tileZ)];
    }

    // Пиксели тайла уже лежат в оперативке внутри DynamicTexture — миникарта собирается из них,
    // не заводя собственной копии атласа.
    @Nullable
    NativeImage pixels(int tileX, int tileZ) {
        DynamicTexture texture = holders[geometry.index(tileX, tileZ)];
        return texture != null ? texture.getPixels() : null;
    }

    void put(int index, byte[] hash, NativeImage image) {
        Identifier name = Identifier.fromNamespaceAndPath("alphamap", "tile/" + index);
        DynamicTexture texture = new DynamicTexture(() -> "alphamap tile " + index, image);

        DynamicTexture previous = holders[index];
        Minecraft.getInstance().getTextureManager().register(name, texture);
        if (previous != null) previous.close();

        holders[index] = texture;
        names[index] = name;
        loaded[index] = hash;
    }

    void handOver(AtlasTextures next) {
        for (int i = 0; i < holders.length; i++) {
            if (holders[i] == null) continue;

            next.holders[i] = holders[i];
            next.names[i] = names[i];
            next.loaded[i] = loaded[i];

            holders[i] = null;
            names[i] = null;
        }
    }

    void release() {
        for (int i = 0; i < holders.length; i++) {
            drop(i);
        }
    }

    private void drop(int index) {
        if (holders[index] == null) return;

        Minecraft.getInstance().getTextureManager().release(names[index]);
        holders[index].close();
        holders[index] = null;
        names[index] = null;
        loaded[index] = null;
    }
}

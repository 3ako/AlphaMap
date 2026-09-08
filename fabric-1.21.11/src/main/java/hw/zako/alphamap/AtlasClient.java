package hw.zako.alphamap;

import com.mojang.blaze3d.platform.NativeImage;
import hw.zako.alphamap.protocol.AtlasGeometry;
import hw.zako.alphamap.protocol.MapProtocol;
import hw.zako.alphamap.protocol.ServerMessage;
import hw.zako.alphamap.protocol.TileCache;
import hw.zako.alphamap.protocol.TileQueue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Accessors(fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class AtlasClient {

    private static final long HELLO_INTERVAL_MILLIS = 5_000;

    private static final Executor OFF_THREAD =
            task -> Thread.ofVirtual().name("alphamap-io").start(task);

    TileQueue queue = new TileQueue();

    @NonFinal
    @Nullable
    AtlasTextures atlas;
    @NonFinal
    int epoch;
    @NonFinal
    long helloAt;

    @NonFinal
    @Getter
    @Nullable
    Component status;

    @NonFinal
    @Getter
    List<ServerMessage.Marker> markers = List.of();

    public @Nullable AtlasGeometry geometry() {
        return atlas != null ? atlas.geometry() : null;
    }

    public @Nullable Identifier texture(int tileX, int tileZ) {
        return atlas != null ? atlas.texture(tileX, tileZ) : null;
    }

    public void hello() {
        long now = System.currentTimeMillis();
        if (now - helloAt < HELLO_INTERVAL_MILLIS) return;

        if (!ClientPlayNetworking.canSend(MapPayload.TYPE)) {
            status = Component.translatable("alphamap.no_server");
            return;
        }

        helloAt = now;
        if (atlas == null) status = Component.translatable("alphamap.loading");
        ClientPlayNetworking.send(new MapPayload(MapProtocol.hello()));
    }

    public void tick() {
        AtlasTextures current = atlas;
        if (current == null) return;

        List<Integer> batch = queue.nextBatch(System.currentTimeMillis());
        if (batch == null) return;

        ClientPlayNetworking.send(new MapPayload(MapProtocol.request(batch, current.geometry())));
    }

    public void receive(byte[] data) {
        switch (ServerMessage.parse(data)) {
            case ServerMessage.Manifest manifest -> manifest(manifest);
            case ServerMessage.Tile tile -> tile(tile);
            case ServerMessage.Unavailable unavailable -> unavailable(unavailable.reason());
            case ServerMessage.Markers received -> markers = received.markers();
            case null -> {
            }
        }
    }

    public void reset() {
        if (atlas != null) atlas.release();
        atlas = null;
        status = null;
        helloAt = 0;
        epoch++;
        markers = List.of();
        queue.clear();
    }

    private void manifest(ServerMessage.Manifest manifest) {
        if (manifest.protocolVersion() != MapProtocol.VERSION) {
            status = Component.translatable("alphamap.version");
            return;
        }

        AtlasGeometry geometry = manifest.geometry();
        AtlasTextures next = new AtlasTextures(geometry, manifest.hashes());

        AtlasTextures previous = atlas;
        boolean sameIsland = previous != null && previous.geometry().id() == geometry.id();
        if (previous != null) {
            // Переносить тайлы можно, только если сетка не поменялась: иначе тот же индекс означает
            // другой кусок мира. Геометрия — рекорд, так что сравнение покрывает и остров, и сетку.
            if (previous.geometry().equals(geometry)) previous.handOver(next);
            previous.release();
        }

        atlas = next;
        status = null;
        epoch++;
        queue.clear();

        int generation = epoch;
        List<Integer> wanted = new ArrayList<>();
        for (int index = 0; index < next.count(); index++) {
            if (MapProtocol.isBlank(next.hash(index)) || next.current(index)) continue;
            wanted.add(index);
        }

        TileCache cache = cache();
        long id = geometry.id();
        OFF_THREAD.execute(() -> {
            if (!sameIsland) cache.keepOnly(id);
            for (int index : wanted) {
                NativeImage image = decode(cache.read(id, next.hash(index)));
                Minecraft.getInstance().execute(() -> cached(generation, index, image));
            }
        });
    }

    private void tile(ServerMessage.Tile tile) {
        AtlasTextures current = atlas;
        if (current == null || !current.geometry().contains(tile.tileX(), tile.tileZ())) return;

        int index = current.geometry().index(tile.tileX(), tile.tileZ());
        int generation = epoch;
        long id = current.geometry().id();

        TileCache cache = cache();
        OFF_THREAD.execute(() -> {
            cache.write(id, tile.hash(), tile.png());
            NativeImage image = decode(tile.png());
            Minecraft.getInstance().execute(() -> answered(generation, index, tile.hash(), image));
        });
    }

    private void unavailable(ServerMessage.Reason reason) {
        status = Component.translatable(switch (reason) {
            case NO_MAP -> "alphamap.unavailable.no_map";
            case NOT_READY -> "alphamap.unavailable.not_ready";
            case WRONG_VERSION -> "alphamap.version";
            case TOO_FAST, UNKNOWN -> "alphamap.unavailable.too_fast";
        });
    }

    private void cached(int generation, int index, @Nullable NativeImage image) {
        AtlasTextures current = accept(generation, image);
        if (current == null) return;

        if (image == null) queue.need(index);
        else current.put(index, current.hash(index), image);
    }

    private void answered(int generation, int index, byte[] hash, @Nullable NativeImage image) {
        AtlasTextures current = accept(generation, image);
        if (current == null) return;

        queue.answered(index);
        if (image != null) current.put(index, hash, image);
    }

    private @Nullable AtlasTextures accept(int generation, @Nullable NativeImage image) {
        AtlasTextures current = atlas;
        if (current != null && generation == epoch) return current;

        if (image != null) image.close();
        return null;
    }

    private static TileCache cache() {
        Path root = Minecraft.getInstance().gameDirectory.toPath().resolve("alphamap");
        return new TileCache(root);
    }

    private static @Nullable NativeImage decode(byte @Nullable [] png) {
        if (png == null) return null;
        try {
            return NativeImage.read(png);
        } catch (IOException broken) {
            return null;
        }
    }
}

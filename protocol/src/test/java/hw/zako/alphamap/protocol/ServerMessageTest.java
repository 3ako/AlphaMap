package hw.zako.alphamap.protocol;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Байты здесь собираются ровно так, как их пишет `MapAtlasChannel` на сервере. Если формат
/// разъедется, разъедется и этот тест — больше сверять его не с чем, сервер и мод живут в разных
/// репозиториях.
class ServerMessageTest {

    private static final int TILES_PER_SIDE = 7;

    @Test
    void readsAManifestTheServerWould() {
        byte[][] hashes = hashes(TILES_PER_SIDE * TILES_PER_SIDE);
        hashes[0] = new byte[MapProtocol.HASH_LENGTH];

        ServerMessage message = ServerMessage.parse(manifest(hashes));

        ServerMessage.Manifest manifest = assertInstanceOf(ServerMessage.Manifest.class, message);
        assertEquals(MapProtocol.VERSION, manifest.protocolVersion());

        AtlasGeometry geometry = manifest.geometry();
        assertEquals(0x1234_5678_9ABC_DEF0L, geometry.id());
        assertEquals(-1792, geometry.originX());
        assertEquals(-1792, geometry.originZ());
        assertEquals(2, geometry.blocksPerPixel());
        assertEquals(256, geometry.tilePixels());
        assertEquals(TILES_PER_SIDE, geometry.tilesPerSide());
        assertEquals(1898, geometry.radius());

        assertEquals(49, manifest.hashes().length);
        assertTrue(MapProtocol.isBlank(manifest.hashes()[0]));
        assertArrayEquals(hashes[48], manifest.hashes()[48]);
    }

    @Test
    void manifestIsUnderAKilobyteAtTheServerDefaults() {
        assertEquals(818, manifest(hashes(TILES_PER_SIDE * TILES_PER_SIDE)).length);
    }

    @Test
    void readsATile() {
        byte[] hash = hash(7);
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

        ServerMessage.Tile tile = assertInstanceOf(ServerMessage.Tile.class,
                ServerMessage.parse(tile(3, 5, hash, png)));

        assertEquals(3, tile.tileX());
        assertEquals(5, tile.tileZ());
        assertArrayEquals(hash, tile.hash());
        assertArrayEquals(png, tile.png());
    }

    @Test
    void readsEveryUnavailableReason() {
        assertEquals(ServerMessage.Reason.NO_MAP, reason(0));
        assertEquals(ServerMessage.Reason.NOT_READY, reason(1));
        assertEquals(ServerMessage.Reason.WRONG_VERSION, reason(2));
        assertEquals(ServerMessage.Reason.TOO_FAST, reason(3));
        assertEquals(ServerMessage.Reason.UNKNOWN, reason(9));
    }

    @Test
    void refusesGarbageInsteadOfThrowing() {
        assertNull(ServerMessage.parse(new byte[0]));
        assertNull(ServerMessage.parse(new byte[]{(byte) 0xFF}));
        byte[] truncated = Arrays.copyOf(manifest(hashes(TILES_PER_SIDE * TILES_PER_SIDE)), 200);
        assertNull(ServerMessage.parse(truncated));
    }

    @Test
    void readsMarkers() {
        ServerMessage.Markers markers = assertInstanceOf(ServerMessage.Markers.class,
                ServerMessage.parse(markers(new String[]{"mineshaft", "quarry_ore"})));

        assertEquals(2, markers.markers().size());
        assertEquals(new ServerMessage.Marker(-1000, 500, "mineshaft"), markers.markers().get(0));
        assertEquals("quarry_ore", markers.markers().get(1).icon());
    }

    @Test
    void readsAnEmptyMarkerList() {
        ServerMessage.Markers markers = assertInstanceOf(ServerMessage.Markers.class,
                ServerMessage.parse(markers(new String[0])));
        assertTrue(markers.markers().isEmpty());
    }

    private static byte[] markers(String[] icons) {
        int size = 1 + 2;
        for (String icon : icons) size += 4 + 4 + 1 + icon.getBytes(StandardCharsets.UTF_8).length;

        ByteBuffer out = ByteBuffer.allocate(size);
        out.put((byte) ServerMessage.MARKERS).putShort((short) icons.length);
        for (int i = 0; i < icons.length; i++) {
            byte[] id = icons[i].getBytes(StandardCharsets.UTF_8);
            out.putInt(-1000 + i).putInt(500 + i).put((byte) id.length).put(id);
        }
        return out.array();
    }

    private static byte[] manifest(byte[][] hashes) {
        int tilesPerSide = (int) Math.round(Math.sqrt(hashes.length));
        ByteBuffer out = ByteBuffer.allocate(
                1 + 4 + 8 + 4 + 4 + 4 + 4 + 1 + 4 + hashes.length * MapProtocol.HASH_LENGTH);

        out.put((byte) ServerMessage.MANIFEST)
                .putInt(MapProtocol.VERSION)
                .putLong(0x1234_5678_9ABC_DEF0L)
                .putInt(-1792)
                .putInt(-1792)
                .putInt(2)
                .putInt(256)
                .put((byte) tilesPerSide)
                .putInt(1898);
        for (byte[] hash : hashes) {
            out.put(hash);
        }
        return out.array();
    }

    private static byte[] tile(int tileX, int tileZ, byte[] hash, byte[] png) {
        return ByteBuffer.allocate(1 + 1 + 1 + MapProtocol.HASH_LENGTH + 4 + png.length)
                .put((byte) ServerMessage.TILE)
                .put((byte) tileX)
                .put((byte) tileZ)
                .put(hash)
                .putInt(png.length)
                .put(png)
                .array();
    }

    private static ServerMessage.Reason reason(int code) {
        ServerMessage message = ServerMessage.parse(new byte[]{(byte) ServerMessage.UNAVAILABLE, (byte) code});
        return assertInstanceOf(ServerMessage.Unavailable.class, message).reason();
    }

    private static byte[][] hashes(int count) {
        byte[][] hashes = new byte[count][];
        for (int i = 0; i < count; i++) {
            hashes[i] = hash(i);
        }
        return hashes;
    }

    private static byte[] hash(int seed) {
        byte[] hash = new byte[MapProtocol.HASH_LENGTH];
        Arrays.fill(hash, (byte) (seed + 1));
        return hash;
    }
}

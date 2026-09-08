package hw.zako.alphamap.protocol;

import org.jetbrains.annotations.Nullable;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public sealed interface ServerMessage {

    int MANIFEST = 0x00;
    int TILE = 0x01;
    int UNAVAILABLE = 0x02;
    int MARKERS = 0x03;

    record Manifest(int protocolVersion, AtlasGeometry geometry, byte[][] hashes) implements ServerMessage {
    }

    record Tile(int tileX, int tileZ, byte[] hash, byte[] png) implements ServerMessage {
    }

    record Unavailable(Reason reason) implements ServerMessage {
    }

    record Markers(List<Marker> markers) implements ServerMessage {
    }

    record Marker(int x, int z, String icon, String label) {
    }

    enum Reason {
        NO_MAP,
        NOT_READY,
        WRONG_VERSION,
        TOO_FAST,
        UNKNOWN;

        static Reason of(int code) {
            return switch (code) {
                case 0 -> NO_MAP;
                case 1 -> NOT_READY;
                case 2 -> WRONG_VERSION;
                case 3 -> TOO_FAST;
                default -> UNKNOWN;
            };
        }
    }

    static @Nullable ServerMessage parse(byte[] data) {
        ByteBuffer in = ByteBuffer.wrap(data);
        try {
            return switch (in.get() & 0xFF) {
                case MANIFEST -> manifest(in);
                case TILE -> tile(in);
                case UNAVAILABLE -> new Unavailable(Reason.of(in.get() & 0xFF));
                case MARKERS -> markers(in);
                default -> null;
            };
        } catch (BufferUnderflowException malformed) {
            return null;
        }
    }

    private static Manifest manifest(ByteBuffer in) {
        int protocolVersion = in.getInt();
        AtlasGeometry geometry = new AtlasGeometry(
                in.getLong(),
                in.getInt(),
                in.getInt(),
                in.getInt(),
                in.getInt(),
                in.get() & 0xFF,
                in.getInt());

        byte[][] hashes = new byte[geometry.tileCount()][];
        for (int i = 0; i < hashes.length; i++) {
            byte[] hash = new byte[MapProtocol.HASH_LENGTH];
            in.get(hash);
            hashes[i] = hash;
        }
        return new Manifest(protocolVersion, geometry, hashes);
    }

    private static Markers markers(ByteBuffer in) {
        int count = in.getShort() & 0xFFFF;
        List<Marker> markers = new ArrayList<>(Math.min(count, 512));
        for (int i = 0; i < count; i++) {
            int x = in.getInt();
            int z = in.getInt();
            byte[] icon = new byte[in.get() & 0xFF];
            in.get(icon);
            byte[] label = new byte[in.get() & 0xFF];
            in.get(label);
            markers.add(new Marker(x, z,
                    new String(icon, StandardCharsets.UTF_8),
                    new String(label, StandardCharsets.UTF_8)));
        }
        return new Markers(List.copyOf(markers));
    }

    private static Tile tile(ByteBuffer in) {
        int tileX = in.get() & 0xFF;
        int tileZ = in.get() & 0xFF;

        byte[] hash = new byte[MapProtocol.HASH_LENGTH];
        in.get(hash);

        byte[] png = new byte[in.getInt()];
        in.get(png);

        return new Tile(tileX, tileZ, hash, png);
    }
}

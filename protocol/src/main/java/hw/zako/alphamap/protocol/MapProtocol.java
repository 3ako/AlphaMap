package hw.zako.alphamap.protocol;

import lombok.experimental.UtilityClass;

import java.nio.ByteBuffer;
import java.util.List;

@UtilityClass
public class MapProtocol {

    public final String CHANNEL = "alphaisland:map";
    public final int VERSION = 3;

    public final int HASH_LENGTH = 16;

    public final int MAX_TILES_PER_REQUEST = 8;

    private final int HELLO = 0x00;
    private final int REQUEST = 0x01;

    public byte[] hello() {
        return ByteBuffer.allocate(1 + 4)
                .put((byte) HELLO)
                .putInt(VERSION)
                .array();
    }

    public byte[] request(List<Integer> indices, AtlasGeometry geometry) {
        if (indices.isEmpty() || indices.size() > MAX_TILES_PER_REQUEST) {
            throw new IllegalArgumentException(
                    "За раз можно просить от 1 до " + MAX_TILES_PER_REQUEST + " тайлов, а не " + indices.size());
        }

        ByteBuffer out = ByteBuffer.allocate(1 + 1 + indices.size() * 2);
        out.put((byte) REQUEST).put((byte) indices.size());
        for (int index : indices) {
            out.put((byte) geometry.tileX(index));
            out.put((byte) geometry.tileZ(index));
        }
        return out.array();
    }

    public boolean isBlank(byte[] hash) {
        for (byte b : hash) {
            if (b != 0) return false;
        }
        return true;
    }
}

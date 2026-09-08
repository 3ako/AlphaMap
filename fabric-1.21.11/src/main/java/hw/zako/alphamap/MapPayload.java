package hw.zako.alphamap;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import hw.zako.alphamap.protocol.MapProtocol;
import net.minecraft.resources.Identifier;

public record MapPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<MapPayload> TYPE =
            new Type<>(Identifier.parse(MapProtocol.CHANNEL));

    public static final StreamCodec<FriendlyByteBuf, MapPayload> CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> buffer.writeBytes(payload.data()),
            buffer -> {
                byte[] data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
                return new MapPayload(data);
            });

    @Override
    public Type<MapPayload> type() {
        return TYPE;
    }
}

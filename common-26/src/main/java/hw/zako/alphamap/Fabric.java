package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;

@UtilityClass
public class Fabric {

    public void registerKey(KeyMapping key) {
        KeyMappingHelper.registerKeyMapping(key);
    }

    public void registerPayload() {
        PayloadTypeRegistry.serverboundPlay().register(MapPayload.TYPE, MapPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MapPayload.TYPE, MapPayload.CODEC);
    }
}

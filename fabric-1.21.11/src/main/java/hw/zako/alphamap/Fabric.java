package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;

@UtilityClass
public class Fabric {

    public void registerKey(KeyMapping key) {
        KeyBindingHelper.registerKeyBinding(key);
    }

    public void registerPayload() {
        PayloadTypeRegistry.playC2S().register(MapPayload.TYPE, MapPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MapPayload.TYPE, MapPayload.CODEC);
    }
}

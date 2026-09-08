package hw.zako.alphamap;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class AlphaMapClient implements ClientModInitializer {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("alphamap", "map"));

    public static final KeyMapping OPEN_MAP = new KeyMapping(
            "key.alphamap.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    private static final Identifier OVERLAY = Identifier.fromNamespaceAndPath("alphamap", "map");

    public static boolean pinned() {
        return !MapSketch.isEmpty() || MapInput.engaged() || MapInput.tool() == MapTool.PENCIL;
    }

    public static boolean mapOpen() {
        return OPEN_MAP.isDown() || pinned();
    }
    private static final Identifier WAYPOINTS = Identifier.fromNamespaceAndPath("alphamap", "waypoints");

    @Override
    public void onInitializeClient() {
        AtlasClient atlas = new AtlasClient();
        MapSettings settings = MapSettings.load();

        PayloadTypeRegistry.playC2S().register(MapPayload.TYPE, MapPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MapPayload.TYPE, MapPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(MapPayload.TYPE,
                (payload, context) -> atlas.receive(payload.data()));

        KeyBindingHelper.registerKeyBinding(OPEN_MAP);

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, WAYPOINTS, new WaypointOverlay(atlas, settings));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, OVERLAY, new MapOverlay(atlas, settings));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> Waypoints.enter());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            atlas.reset();
            MapInput.reset(client);
            MapSketch.clear();
            Waypoints.leave();
        });

        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private boolean wasDown;

            @Override
            public void onEndTick(Minecraft client) {
                if (client.player == null) {
                    MapInput.reset(client);
                    wasDown = false;
                    return;
                }

                boolean open = mapOpen();
                if (open && !wasDown) atlas.hello();
                if (open) atlas.tick();
                MapInput.update(client, open);
                wasDown = open;
            }
        });
    }
}

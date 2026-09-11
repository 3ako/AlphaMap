package hw.zako.alphamap;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
    private static final Identifier MINIMAP = Identifier.fromNamespaceAndPath("alphamap", "minimap");

    private static MinimapOverlay minimap;

    public static MinimapOverlay minimap() {
        return minimap;
    }

    private static void forget(AtlasClient atlas, Minecraft client) {
        atlas.reset();
        MapInput.reset(client);
        MapSketch.clear();
        MinimapEntities.clear();
        minimap.release();
    }

    @Override
    public void onInitializeClient() {
        AtlasClient atlas = new AtlasClient();
        MapSettings settings = MapSettings.load();

        Fabric.registerPayload();
        ClientPlayNetworking.registerGlobalReceiver(MapPayload.TYPE,
                (payload, context) -> atlas.receive(payload.data()));

        Fabric.registerKey(OPEN_MAP);

        minimap = new MinimapOverlay(atlas, settings);

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, MINIMAP, new MinimapHud(minimap));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, WAYPOINTS, new WaypointHud(new WaypointOverlay(atlas, settings)));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, OVERLAY, new MapHud(new MapOverlay(atlas, settings)));

        // Переход между серверами прокси (/hub, /spawn) соединение не рвёт: DISCONNECT не придёт,
        // а JOIN придёт — бэкенд шлёт логин заново. Поэтому карту забываем именно на JOIN.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            forget(atlas, client);
            Waypoints.enter();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            forget(atlas, client);
            Waypoints.leave();
        });

        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private boolean wasWanted;
            private ClientLevel world;

            @Override
            public void onEndTick(Minecraft client) {
                // Подстраховка к JOIN: мир пересоздают и при переходе между серверами прокси, и при
                // смене измерения. Смерть в том же мире мир не меняет, так что карту она не сбросит.
                if (client.level != world) {
                    world = client.level;
                    forget(atlas, client);
                    wasWanted = false;
                }

                if (client.player == null) {
                    MapInput.reset(client);
                    MinimapEntities.clear();
                    wasWanted = false;
                    return;
                }

                boolean open = mapOpen();
                boolean ready = atlas.ready();
                // Сам спрашиваем сервер только ради миникарты и только если карта там вообще есть;
                // на открытие карты руками отвечаем всегда — иначе игрок не узнает, что её нет.
                boolean wanted = open || (settings.minimap() && AtlasClient.available());
                if (wanted && (!wasWanted || !ready)) atlas.hello();
                if (wanted) atlas.tick();

                // Мышь, миксины и обход сущностей включаются только когда карта загружена.
                MapInput.update(client, open && ready);
                MinimapEntities.tick(client, settings, ready);
                wasWanted = wanted;
            }
        });
    }
}

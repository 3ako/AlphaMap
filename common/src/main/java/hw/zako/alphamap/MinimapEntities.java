package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class MinimapEntities {

    public enum Kind {
        PLAYER,
        HOSTILE,
        PASSIVE
    }

    public record Dot(Entity entity, Kind kind,
                      @Nullable Identifier texture, MobHeads.@Nullable Head head) {
    }

    private final int REFRESH_TICKS = 10;
    private final int LIMIT = 128;

    private final List<Dot> shown = new ArrayList<>();

    private int waited;

    public List<Dot> shown() {
        return shown;
    }

    public void clear() {
        shown.clear();
        waited = 0;
    }

    public void tick(Minecraft client, MapSettings settings, boolean ready, boolean mapOpen) {
        if (waited-- > 0) return;
        waited = REFRESH_TICKS;

        clear();
        if (!ready || client.player == null || client.level == null) return;
        if (!settings.minimap() && !mapOpen) return;
        if (!settings.minimapPlayers() && !settings.minimapHostiles() && !settings.minimapPassives()) return;

        double reach = settings.minimapBlocks();
        if (mapOpen) {
            reach = Math.max(reach, client.options.getEffectiveRenderDistance() * 16.0);
        }
        double range = reach * reach;
        collect(client, client.level, settings, range);
    }

    private void collect(Minecraft client, ClientLevel level, MapSettings settings, double range) {
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == client.player || entity.isInvisible()) continue;

            double dx = entity.getX() - client.player.getX();
            double dz = entity.getZ() - client.player.getZ();
            if (dx * dx + dz * dz > range) continue;

            Kind kind = kind(client, entity, settings);
            if (kind == null) continue;

            String id = Vanilla.mobId(entity);
            String state = id != null && MobHeads.has(id)
                    ? settings.minimapMob(id)
                    : kindState(settings, kind);
            if (MapSettings.OFF.equals(state)) continue;

            MobHeads.Head head = MapSettings.HEADS.equals(state) ? MobHeads.of(entity) : null;
            Identifier texture = head != null ? Vanilla.mobTexture(client, entity) : null;
            shown.add(new Dot(entity, kind, texture, texture != null ? head : null));
            if (shown.size() >= LIMIT) return;
        }
    }

    private @Nullable Kind kind(Minecraft client, Entity entity, MapSettings settings) {
        if (entity instanceof Player player) {
            return spectator(client, player) ? null : Kind.PLAYER;
        }
        if (!(entity instanceof Mob)) return null;
        return entity instanceof Enemy ? Kind.HOSTILE : Kind.PASSIVE;
    }

    private String kindState(MapSettings settings, Kind kind) {
        boolean shown = switch (kind) {
            case PLAYER -> settings.minimapPlayers();
            case HOSTILE -> settings.minimapHostiles();
            case PASSIVE -> settings.minimapPassives();
        };
        return shown ? MapSettings.DOTS : MapSettings.OFF;
    }

    private boolean spectator(Minecraft client, Player player) {
        if (player.isSpectator()) return true;
        if (client.getConnection() == null) return false;

        PlayerInfo info = client.getConnection().getPlayerInfo(player.getUUID());
        return info != null && info.getGameMode() == GameType.SPECTATOR;
    }
}

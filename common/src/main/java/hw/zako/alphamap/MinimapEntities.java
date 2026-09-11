package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// Список ищется раз в полсекунды, а координаты берутся у живых ссылок каждый кадр: точки едут плавно,
// но обход всех сущностей мира не попадает в отрисовку.
@UtilityClass
public class MinimapEntities {

    public enum Kind {
        PLAYER,
        HOSTILE,
        PASSIVE
    }

    public record Dot(Entity entity, Kind kind) {
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

    public void tick(Minecraft client, MapSettings settings, boolean ready) {
        if (waited-- > 0) return;
        waited = REFRESH_TICKS;

        clear();
        if (!ready || !settings.minimap() || client.player == null || client.level == null) return;
        if (!settings.minimapPlayers() && !settings.minimapHostiles() && !settings.minimapPassives()) return;

        double reach = settings.minimapBlocks();
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

            shown.add(new Dot(entity, kind));
            if (shown.size() >= LIMIT) return;
        }
    }

    private @Nullable Kind kind(Minecraft client, Entity entity, MapSettings settings) {
        if (entity instanceof Player player) {
            return settings.minimapPlayers() && !spectator(client, player) ? Kind.PLAYER : null;
        }
        if (!(entity instanceof Mob)) return null;

        if (entity instanceof Enemy) return settings.minimapHostiles() ? Kind.HOSTILE : null;
        return settings.minimapPassives() ? Kind.PASSIVE : null;
    }

    // Свой режим игрок знает по способностям, чужой — только из списка игроков в табе.
    private boolean spectator(Minecraft client, Player player) {
        if (player.isSpectator()) return true;
        if (client.getConnection() == null) return false;

        PlayerInfo info = client.getConnection().getPlayerInfo(player.getUUID());
        return info != null && info.getGameMode() == GameType.SPECTATOR;
    }
}

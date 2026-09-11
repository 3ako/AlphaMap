package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class DeathPoint {

    private final int COLOUR = 0xFFFF3B30;
    private final int OUTLINE = 0xC0000000;

    public @Nullable BlockPos of(Minecraft client, MapSettings settings) {
        if (!settings.deathPoint() || client.player == null || client.level == null) return null;

        return client.player.getLastDeathLocation()
                .filter(place -> place.dimension().equals(client.level.dimension()))
                .map(GlobalPos::pos)
                .orElse(null);
    }

    public void draw(Canvas canvas, int x, int y, int half) {
        canvas.fill(x - half - 1, y - half - 1, x + half + 1, y + half + 1, OUTLINE);
        canvas.fill(x - half, y - half, x + half, y - half + 1, COLOUR);
        canvas.fill(x - half, y + half - 1, x + half, y + half, COLOUR);
        canvas.fill(x - half, y - half, x - half + 1, y + half, COLOUR);
        canvas.fill(x + half - 1, y - half, x + half, y + half, COLOUR);
        canvas.fill(x - 1, y - 1, x + 1, y + 1, COLOUR);
    }
}

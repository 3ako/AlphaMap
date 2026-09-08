package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class MarkerIcons {

    private final Set<String> KNOWN = Set.of(
            "airland", "bastion", "beacon", "cave_evil", "cave_good", "copper", "mill",
            "mineshaft", "promzone", "quarry_ore", "quarry_sulfur", "stronghold", "village");

    private final Map<String, Identifier> RESOLVED = new ConcurrentHashMap<>();

    public @Nullable Identifier of(String icon) {
        if (!KNOWN.contains(icon)) return null;
        return RESOLVED.computeIfAbsent(icon,
                name -> Identifier.fromNamespaceAndPath("alphamap", "textures/marker/" + name + ".png"));
    }
}

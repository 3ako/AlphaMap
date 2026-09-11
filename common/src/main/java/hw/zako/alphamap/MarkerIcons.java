package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class MarkerIcons {

    // Текстуры, которые лежат в моде. Всё остальное рисуется запасной иконкой, а не пропадает.
    private final Set<String> SHIPPED = Set.of(
            "airland", "bastion", "beacon", "bed", "cave_evil", "cave_good", "copper", "mill",
            "mineshaft", "promzone", "quarry_ore", "quarry_sulfur", "stronghold", "village");

    private final Identifier UNKNOWN =
            Identifier.fromNamespaceAndPath("alphamap", "textures/marker/unknown.png");

    private final Map<String, Identifier> RESOLVED = new ConcurrentHashMap<>();

    // Типы, которые сервер присылал в этой сессии: из них строится список в настройках, поэтому
    // новая структура на сервере появляется в настройках сама, без правок мода.
    private final Set<String> SEEN = ConcurrentHashMap.newKeySet();

    public Identifier of(String icon) {
        return RESOLVED.computeIfAbsent(icon, name -> SHIPPED.contains(name)
                ? Identifier.fromNamespaceAndPath("alphamap", "textures/marker/" + name + ".png")
                : UNKNOWN);
    }

    public void remember(String icon) {
        SEEN.add(icon);
    }

    public Set<String> seen() {
        return Set.copyOf(SEEN);
    }
}

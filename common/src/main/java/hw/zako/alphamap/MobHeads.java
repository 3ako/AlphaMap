package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

@UtilityClass
public class MobHeads {

    public record Head(int u, int v, int width, int height, int sheetWidth, int sheetHeight,
                      Identifier sample) {
    }

    private final Head HUMANOID = new Head(8, 8, 8, 8, 64, 64, null);
    private final Head HUMANOID_FLAT = new Head(8, 8, 8, 8, 64, 32, null);
    private final Head VILLAGER = new Head(8, 8, 8, 10, 64, 64, null);

    private final Map<String, Head> HEADS = new LinkedHashMap<>();
    private final Map<String, String> KEYS = new LinkedHashMap<>();
    private final Map<String, String> SAMPLES = new LinkedHashMap<>();
    private final Set<String> HOSTILE = new LinkedHashSet<>();

    private void put(String id, Head head, boolean hostile) {
        String sample = SAMPLES.get(id);
        HEADS.put(id, new Head(head.u(), head.v(), head.width(), head.height(),
                head.sheetWidth(), head.sheetHeight(),
                Identifier.fromNamespaceAndPath("minecraft", "textures/entity/" + sample + ".png")));
        if (hostile) HOSTILE.add(id);
    }

    static {
        SAMPLES.put("player", "player/wide/steve");
        SAMPLES.put("zombie", "zombie/zombie");
        SAMPLES.put("husk", "zombie/husk");
        SAMPLES.put("drowned", "zombie/drowned");
        SAMPLES.put("zombie_villager", "zombie_villager/zombie_villager");
        SAMPLES.put("skeleton", "skeleton/skeleton");
        SAMPLES.put("stray", "skeleton/stray");
        SAMPLES.put("bogged", "skeleton/bogged");
        SAMPLES.put("wither_skeleton", "skeleton/wither_skeleton");
        SAMPLES.put("piglin_brute", "piglin/piglin_brute");
        SAMPLES.put("zombified_piglin", "piglin/zombified_piglin");
        SAMPLES.put("giant", "zombie/zombie");
        SAMPLES.put("piglin", "piglin/piglin");
        SAMPLES.put("pig", "pig/temperate_pig");
        SAMPLES.put("creeper", "creeper/creeper");
        SAMPLES.put("enderman", "enderman/enderman");
        SAMPLES.put("blaze", "blaze");
        SAMPLES.put("sheep", "sheep/sheep");
        SAMPLES.put("witch", "witch");
        SAMPLES.put("illager", "illager/pillager");
        SAMPLES.put("vindicator", "illager/vindicator");
        SAMPLES.put("evoker", "illager/evoker");
        SAMPLES.put("illusioner", "illager/illusioner");
        SAMPLES.put("villager", "villager/villager");
        SAMPLES.put("wandering_trader", "wandering_trader");
        SAMPLES.put("spider", "spider/spider");
        SAMPLES.put("cave_spider", "spider/cave_spider");
        SAMPLES.put("cow", "cow/temperate_cow");
        SAMPLES.put("mooshroom", "cow/red_mooshroom");
        SAMPLES.put("chicken", "chicken/temperate_chicken");
        SAMPLES.put("ghast", "ghast/ghast");
        SAMPLES.put("slime", "slime/slime");
        SAMPLES.put("magma_cube", "slime/magmacube");
        SAMPLES.put("wolf", "wolf/wolf");
        SAMPLES.put("cat", "cat/tabby");
        SAMPLES.put("fox", "fox/fox");
        SAMPLES.put("bee", "bee/bee");
        SAMPLES.put("iron_golem", "iron_golem/iron_golem");
        SAMPLES.put("snow_golem", "snow_golem");
        SAMPLES.put("ravager", "illager/ravager");
        SAMPLES.put("warden", "warden/warden");
        SAMPLES.put("panda", "panda/panda");
        SAMPLES.put("polar_bear", "bear/polarbear");
        SAMPLES.put("rabbit", "rabbit/brown");
        SAMPLES.put("goat", "goat/goat");
        SAMPLES.put("llama", "llama/creamy");
        SAMPLES.put("turtle", "turtle/big_sea_turtle");
        SAMPLES.put("axolotl", "axolotl/axolotl_lucy");
        SAMPLES.put("bat", "bat");
        SAMPLES.put("dolphin", "dolphin");
        SAMPLES.put("sniffer", "sniffer/sniffer");
        SAMPLES.put("ocelot", "cat/ocelot");
        SAMPLES.put("parrot", "parrot/parrot_red_blue");
        SAMPLES.put("happy_ghast", "ghast/happy_ghast");
        SAMPLES.put("guardian", "guardian");
        SAMPLES.put("elder_guardian", "guardian_elder");
        SAMPLES.put("creaking", "creaking/creaking");
        SAMPLES.put("shulker", "shulker/shulker");
        SAMPLES.put("hoglin", "hoglin/hoglin");
        SAMPLES.put("zoglin", "hoglin/zoglin");
        SAMPLES.put("phantom", "phantom");

        for (String id : new String[]{"zombie", "husk", "drowned", "zombie_villager",
                "piglin_brute", "zombified_piglin", "giant"}) {
            put(id, HUMANOID, true);
        }
        for (String id : new String[]{"skeleton", "stray", "bogged", "wither_skeleton"}) {
            put(id, HUMANOID_FLAT, true);
        }
        put("player", HUMANOID, false);
        put("piglin", HUMANOID, false);
        put("pig", HUMANOID, false);

        put("creeper", HUMANOID_FLAT, true);
        put("enderman", HUMANOID_FLAT, true);
        put("blaze", HUMANOID_FLAT, true);
        put("sheep", new Head(8, 8, 6, 6, 64, 32, null), false);

        group("illager", VILLAGER, true, "pillager", "vindicator", "evoker", "illusioner");
        put("witch", new Head(8, 8, 8, 10, 64, 128, null), true);
        put("villager", VILLAGER, false);
        put("wandering_trader", VILLAGER, false);

        put("spider", new Head(40, 12, 8, 8, 64, 32, null), true);
        put("cave_spider", new Head(40, 12, 8, 8, 64, 32, null), true);
        put("cow", new Head(6, 6, 8, 8, 64, 64, null), false);
        put("mooshroom", new Head(6, 6, 8, 8, 64, 64, null), false);
        put("chicken", new Head(3, 3, 4, 6, 64, 32, null), false);

        put("ghast", new Head(16, 16, 16, 16, 64, 32, null), true);
        put("wolf", new Head(4, 4, 6, 6, 64, 32, null), false);
        put("cat", new Head(5, 5, 5, 4, 64, 32, null), false);
        put("fox", new Head(7, 11, 8, 6, 48, 32, null), false);
        put("bee", new Head(10, 10, 7, 7, 64, 64, null), false);

        put("iron_golem", new Head(8, 8, 8, 10, 128, 128, null), false);
        put("snow_golem", new Head(8, 8, 8, 8, 64, 64, null), false);
        put("panda", new Head(9, 15, 13, 10, 64, 64, null), false);
        put("polar_bear", new Head(7, 7, 7, 7, 128, 64, null), false);
        put("ravager", new Head(16, 16, 16, 20, 128, 128, null), true);
        put("warden", new Head(10, 42, 16, 16, 128, 128, null), true);

        put("rabbit", new Head(37, 5, 5, 4, 64, 32, null), false);
        put("goat", new Head(44, 56, 5, 7, 64, 64, null), false);
        group("llama", new Head(9, 9, 4, 4, 128, 64, null), false, "trader_llama");
        put("turtle", new Head(9, 6, 6, 5, 128, 64, null), false);
        put("axolotl", new Head(5, 6, 8, 5, 64, 64, null), false);
        put("bat", new Head(2, 9, 4, 3, 32, 32, null), false);
        put("dolphin", new Head(6, 6, 8, 7, 64, 64, null), false);
        put("sniffer", new Head(8, 15, 18, 11, 192, 192, null), false);
        put("ocelot", new Head(5, 5, 5, 4, 64, 32, null), false);
        put("parrot", new Head(2, 2, 6, 6, 32, 32, null), false);
        put("happy_ghast", new Head(16, 16, 16, 16, 64, 64, null), false);

        put("guardian", new Head(16, 16, 12, 12, 64, 64, null), true);
        put("elder_guardian", new Head(16, 16, 12, 12, 64, 64, null), true);
        put("creaking", new Head(6, 6, 6, 10, 64, 64, null), true);
        put("shulker", new Head(6, 58, 6, 6, 64, 64, null), true);
        put("hoglin", new Head(80, 20, 14, 6, 128, 64, null), true);
        put("zoglin", new Head(80, 20, 14, 6, 128, 64, null), true);
        put("phantom", new Head(5, 5, 7, 3, 64, 64, null), true);
    }

    public List<String> known() {
        return List.copyOf(HEADS.keySet());
    }

    private void group(String key, Head head, boolean hostile, String... ids) {
        put(key, head, hostile);
        for (String id : ids) KEYS.put(id, key);
    }

    public String key(String id) {
        return KEYS.getOrDefault(id, id);
    }

    public boolean isGroup(String key) {
        return KEYS.containsValue(key);
    }

    public boolean has(String id) {
        return HEADS.containsKey(key(id));
    }

    public boolean hostile(String id) {
        return HOSTILE.contains(key(id));
    }

    public @Nullable Head of(Entity entity) {
        String id = Vanilla.mobId(entity);
        return id != null ? of(id) : null;
    }

    public @Nullable Head of(String id) {
        return HEADS.get(key(id));
    }
}

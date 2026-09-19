package hw.zako.alphamap;

import com.mojang.blaze3d.vertex.PoseStack;
import hw.zako.alphamap.mixin.EntityRenderDispatcherAccessor;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;

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
    private final Set<String> MONSTERS = new HashSet<>();

    private final Map<String, EntityType<?>> TYPES = new HashMap<>();
    private final Map<String, Optional<Head>> DERIVED = new HashMap<>();
    private final int UV_SCALE = 4096;

    private @Nullable Set<String> registered;
    private @Nullable List<String> known;

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
        put("slime", new Head(8, 8, 8, 8, 64, 32, null), true);
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
        if (known == null) {
            Set<String> ids = new LinkedHashSet<>(HEADS.keySet());
            ids.addAll(registered());
            List<String> sorted = new ArrayList<>(ids);
            sorted.sort(Comparator.comparing((String id) -> !"player".equals(id))
                    .thenComparing(id -> !hostile(id))
                    .thenComparing(Comparator.naturalOrder()));
            known = List.copyOf(sorted);
        }
        return known;
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

    private Set<String> registered() {
        if (registered == null) {
            Set<String> ids = new LinkedHashSet<>();
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                Identifier id = EntityType.getKey(type);
                if (!"minecraft".equals(id.getNamespace())) continue;
                TYPES.put(id.getPath(), type);
                if (type.getCategory() == MobCategory.MISC) continue;
                ids.add(key(id.getPath()));
                if (type.getCategory() == MobCategory.MONSTER) MONSTERS.add(key(id.getPath()));
            }
            registered = ids;
        }
        return registered;
    }

    public boolean has(String id) {
        return known().contains(key(id));
    }

    public boolean hostile(String id) {
        String key = key(id);
        if (HEADS.containsKey(key)) return HOSTILE.contains(key);
        registered();
        return MONSTERS.contains(key);
    }

    public @Nullable Head of(Entity entity) {
        String id = Vanilla.mobId(entity);
        return id != null ? of(id) : null;
    }

    public @Nullable Head of(String id) {
        String key = key(id);
        Head head = HEADS.get(key);
        return head != null ? head : derived(key);
    }

    private @Nullable Head derived(String id) {
        Optional<Head> cached = DERIVED.get(id);
        if (cached != null) return cached.orElse(null);

        registered();
        EntityType<?> type = TYPES.get(id);
        Head head = null;
        if (type != null) {
            EntityRenderer<?, ?> renderer = ((EntityRenderDispatcherAccessor) Minecraft.getInstance()
                    .getEntityRenderDispatcher()).alphamap$renderers().get(type);
            if (renderer instanceof LivingEntityRenderer<?, ?, ?> living) head = face(living);
        }
        DERIVED.put(id, Optional.ofNullable(head));
        return head;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private @Nullable Head face(LivingEntityRenderer<?, ?, ?> renderer) {
        float[][] found = new float[2][];
        try {
            renderer.getModel().root().visit(new PoseStack(), (pose, path, index, cube) -> {
                if (index != 0) return;
                float[] front = front(cube);
                if (front == null) return;
                if (found[1] == null) found[1] = front;
                if (found[0] == null && path.endsWith("head")) found[0] = front;
            });
        } catch (RuntimeException e) {
            return null;
        }
        float[] box = found[0] != null ? found[0] : found[1];
        if (box == null) return null;

        Identifier sample;
        try {
            sample = ((LivingEntityRenderer) renderer).getTextureLocation(renderer.createRenderState());
        } catch (RuntimeException e) {
            sample = null;
        }
        return new Head(Math.round(box[0] * UV_SCALE), Math.round(box[1] * UV_SCALE),
                Math.max(1, Math.round((box[2] - box[0]) * UV_SCALE)),
                Math.max(1, Math.round((box[3] - box[1]) * UV_SCALE)),
                UV_SCALE, UV_SCALE, sample);
    }

    private float @Nullable [] front(ModelPart.Cube cube) {
        for (ModelPart.Polygon polygon : cube.polygons) {
            if (polygon.normal().z() > -0.9f) continue;
            float minU = Float.MAX_VALUE, minV = Float.MAX_VALUE, maxU = -Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                minU = Math.min(minU, vertex.u());
                minV = Math.min(minV, vertex.v());
                maxU = Math.max(maxU, vertex.u());
                maxV = Math.max(maxV, vertex.v());
            }
            return maxU > minU && maxV > minV ? new float[]{minU, minV, maxU, maxV} : null;
        }
        return null;
    }
}

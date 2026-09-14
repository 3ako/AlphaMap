package hw.zako.alphamap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class Waypoints {

    public final int[] PALETTE = {
            0xFFFFFF, 0xFF4D4D, 0xFF9F2B, 0xFFE04D,
            0x5FE86B, 0x62E8FF, 0x5B8CFF, 0xC77DFF};

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private List<Waypoint> loaded = new ArrayList<>();
    private long island;
    private boolean known;

    public List<Waypoint> all() {
        return loaded;
    }

    public void add(Waypoint waypoint) {
        loaded.add(waypoint);
        save();
    }

    public void replace(int index, Waypoint waypoint) {
        if (index < 0 || index >= loaded.size()) return;
        loaded.set(index, waypoint);
        save();
    }

    public void remove(int index) {
        if (index < 0 || index >= loaded.size()) return;
        loaded.remove(index);
        save();
    }

    public String defaultName() {
        return "Метка " + (loaded.size() + 1);
    }

    public boolean ready() {
        return known;
    }

    public void enter(long islandId) {
        if (known && island == islandId) return;

        island = islandId;
        known = true;
        loaded = read();
    }

    public void leave() {
        loaded = new ArrayList<>();
        island = 0;
        known = false;
    }

    private Path file() {
        return folder().resolve(Long.toHexString(island) + ".json");
    }

    private Path folder() {
        return FabricLoader.getInstance().getConfigDir().resolve("alphamap");
    }

    private List<Waypoint> read() {
        Path file = file();
        if (Files.exists(file)) return parse(file);

        Path old = folder().resolve(server() + ".json");
        if (!Files.exists(old)) return new ArrayList<>();

        List<Waypoint> list = parse(old);
        if (!write(file, list)) return list;

        try {
            Files.delete(old);
        } catch (IOException stubborn) {
        }
        return list;
    }

    private String server() {
        var current = Minecraft.getInstance().getCurrentServer();
        if (current == null || current.ip == null) return "singleplayer";
        return current.ip.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private List<Waypoint> parse(Path file) {
        try (var reader = Files.newBufferedReader(file)) {
            List<Waypoint> list = GSON.fromJson(reader, new TypeToken<List<Waypoint>>() {
            }.getType());
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        } catch (IOException | RuntimeException broken) {
            return new ArrayList<>();
        }
    }

    private void save() {
        if (!known) return;
        write(file(), loaded);
    }

    private boolean write(Path file, List<Waypoint> list) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(list));
            return true;
        } catch (IOException unwritable) {
            return false;
        }
    }
}

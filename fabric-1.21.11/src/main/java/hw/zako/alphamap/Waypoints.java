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
    private String server = "";

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

    public void enter() {
        server = key();
        loaded = read();
    }

    public void leave() {
        loaded = new ArrayList<>();
        server = "";
    }

    private String key() {
        var current = Minecraft.getInstance().getCurrentServer();
        if (current == null || current.ip == null) return "singleplayer";
        return current.ip.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private Path file() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("alphamap").resolve(server + ".json");
    }

    private List<Waypoint> read() {
        Path file = file();
        if (!Files.exists(file)) return new ArrayList<>();
        try (var reader = Files.newBufferedReader(file)) {
            List<Waypoint> list = GSON.fromJson(reader, new TypeToken<List<Waypoint>>() {
            }.getType());
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        } catch (IOException | RuntimeException broken) {
            return new ArrayList<>();
        }
    }

    private void save() {
        if (server.isEmpty()) return;
        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(loaded));
        } catch (IOException unwritable) {
        }
    }
}

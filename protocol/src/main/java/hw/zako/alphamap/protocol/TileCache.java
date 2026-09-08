package hw.zako.alphamap.protocol;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.stream.Stream;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class TileCache {

    Path root;

    public Path directoryOf(long atlasId) {
        return root.resolve(Long.toHexString(atlasId));
    }

    public Path fileOf(long atlasId, byte[] hash) {
        return directoryOf(atlasId).resolve(HexFormat.of().formatHex(hash) + ".png");
    }

    public byte @Nullable [] read(long atlasId, byte[] hash) {
        try {
            Path file = fileOf(atlasId, hash);
            return Files.exists(file) ? Files.readAllBytes(file) : null;
        } catch (IOException unreadable) {
            return null;
        }
    }

    public void write(long atlasId, byte[] hash, byte[] png) {
        try {
            Files.createDirectories(directoryOf(atlasId));
            Files.write(fileOf(atlasId, hash), png);
        } catch (IOException unwritable) {
        }
    }

    public void keepOnly(long atlasId) {
        String keeping = Long.toHexString(atlasId);
        try (Stream<Path> directories = Files.list(root)) {
            for (Path directory : directories.toList()) {
                if (directory.getFileName().toString().equals(keeping)) continue;
                delete(directory);
            }
        } catch (IOException nothingToClean) {
        }
    }

    private static void delete(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException stubborn) {
                }
            });
        }
    }
}

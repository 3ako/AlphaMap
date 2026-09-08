package hw.zako.alphamap.protocol;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class TileQueue {

    public static final long BATCH_TIMEOUT_MILLIS = 3_000;

    Set<Integer> missing = new LinkedHashSet<>();

    @NonFinal
    int outstanding;
    @NonFinal
    long batchAt;

    public void need(int index) {
        missing.add(index);
    }

    public void answered(int index) {
        missing.remove(index);
        if (outstanding > 0) outstanding--;
    }

    public boolean isEmpty() {
        return missing.isEmpty();
    }

    public int size() {
        return missing.size();
    }

    public void clear() {
        missing.clear();
        outstanding = 0;
        batchAt = 0;
    }

    public @Nullable List<Integer> nextBatch(long now) {
        if (missing.isEmpty()) return null;
        if (outstanding > 0 && now - batchAt < BATCH_TIMEOUT_MILLIS) return null;

        List<Integer> batch = missing.stream().limit(MapProtocol.MAX_TILES_PER_REQUEST).toList();
        outstanding = batch.size();
        batchAt = now;
        return batch;
    }
}

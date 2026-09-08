package hw.zako.alphamap.protocol;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileQueueTest {

    private static final AtlasGeometry ATLAS = new AtlasGeometry(1, -1792, -1792, 2, 256, 7, 1598);

    @Test
    void asksInBatchesOfEight() {
        TileQueue queue = queueOf(49);

        List<Integer> batch = queue.nextBatch(0);
        assertNotNull(batch);
        assertEquals(MapProtocol.MAX_TILES_PER_REQUEST, batch.size());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7), batch);
    }

    @Test
    void waitsForTheBatchBeforeAskingAgain() {
        TileQueue queue = queueOf(49);
        List<Integer> batch = queue.nextBatch(0);
        assertNotNull(batch);

        assertNull(queue.nextBatch(1), "пока пачка не приехала, следующая уходить не должна");

        for (int index : batch) {
            queue.answered(index);
        }
        assertNotNull(queue.nextBatch(2), "пачка приехала — можно за следующей");
    }

    @Test
    void doesNotHangOnATileThatNeverArrives() {
        TileQueue queue = queueOf(49);
        assertNotNull(queue.nextBatch(0));

        assertNull(queue.nextBatch(TileQueue.BATCH_TIMEOUT_MILLIS - 1));
        assertNotNull(queue.nextBatch(TileQueue.BATCH_TIMEOUT_MILLIS));
    }

    @Test
    void aWholeColdAtlasFitsTheServerBurst() {
        TileQueue queue = queueOf(49);

        int asked = 0;
        long now = 0;
        while (!queue.isEmpty()) {
            List<Integer> batch = queue.nextBatch(now);
            assertNotNull(batch, "очередь не пуста, а просить нечего");
            asked += batch.size();
            for (int index : batch) {
                queue.answered(index);
            }
            now++;
        }
        assertEquals(49, asked);
        assertTrue(asked <= 64);
    }

    @Test
    void anAnsweredTileNeverComesBack() {
        TileQueue queue = queueOf(3);
        queue.answered(0);
        queue.answered(1);
        queue.answered(2);
        assertTrue(queue.isEmpty());
        assertNull(queue.nextBatch(0));
    }

    @Test
    void encodesTheBatchAsTheServerReadsIt() {
        byte[] message = MapProtocol.request(List.of(0, 8, 48), ATLAS);

        assertEquals(1 + 1 + 3 * 2, message.length);
        assertEquals(0x01, message[0]);
        assertEquals(3, message[1]);
        assertEquals(0, message[2]);
        assertEquals(0, message[3]);
        assertEquals(1, message[4]);
        assertEquals(1, message[5]);
        assertEquals(6, message[6]);
        assertEquals(6, message[7]);
    }

    private static TileQueue queueOf(int tiles) {
        TileQueue queue = new TileQueue();
        for (int i = 0; i < tiles; i++) {
            queue.need(i);
        }
        return queue;
    }
}

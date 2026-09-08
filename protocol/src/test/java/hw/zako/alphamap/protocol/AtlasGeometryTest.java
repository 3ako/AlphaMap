package hw.zako.alphamap.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Геометрия боевых дефолтов сервера: остров радиусом 1598 блоков, сетка 7×7 по 256 пикселей,
/// два блока на пиксель.
class AtlasGeometryTest {

    private static final AtlasGeometry ATLAS = new AtlasGeometry(1, -1792, -1792, 2, 256, 7, 1598);

    @Test
    void indexingMatchesTheOrderTheManifestUses() {
        assertEquals(0, ATLAS.index(0, 0));
        assertEquals(3, ATLAS.index(3, 0));
        assertEquals(7, ATLAS.index(0, 1));
        assertEquals(48, ATLAS.index(6, 6));

        for (int index = 0; index < ATLAS.tileCount(); index++) {
            assertEquals(index, ATLAS.index(ATLAS.tileX(index), ATLAS.tileZ(index)));
        }
    }

    @Test
    void centreOfTheIslandLandsInTheMiddleTile() {
        int middle = ATLAS.tilesPerSide() / 2;
        assertEquals(middle, (int) (ATLAS.pixelX(0) / ATLAS.tilePixels()));
        assertEquals(middle, (int) (ATLAS.pixelZ(0) / ATLAS.tilePixels()));
    }

    @Test
    void mapsBlocksToPixels() {
        assertEquals(0.0, ATLAS.pixelX(-1792));
        assertEquals(896.0, ATLAS.pixelX(0));
        assertEquals(1792.0, ATLAS.pixelX(1792));
        assertEquals(ATLAS.sidePixels(), 1792);
        assertEquals(0.5, ATLAS.pixelZ(-1791));
    }

    @Test
    void knowsWhatIsOffTheGrid() {
        assertTrue(ATLAS.contains(0, 0));
        assertTrue(ATLAS.contains(6, 6));
        assertFalse(ATLAS.contains(7, 0));
        assertFalse(ATLAS.contains(-1, 0));
        assertFalse(ATLAS.contains(0, 7));
    }
}

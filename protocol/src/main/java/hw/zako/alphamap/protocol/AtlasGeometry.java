package hw.zako.alphamap.protocol;

public record AtlasGeometry(
        long id,
        int originX,
        int originZ,
        int blocksPerPixel,
        int tilePixels,
        int tilesPerSide,
        int radius) {

    public int tileCount() {
        return tilesPerSide * tilesPerSide;
    }

    public int index(int tileX, int tileZ) {
        return tileZ * tilesPerSide + tileX;
    }

    public int tileX(int index) {
        return index % tilesPerSide;
    }

    public int tileZ(int index) {
        return index / tilesPerSide;
    }

    public boolean contains(int tileX, int tileZ) {
        return tileX >= 0 && tileX < tilesPerSide && tileZ >= 0 && tileZ < tilesPerSide;
    }

    public int sidePixels() {
        return tilesPerSide * tilePixels;
    }

    /// Сторона видимого квадрата в пикселях атласа — то, что и надо показывать.
    public double visiblePixels() {
        return 2.0 * radius / blocksPerPixel;
    }

    /// Пиксель атласа, с которого видимый квадрат начинается. Центр мира — ноль, атлас симметричен.
    public double visibleOriginPixel() {
        return pixelX(-radius);
    }

    public double pixelX(double blockX) {
        return (blockX - originX) / (double) blocksPerPixel;
    }

    public double pixelZ(double blockZ) {
        return (blockZ - originZ) / (double) blocksPerPixel;
    }
}

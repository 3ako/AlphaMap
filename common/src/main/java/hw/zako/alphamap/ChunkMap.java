package hw.zako.alphamap;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class ChunkMap {

    public static final ChunkMap INSTANCE = new ChunkMap();

    static final int MISSING = 0;

    private static final int UNLOADED = 0x40000000;
    private static final int FALLBACK = 1;
    private static final int NO_FLOOR = Integer.MAX_VALUE;
    private static final int VOID_FLOOR = Integer.MAX_VALUE - 1;
    private static final int REACH_SAMPLES = 32;
    private static final int NO_DATA = Integer.MIN_VALUE;
    private static final int UNKNOWN_TEXTURE = -1;
    private static final int NO_SPRITE = 0;
    private static final int WHITE = 0xFFFFFF;
    private static final int UNIT = 128;
    private static final int TEXELS = 16;
    static final int MIP_LEVELS = 4;

    private static final int ABOVE_FEET = 2;
    private static final int CEILING_REACH = 2;
    private static final int Y_SLACK = 1;
    private static final int DEPTH = 24;
    private static final int WATER_DIG = 24;
    private static final double WALL_SHADE = 0.4;
    private static final int SWEEP_ABOVE = 1024;
    private static final long GAP_REFRESH_MILLIS = 1000;
    private static final long SWEEP_MILLIS = 5000;
    private static final long BUDGET_NANOS = 2_000_000;

    Long2ObjectOpenHashMap<Column> surfaceColumns = new Long2ObjectOpenHashMap<>();
    Long2ObjectOpenHashMap<Column> caveColumns = new Long2ObjectOpenHashMap<>();
    ArrayDeque<Column> spare = new ArrayDeque<>();
    Reference2IntOpenHashMap<BlockState> stateSprites = new Reference2IntOpenHashMap<>();
    Object2IntOpenHashMap<Identifier> spriteIndices = new Object2IntOpenHashMap<>();
    ArrayList<@Nullable Sprite> sprites = new ArrayList<>();
    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

    @NonFinal
    @Nullable ClientLevel level;
    @NonFinal
    boolean cave;
    @NonFinal
    int baseY;
    @NonFinal
    Long2ObjectOpenHashMap<Column> columns = surfaceColumns;
    @NonFinal
    int generation;
    @NonFinal
    int caveGeneration;
    @NonFinal
    long deadline;
    @NonFinal
    boolean pending;
    @NonFinal
    int revision;
    @NonFinal
    boolean gaps;
    @NonFinal
    long gapsAt;
    @NonFinal
    long sweptAt;
    @NonFinal
    @Nullable ClientLevel ceilingLevel;
    @NonFinal
    long ceilingAt;
    @NonFinal
    int ceilingRevision;
    @NonFinal
    boolean ceiling;
    @NonFinal
    @Nullable Column last;
    @NonFinal
    long lastKey;

    @NonFinal
    int lookSprite;
    @NonFinal
    int lookTint;
    @NonFinal
    int detailSprite;
    @NonFinal
    int detailModulate;
    @NonFinal
    int shownSprite;
    @NonFinal
    int shownModulate;

    private ChunkMap() {
        stateSprites.defaultReturnValue(UNKNOWN_TEXTURE);
        spriteIndices.defaultReturnValue(UNKNOWN_TEXTURE);
        sprites.add(null);
    }

    public static void blockChanged(ClientLevel level, BlockPos pos) {
        INSTANCE.invalidate(level, pos.getX(), pos.getZ());
    }

    boolean underground(ClientLevel level, BlockPos feet) {
        long at = feet.asLong();
        if (level == ceilingLevel && at == ceilingAt && revision == ceilingRevision) return ceiling;

        int x = feet.getX();
        int y = feet.getY();
        int z = feet.getZ();
        boolean covered = covered(level, x, y, z)
                && covered(level, x - CEILING_REACH, y, z) && covered(level, x + CEILING_REACH, y, z)
                && covered(level, x, y, z - CEILING_REACH) && covered(level, x, y, z + CEILING_REACH);

        ceilingLevel = level;
        ceilingAt = at;
        ceilingRevision = revision;
        ceiling = covered;
        return covered;
    }

    private static boolean covered(ClientLevel level, int blockX, int feetY, int blockZ) {
        LevelChunk chunk = level.getChunkSource().getChunk(blockX >> 4, blockZ >> 4, ChunkStatus.FULL, false);
        if (chunk == null) return false;

        LevelChunkSection[] sections = chunk.getSections();
        int from = feetY + ABOVE_FEET + 1;
        int x = blockX & 15;
        int z = blockZ & 15;
        for (int i = Math.max(0, chunk.getSectionIndex(from)); i <= chunk.getHighestFilledSectionIndex(); i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir()) continue;

            int base = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int dy = Math.max(0, from - base); dy < 16; dy++) {
                BlockState state = section.getBlockState(x, dy, z);
                if (!state.isAir() && state.blocksMotion()
                        && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)) return true;
            }
        }
        return false;
    }

    int revision() {
        return revision;
    }

    void follow(ClientLevel level, boolean cave, int feetY) {
        last = null;
        deadline = System.nanoTime() + BUDGET_NANOS;
        if (this.level != level) {
            this.level = level;
            release(surfaceColumns);
            release(caveColumns);
            baseY = feetY;
            caveGeneration++;
            this.cave = cave;
            use(cave);
        } else if (this.cave != cave) {
            this.cave = cave;
            use(cave);
        }

        if (cave && Math.abs(feetY - baseY) > Y_SLACK) {
            baseY = feetY;
            caveGeneration++;
            use(true);
        }

        if (pending) {
            pending = false;
            revision++;
        }

        long now = System.currentTimeMillis();
        if (gaps && now - gapsAt >= GAP_REFRESH_MILLIS) {
            gaps = false;
            gapsAt = now;
            revision++;
        }
        if (surfaceColumns.size() + caveColumns.size() > SWEEP_ABOVE && now - sweptAt >= SWEEP_MILLIS) {
            sweptAt = now;
            sweep(surfaceColumns);
            sweep(caveColumns);
        }
    }

    private void use(boolean cave) {
        columns = cave ? caveColumns : surfaceColumns;
        generation = cave ? caveGeneration : 0;
        last = null;
        revision++;
    }

    private void release(Long2ObjectOpenHashMap<Column> layer) {
        spare.addAll(layer.values());
        layer.clear();
    }

    private void sweep(Long2ObjectOpenHashMap<Column> layer) {
        ObjectIterator<Long2ObjectMap.Entry<Column>> it = layer.long2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            Long2ObjectMap.Entry<Column> entry = it.next();
            long key = entry.getLongKey();
            Column column = entry.getValue();
            if (loaded((int) (key >> 32), (int) key) != column.chunk) {
                spare.add(column);
                it.remove();
            }
        }
        last = null;
    }

    private void invalidate(ClientLevel level, int x, int z) {
        if (this.level != level) return;
        boolean touched = forget(surfaceColumns, 0, x, z);
        touched |= forget(surfaceColumns, 0, x, z + 1);
        touched |= forget(surfaceColumns, 0, x + 1, z);
        touched |= forget(caveColumns, caveGeneration, x, z);
        touched |= forget(caveColumns, caveGeneration, x, z + 1);
        touched |= forget(caveColumns, caveGeneration, x + 1, z);
        if (touched) revision++;
    }

    private boolean forget(Long2ObjectOpenHashMap<Column> layer, int layerGeneration, int x, int z) {
        Column column = layer.get(key(x >> 4, z >> 4));
        if (column == null || column.generation != layerGeneration) return false;
        int index = index(x, z);
        column.colours[index] = MISSING;
        column.floors[index] = NO_DATA;
        return true;
    }

    int colour(int x, int z) {
        Column column = column(x >> 4, z >> 4);
        if (column == null) {
            gaps = true;
            shownSprite = NO_SPRITE;
            return cave ? UNLOADED : MISSING;
        }

        int index = index(x, z);
        int colour = column.colours[index];
        if (colour == MISSING) {
            if (System.nanoTime() > deadline) {
                pending = true;
                shownSprite = NO_SPRITE;
                return cave ? UNLOADED : MISSING;
            }
            detailSprite = NO_SPRITE;
            colour = cave ? cave(column.chunk, x, z) : surface(column.chunk, x, z);
            column.colours[index] = colour;
            column.sprites[index] = detailSprite;
            column.modulates[index] = detailModulate;
        }
        shownSprite = column.sprites[index];
        shownModulate = column.modulates[index];
        return colour == FALLBACK ? MISSING : colour;
    }

    int @Nullable [] texels(int mip) {
        Sprite sprite = sprites.get(shownSprite);
        return sprite == null ? null : sprite.mips()[mip];
    }

    int modulate() {
        return shownModulate;
    }

    private @Nullable Column column(int chunkX, int chunkZ) {
        long key = key(chunkX, chunkZ);
        if (last != null && lastKey == key) return last;

        LevelChunk chunk = loaded(chunkX, chunkZ);
        if (chunk == null) return null;

        Column column = columns.get(key);
        if (column == null) {
            column = spare.isEmpty() ? new Column() : spare.poll();
            column.chunk = chunk;
            column.generation = generation - 1;
            columns.put(key, column);
        }
        if (column.chunk != chunk || column.generation != generation) {
            column.chunk = chunk;
            column.generation = generation;
            Arrays.fill(column.colours, MISSING);
            Arrays.fill(column.floors, NO_DATA);
        }

        last = column;
        lastKey = key;
        return column;
    }

    private @Nullable LevelChunk loaded(int chunkX, int chunkZ) {
        return level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
    }

    private int surface(LevelChunk chunk, int x, int z) {
        int y = top(chunk, x, z);
        if (y == NO_DATA) return FALLBACK;

        BlockState state = chunk.getBlockState(cursor.set(x, y, z));
        boolean water = state.getFluidState().is(FluidTags.WATER);
        int colour = water ? water(chunk, state, x, y, z) : look(state);
        int rise = rise(y, (z & 15) != 0 ? top(chunk, x, z - 1) : neighbourTop(x, z - 1))
                + rise(y, (x & 15) != 0 ? top(chunk, x - 1, z) : neighbourTop(x - 1, z));
        int shaded = finish(colour, 1.0 + Math.clamp(rise * 0.04, -0.12, 0.12));
        if (water) detailSprite = NO_SPRITE;
        return shaded;
    }

    private int neighbourTop(int x, int z) {
        Column column = column(x >> 4, z >> 4);
        return column == null ? NO_DATA : top(column.chunk, x, z);
    }

    private int top(LevelChunk chunk, int x, int z) {
        Column column = column(x >> 4, z >> 4);
        int index = index(x, z);
        int cached = column.floors[index];
        if (cached != NO_DATA) return cached;

        LevelChunkSection[] sections = chunk.getSections();
        int top = NO_DATA;
        search:
        for (int i = chunk.getHighestFilledSectionIndex(); i >= 0; i--) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir()) continue;

            int base = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int dy = 15; dy >= 0; dy--) {
                BlockState state = section.getBlockState(x & 15, dy, z & 15);
                if (state.isAir() || state.getMapColor(level, cursor.set(x, base + dy, z)) == MapColor.NONE) continue;
                top = base + dy;
                break search;
            }
        }
        if (top != NO_DATA) column.floors[index] = top;
        return top;
    }

    private int water(LevelChunk chunk, BlockState surface, int x, int y, int z) {
        int water = look(surface);
        int bottom = Math.max(level.getMinY(), y - WATER_DIG);
        for (int floor = y - 1; floor >= bottom; floor--) {
            BlockState state = chunk.getBlockState(cursor.set(x, floor, z));
            if (state.getFluidState().is(FluidTags.WATER) || state.getMapColor(level, cursor) == MapColor.NONE) continue;

            double opacity = Math.clamp(0.45 + (y - floor) * 0.06, 0.45, 0.92);
            return blend(look(state), water, opacity);
        }
        return water;
    }

    private static int blend(int under, int over, double opacity) {
        return (int) ((under >> 16 & 0xFF) * (1 - opacity) + (over >> 16 & 0xFF) * opacity) << 16
                | (int) ((under >> 8 & 0xFF) * (1 - opacity) + (over >> 8 & 0xFF) * opacity) << 8
                | (int) ((under & 0xFF) * (1 - opacity) + (over & 0xFF) * opacity);
    }

    private static int rise(int top, int neighbour) {
        return neighbour == NO_DATA ? 0 : top - neighbour;
    }

    private int cave(LevelChunk chunk, int x, int z) {
        int floor = floor(chunk, x, z);
        if (floor == VOID_FLOOR) return UNLOADED;
        if (floor == NO_FLOOR) {
            BlockState wall = chunk.getBlockState(cursor.set(x, baseY, z));
            return finish(look(wall), WALL_SHADE);
        }

        int north = northFloor(chunk, x, z);
        BlockState state = chunk.getBlockState(cursor.set(x, floor, z));
        double relief = north == NO_FLOOR || north == VOID_FLOOR ? 0.8 : north == NO_DATA ? 1.0 : slope(floor, north);
        double depth = Math.clamp(1.0 + (floor - (baseY - 1)) * 0.06, 0.45, 1.2);
        return finish(look(state), relief * depth);
    }

    private int northFloor(LevelChunk chunk, int x, int z) {
        if ((z & 15) != 0) return floor(chunk, x, z - 1);

        Column north = column(x >> 4, (z - 1) >> 4);
        return north == null ? NO_DATA : floor(north.chunk, x, z - 1);
    }

    private int floor(LevelChunk chunk, int x, int z) {
        Column column = column(x >> 4, z >> 4);
        int index = index(x, z);
        int cached = column.floors[index];
        if (cached != NO_DATA) return cached;

        int top = baseY + ABOVE_FEET;
        int bottom = Math.max(level.getMinY(), baseY - DEPTH);
        boolean open = false;
        int floor = NO_FLOOR;
        for (int y = top; y >= bottom; y--) {
            BlockState state = chunk.getBlockState(cursor.set(x, y, z));
            if (state.isAir() || state.getMapColor(level, cursor) == MapColor.NONE) {
                open = true;
            } else if (open) {
                floor = y;
                break;
            }
        }
        if (open && floor == NO_FLOOR) floor = VOID_FLOOR;
        column.floors[index] = floor;
        return floor;
    }

    int caveReach(int centreX, int centreZ, int limit) {
        int step = Math.max(2, limit / REACH_SAMPLES);
        int reach = 0;
        for (int dz = -limit; dz <= limit; dz += step) {
            for (int dx = -limit; dx <= limit; dx += step) {
                int distance = Math.max(Math.abs(dx), Math.abs(dz));
                if (distance <= reach) continue;

                int x = centreX + dx;
                int z = centreZ + dz;
                Column column = column(x >> 4, z >> 4);
                if (column == null) continue;
                int floor = floor(column.chunk, x, z);
                if (floor != NO_FLOOR && floor != VOID_FLOOR) reach = distance;
            }
        }
        return reach;
    }

    private static double slope(int y, int north) {
        return y > north ? 1.15 : y < north ? 0.85 : 1.0;
    }

    private int finish(int rgb, double shade) {
        detailSprite = lookSprite;
        double scale = shade * UNIT / 255;
        detailModulate = channel(lookTint >> 16 & 0xFF, scale) << 16
                | channel(lookTint >> 8 & 0xFF, scale) << 8
                | channel(lookTint & 0xFF, scale);
        return shaded(rgb, shade);
    }

    private int look(BlockState state) {
        int index = stateSprites.getInt(state);
        if (index == UNKNOWN_TEXTURE) {
            index = sprite(BlockLook.topSprite(state));
            stateSprites.put(state, index);
        }
        lookSprite = index;
        lookTint = WHITE;
        if (index == NO_SPRITE) return state.getMapColor(level, cursor).col;

        int texture = sprites.get(index).average();
        int tint = tint(state);
        if (tint == -1) return texture;
        lookTint = tint & WHITE;
        return multiply(texture >> 16 & 0xFF, tint >> 16 & 0xFF) << 16
                | multiply(texture >> 8 & 0xFF, tint >> 8 & 0xFF) << 8
                | multiply(texture & 0xFF, tint & 0xFF);
    }

    private int tint(BlockState state) {
        int y = cursor.getY();
        double cellX = (cursor.getX() - 1.5) / 4.0;
        double cellZ = (cursor.getZ() - 1.5) / 4.0;
        int left = (int) Math.floor(cellX);
        int top = (int) Math.floor(cellZ);
        double tx = cellX - left;
        double tz = cellZ - top;

        int northWest = cellTint(state, left, top, y);
        if (northWest == -1) return -1;
        int northEast = cellTint(state, left + 1, top, y);
        int southWest = cellTint(state, left, top + 1, y);
        int southEast = cellTint(state, left + 1, top + 1, y);
        return lerp(lerp(northWest, northEast, tx), lerp(southWest, southEast, tx), tz);
    }

    private int cellTint(BlockState state, int cellX, int cellZ, int y) {
        return BlockLook.tint(state, level, probe.set(cellX * 4 + 2, y, cellZ * 4 + 2));
    }

    private static int lerp(int a, int b, double t) {
        if (a == b) return a;
        return (int) ((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t) << 16
                | (int) ((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t) << 8
                | (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
    }

    private int sprite(@Nullable Identifier name) {
        if (name == null) return NO_SPRITE;
        int index = spriteIndices.getInt(name);
        if (index == UNKNOWN_TEXTURE) {
            Sprite sprite = load(name);
            if (sprite == null) {
                index = NO_SPRITE;
            } else {
                index = sprites.size();
                sprites.add(sprite);
            }
            spriteIndices.put(name, index);
        }
        return index;
    }

    private static int multiply(int a, int b) {
        return a * b / 255;
    }

    private static @Nullable Sprite load(Identifier sprite) {
        Identifier file = sprite.withPath(path -> "textures/" + path + ".png");
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) return null;

        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            int width = image.getWidth();
            int height = Math.min(image.getHeight(), width);
            int[] cell = new int[4];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) accumulate(cell, image.getPixel(x, y));
            }
            if (cell[3] == 0) return null;
            int average = mean(cell, 0);

            int[][] mips = new int[MIP_LEVELS][];
            mips[0] = new int[TEXELS * TEXELS];
            for (int ty = 0; ty < TEXELS; ty++) {
                for (int tx = 0; tx < TEXELS; tx++) {
                    Arrays.fill(cell, 0);
                    int fromX = tx * width / TEXELS;
                    int fromY = ty * height / TEXELS;
                    int toX = Math.max(fromX + 1, (tx + 1) * width / TEXELS);
                    int toY = Math.max(fromY + 1, (ty + 1) * height / TEXELS);
                    for (int y = fromY; y < toY; y++) {
                        for (int x = fromX; x < toX; x++) accumulate(cell, image.getPixel(x, y));
                    }
                    mips[0][ty * TEXELS + tx] = mean(cell, average);
                }
            }
            for (int level = 1; level < MIP_LEVELS; level++) {
                int side = TEXELS >> level;
                int[] above = mips[level - 1];
                int[] mip = new int[side * side];
                for (int y = 0; y < side; y++) {
                    for (int x = 0; x < side; x++) {
                        Arrays.fill(cell, 0);
                        int from = y * 2 * side * 2 + x * 2;
                        accumulate(cell, 0xFF000000 | above[from]);
                        accumulate(cell, 0xFF000000 | above[from + 1]);
                        accumulate(cell, 0xFF000000 | above[from + side * 2]);
                        accumulate(cell, 0xFF000000 | above[from + side * 2 + 1]);
                        mip[y * side + x] = mean(cell, average);
                    }
                }
                mips[level] = mip;
            }
            return new Sprite(average, mips);
        } catch (IOException e) {
            return null;
        }
    }

    private static void accumulate(int[] sum, int argb) {
        if ((argb >>> 24) < 128) return;
        sum[0] += argb >> 16 & 0xFF;
        sum[1] += argb >> 8 & 0xFF;
        sum[2] += argb & 0xFF;
        sum[3]++;
    }

    private static int mean(int[] sum, int fallback) {
        if (sum[3] == 0) return fallback;
        return sum[0] / sum[3] << 16 | sum[1] / sum[3] << 8 | sum[2] / sum[3];
    }

    private static int shaded(int rgb, double shade) {
        return 0xFF000000
                | channel(rgb >> 16 & 0xFF, shade) << 16
                | channel(rgb >> 8 & 0xFF, shade) << 8
                | channel(rgb & 0xFF, shade);
    }

    private static int channel(int value, double shade) {
        return (int) Math.min(255, value * shade);
    }

    private static int index(int x, int z) {
        return (z & 15) << 4 | (x & 15);
    }

    private static long key(int chunkX, int chunkZ) {
        return (long) chunkX << 32 | (chunkZ & 0xFFFFFFFFL);
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static final class Column {
        final int[] colours = new int[256];
        final int[] floors = new int[256];
        final int[] sprites = new int[256];
        final int[] modulates = new int[256];
        LevelChunk chunk;
        int generation;
    }

    private record Sprite(int average, int[][] mips) {
    }
}

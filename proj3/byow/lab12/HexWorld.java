package byow.lab12;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.util.Random;

/**
 * Draws a world consisting of hexagonal regions.
 */
public class HexWorld {
    private static final int SIZE = 3;           // side length of each hexagon
    private static final long SEED = 2873123;
    private static final Random RANDOM = new Random(SEED);

    /**
     * Adds a hexagon of side length s to the world at position (px, py).
     * (px, py) is the bottom-left corner of the hexagon's bounding box.
     *
     * @param world  the 2D tile array
     * @param px     x position (bottom-left of bounding box)
     * @param py     y position (bottom-left of bounding box)
     * @param s      side length of the hexagon
     * @param t      the tile to fill with
     */
    public static void addHexagon(TETile[][] world, int px, int py, int s, TETile t) {
        if (s < 2) {
            throw new IllegalArgumentException("Hexagon side length must be at least 2.");
        }
        for (int yi = 0; yi < 2 * s; yi++) {
            int startY = py + yi;
            int rowOffset = hexRowOffset(s, yi);
            int rowWidth = hexRowWidth(s, yi);
            int startX = px + rowOffset;
            for (int xi = 0; xi < rowWidth; xi++) {
                // Use colorVariant for a bit of visual variety
                world[startX + xi][startY] = TETile.colorVariant(t, 32, 32, 32, RANDOM);
            }
        }
    }

    /**
     * Computes the x-offset of row i of a size-s hexagon relative to
     * the bottom-left corner of the hexagon's bounding box.
     */
    private static int hexRowOffset(int s, int i) {
        int effectiveI = i;
        if (i >= s) {
            effectiveI = 2 * s - 1 - i;
        }
        return (s - 1) - effectiveI;
    }

    /**
     * Computes the width of row i of a size-s hexagon.
     * Row 0 is the bottom row; row 2s-1 is the top row.
     */
    private static int hexRowWidth(int s, int i) {
        int effectiveI = i;
        if (i >= s) {
            effectiveI = 2 * s - 1 - i;
        }
        return s + 2 * effectiveI;
    }

    /**
     * Picks a random biome tile from a predefined set.
     */
    private static TETile randomBiomeTile() {
        int tileNum = RANDOM.nextInt(7);
        switch (tileNum) {
            case 0: return Tileset.GRASS;
            case 1: return Tileset.FLOWER;
            case 2: return Tileset.SAND;
            case 3: return Tileset.MOUNTAIN;
            case 4: return Tileset.TREE;
            case 5: return Tileset.WATER;
            case 6: return Tileset.FLOOR;
            default: return Tileset.NOTHING;
        }
    }

    /**
     * Draws the full tessellation of 19 hexagons (5 columns: 3-4-5-4-3).
     *
     * For a hexagon of side length s:
     *   - bounding box width  = 3s - 2
     *   - bounding box height = 2s
     *   - horizontal step between adjacent column x-starts = 2s - 1
     *   - vertical offset per column change = s
     */
    public static void drawWorld(TETile[][] world, int s) {
        fillWithNothing(world);

        // Number of hexagons per column
        int[] colSizes = {3, 4, 5, 4, 3};

        // Bottom-left starting position with some padding
        int startX = s - 1;  // enough room for the first column's indent rows
        // The center column (5 hexes) is the tallest; other columns are offset up
        int bottomY = 0;

        for (int col = 0; col < 5; col++) {
            int numHexes = colSizes[col];
            // x position of this column
            int colX = startX + col * (2 * s - 1);
            // y offset: center column is lowest; each step away raises by s
            int colY = bottomY + (5 - numHexes) * s;
            drawHexColumn(world, colX, colY, s, numHexes);
        }
    }

    /**
     * Fills the world with NOTHING tiles.
     */
    private static void fillWithNothing(TETile[][] world) {
        int width = world.length;
        int height = world[0].length;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                world[x][y] = Tileset.NOTHING;
            }
        }
    }

    /**
     * Draws a column of numHexes hexagons, each of side length s,
     * starting from position (startX, startY) — bottom-left of the
     * lowest hexagon in the column.
     */
    private static void drawHexColumn(TETile[][] world, int startX, int startY,
                                      int s, int numHexes) {
        for (int i = 0; i < numHexes; i++) {
            int yPos = startY + i * (2 * s);
            TETile t = randomBiomeTile();
            addHexagon(world, startX, yPos, s, t);
        }
    }

    /**
     * Computes the minimum world width needed for the tessellation.
     * 5 columns: first at x = (s-1), each spaced (2s-1) apart,
     * last column's widest row = 3s-2.
     */
    private static int worldWidth(int s) {
        return (s - 1) + 4 * (2 * s - 1) + (3 * s - 2) + 1;
    }

    /**
     * Computes the minimum world height needed.
     * Tallest column has 5 hexagons, each 2s tall.
     */
    private static int worldHeight(int s) {
        return 5 * 2 * s;
    }

    public static void main(String[] args) {
        int size = SIZE;

        int width = worldWidth(size);
        int height = worldHeight(size);

        TERenderer ter = new TERenderer();
        ter.initialize(width, height);

        TETile[][] world = new TETile[width][height];
        drawWorld(world, size);

        ter.renderFrame(world);
    }
}

package byow.Core;

import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.io.Serializable;
import java.util.Random;

/**
 * L 形走廊：连接两个 Position（通常是两个房间的中心）。
 *
 * 实现策略：随机选择「先水平后垂直」或「先垂直后水平」。
 * 走廊本身宽度为 1 格 FLOOR，两侧补 WALL。
 */
public class Hallway implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Position from;
    private final Position to;
    private final boolean horizontalFirst;

    public Hallway(Position from, Position to, Random rand) {
        this.from = from;
        this.to = to;
        this.horizontalFirst = rand.nextBoolean();
    }

    public void drawHallway(TETile[][] world) {
        if (horizontalFirst) {
            drawHorizontal(world, from.x, to.x, from.y);
            drawVertical(world, from.y, to.y, to.x);
        } else {
            drawVertical(world, from.y, to.y, from.x);
            drawHorizontal(world, from.x, to.x, to.y);
        }
    }

    /** 在固定 y 上从 x1 到 x2 铺一条水平走廊。 */
    private static void drawHorizontal(TETile[][] world, int x1, int x2, int y) {
        int lo = Math.min(x1, x2);
        int hi = Math.max(x1, x2);
        for (int x = lo; x <= hi; x++) {
            placeFloor(world, x, y);
            placeWallIfEmpty(world, x, y - 1);
            placeWallIfEmpty(world, x, y + 1);
        }
        // 端点上下也要封口
        placeWallIfEmpty(world, lo - 1, y);
        placeWallIfEmpty(world, hi + 1, y);
    }

    /** 在固定 x 上从 y1 到 y2 铺一条垂直走廊。 */
    private static void drawVertical(TETile[][] world, int y1, int y2, int x) {
        int lo = Math.min(y1, y2);
        int hi = Math.max(y1, y2);
        for (int y = lo; y <= hi; y++) {
            placeFloor(world, x, y);
            placeWallIfEmpty(world, x - 1, y);
            placeWallIfEmpty(world, x + 1, y);
        }
        placeWallIfEmpty(world, x, lo - 1);
        placeWallIfEmpty(world, x, hi + 1);
    }

    private static void placeFloor(TETile[][] world, int x, int y) {
        if (inBounds(world, x, y)) {
            world[x][y] = Tileset.FLOOR;
        }
    }

    private static void placeWallIfEmpty(TETile[][] world, int x, int y) {
        if (!inBounds(world, x, y)) return;
        if (world[x][y] != Tileset.FLOOR) {
            world[x][y] = Tileset.WALL;
        }
    }

    private static boolean inBounds(TETile[][] world, int x, int y) {
        return x >= 0 && y >= 0 && x < world.length && y < world[0].length;
    }
}


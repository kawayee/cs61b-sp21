package byow.Core;

import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.io.Serializable;
import java.util.Random;

/**
 * 矩形房间。由左下角坐标与宽高描述。
 *
 * 房间所占据的「内部」范围 (FLOOR) 为：
 *   x ∈ [bottomLeft.x, topRight.x],  y ∈ [bottomLeft.y, topRight.y]
 * 房间的「包围墙」 (WALL) 则向外扩张 1 格。
 */
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Position bottomLeft;
    private final Position topRight;
    private final int width;
    private final int height;
    private final TETile floorType;   // 房间的地面类型：FLOOR / GRASS / SAND 等

    public Room(Position bottomLeft, int width, int height, TETile floorType) {
        this.bottomLeft = bottomLeft;
        this.width = width;
        this.height = height;
        this.topRight = new Position(bottomLeft.x + width - 1, bottomLeft.y + height - 1);
        this.floorType = floorType;
    }

    /**
     * 判定两个房间是否重叠（含外层墙体的 1 格缓冲）。
     * 用「矩形相离」的反向：只要在任一方向上完全分开就不重叠。
     */
    public boolean overlaps(Room other) {
        return !(this.topRight.x + 1 < other.bottomLeft.x - 1
                || this.bottomLeft.x - 1 > other.topRight.x + 1
                || this.topRight.y + 1 < other.bottomLeft.y - 1
                || this.bottomLeft.y - 1 > other.topRight.y + 1);
    }

    /** 房间（含外层墙）是否完全在世界边界内。 */
    public boolean inBounds(int worldWidth, int worldHeight) {
        return bottomLeft.x - 1 >= 0
                && bottomLeft.y - 1 >= 0
                && topRight.x + 1 < worldWidth
                && topRight.y + 1 < worldHeight;
    }

    /**
     * 把房间画到世界中：
     * 1) 内部填 FLOOR；
     * 2) 四周填 WALL（如果当前不是 FLOOR，避免覆盖走廊接入口）。
     */
    public void drawRoom(TETile[][] world) {
        for (int x = bottomLeft.x; x <= topRight.x; x++) {
            for (int y = bottomLeft.y; y <= topRight.y; y++) {
                world[x][y] = Tileset.FLOOR;
            }
        }
        for (int x = bottomLeft.x - 1; x <= topRight.x + 1; x++) {
            placeWallIfEmpty(world, x, bottomLeft.y - 1);
            placeWallIfEmpty(world, x, topRight.y + 1);
        }
        for (int y = bottomLeft.y - 1; y <= topRight.y + 1; y++) {
            placeWallIfEmpty(world, bottomLeft.x - 1, y);
            placeWallIfEmpty(world, topRight.x + 1, y);
        }
    }

    private static void placeWallIfEmpty(TETile[][] world, int x, int y) {
        if (x < 0 || y < 0 || x >= world.length || y >= world[0].length) {
            return;
        }
        if (world[x][y] != Tileset.FLOOR) {
            world[x][y] = Tileset.WALL;
        }
    }

    /** 返回房间中心坐标，用于连接走廊。 */
    public Position center() {
        return new Position(
                (bottomLeft.x + topRight.x) / 2,
                (bottomLeft.y + topRight.y) / 2
        );
    }

    /** 返回房间内任意一格的坐标（保证是 FLOOR）。 */
    public Position randomFloor(Random rand) {
        int rx = bottomLeft.x + rand.nextInt(width);
        int ry = bottomLeft.y + rand.nextInt(height);
        return new Position(rx, ry);
    }

    /** 返回房间四边墙上内任意一格的坐标（保证是 WALL，不能是四个角）。 */
    public Position randomWall(Random rand) {
        int rx = bottomLeft.x;
        int ry = bottomLeft.y;
        int direction = rand.nextInt(4);
        switch (direction){
            case 0:
                rx = rx + rand.nextInt(width);
                ry = ry - 1; break;
            case 1:
                rx = rx + width;
                ry = ry + rand.nextInt(height); break;
            case 2:
                rx = rx + rand.nextInt(width);
                ry = ry + height; break;
            case 3:
                rx = rx - 1;
                ry = ry + rand.nextInt(height);
        }
        return new Position(rx, ry);
    }

    /** 判断一个 tile 是否属于「地面类」（不应被墙覆盖）。 */
    public static boolean isFloorLike(TETile t) {
        return t == Tileset.FLOOR || t == Tileset.GRASS
                || t == Tileset.SAND  || t == Tileset.FLOWER;
    }

    public Position getBottomLeft() { return bottomLeft; }
    public Position getTopRight()   { return topRight; }
    public int getWidth()           { return width; }
    public int getHeight()          { return height; }
    public TETile getFloorType()    { return floorType; }
}


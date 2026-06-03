package byow.Core;

import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 世界生成器（Phase 1 核心）。
 *
 * 功能：
 *   1. 生成 N 个互不重叠的房间，每个房间随机分配地形（FLOOR / GRASS / SAND）。
 *   2. 按 x 坐标排序后用 L 形走廊顺序连接，保证连通性。
 *   3. 在地面上随机散布装饰性 FLOWER 瓦片。
 *   4. 放置一把 KEY 和一扇 LOCKED_DOOR（在不同房间）。
 *
 * 决定论保证：整个过程只使用一个 Random，不依赖任何外部状态。
 */
public class WorldGenerator implements Serializable {
    private static final long serialVersionUID = 1L;

    // ---- 世界尺寸 ----
    public static final int WIDTH  = 80;
    public static final int HEIGHT = 30;
    public static final int HUD_HEIGHT = 2;

    // ---- 房间生成参数 ----
    private static final int MIN_ROOMS = 12;
    private static final int MAX_ROOMS = 22;
    private static final int MIN_ROOM_W = 4;
    private static final int MAX_ROOM_W = 9;
    private static final int MIN_ROOM_H = 4;
    private static final int MAX_ROOM_H = 7;
    private static final int MAX_ATTEMPTS = 500;

    // ---- 装饰参数 ----
    private static final double FLOWER_PROBABILITY = 0.04;



    // ---- 地形调色板（随机为房间选择） ----
    private static final TETile[] TERRAIN_PALETTE = {
            Tileset.FLOOR, Tileset.FLOOR, Tileset.GRASS, Tileset.SAND
    };

    // ---- 实例字段 ----
    private final long seed;
    private final Random rand;
    private final TETile[][] world;
    private final List<Room> rooms = new ArrayList<>();
    private Position keyPosition;
    private Position doorPosition;

    public WorldGenerator(long seed) {
        this.seed = seed;
        this.rand = new Random(seed);
        this.world = new TETile[WIDTH][HEIGHT];
    }

    // ==================================================================
    //  公共 API
    // ==================================================================

    /** 生成整个世界并返回 TETile[][]。 */
    public TETile[][] generate() {
        fillWithNothing();
        generateRooms();
        drawAllRooms();
        connectRooms();
        scatterFlowers();
        placeKeyAndDoor();
        return world;
    }

    // ==================================================================
    //  Step 1：初始化
    // ==================================================================

    private void fillWithNothing() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                world[x][y] = Tileset.NOTHING;
            }
        }
    }

    // ==================================================================
    //  Step 2：随机生成房间
    // ==================================================================

    private void generateRooms() {
        int target = MIN_ROOMS + rand.nextInt(MAX_ROOMS - MIN_ROOMS + 1);
        int attempts = 0;
        while (rooms.size() < target && attempts < MAX_ATTEMPTS) {
            attempts++;
            int w = MIN_ROOM_W + rand.nextInt(MAX_ROOM_W - MIN_ROOM_W + 1);
            int h = MIN_ROOM_H + rand.nextInt(MAX_ROOM_H - MIN_ROOM_H + 1);
            int x = 1 + rand.nextInt(Math.max(1, WIDTH - w - 2));
            int y = 1 + HUD_HEIGHT + rand.nextInt(Math.max(1, HEIGHT - h - 2 - HUD_HEIGHT));

            // 随机选择地形
            TETile terrain = TERRAIN_PALETTE[rand.nextInt(TERRAIN_PALETTE.length)];
            Room candidate = new Room(new Position(x, y), w, h, terrain);

            if (!candidate.inBounds(WIDTH, HEIGHT)) continue;
            if (overlapsAny(candidate)) continue;
            rooms.add(candidate);
        }
    }

    private boolean overlapsAny(Room candidate) {
        for (Room r : rooms) {
            if (r.overlaps(candidate)) return true;
        }
        return false;
    }

    // ==================================================================
    //  Step 3：绘制房间
    // ==================================================================

    private void drawAllRooms() {
        for (Room r : rooms) {
            r.drawRoom(world);
        }
    }

    // ==================================================================
    //  Step 4：用走廊连接（按 x 排序后顺序连接）
    // ==================================================================

    private void connectRooms() {
        List<Room> sorted = new ArrayList<>(rooms);
        sorted.sort(Comparator
                .comparingInt((Room r) -> r.center().x)
                .thenComparingInt(r -> r.center().y));

        for (int i = 0; i + 1 < sorted.size(); i++) {
            Position a = sorted.get(i).center();
            Position b = sorted.get(i + 1).center();
            new Hallway(a, b, rand).drawHallway(world);
        }
    }

    // ==================================================================
    //  Step 5：散布装饰性 FLOWER
    // ==================================================================

    private void scatterFlowers() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (Room.isFloorLike(world[x][y]) && rand.nextDouble() < FLOWER_PROBABILITY) {
                    world[x][y] = Tileset.FLOWER;
                }
            }
        }
    }

    // ==================================================================
    //  Step 6：放置 KEY 和 LOCKED_DOOR
    // ==================================================================

    private void placeKeyAndDoor() {
        if (rooms.size() < 2) return;

        // 在两个不同的房间中分别放置 key 和 door
        int keyRoomIdx  = rand.nextInt(rooms.size());
        int doorRoomIdx = (keyRoomIdx + 1 + rand.nextInt(rooms.size() - 1)) % rooms.size();

        Room keyRoom  = rooms.get(keyRoomIdx);
        Room doorRoom = rooms.get(doorRoomIdx);

        keyPosition  = keyRoom.randomFloor(rand);
        doorPosition = doorRoom.randomWall(rand);

        world[keyPosition.x][keyPosition.y]   = Tileset.KEY_TILE;
        world[doorPosition.x][doorPosition.y]  = Tileset.LOCKED_DOOR;
    }

    // ==================================================================
    //  Getters
    // ==================================================================

    public TETile[][] getWorld()       { return world; }
    public List<Room> getRooms()       { return rooms; }
    public long getSeed()              { return seed; }
    public Random getRandom()          { return rand; }
    public Position getKeyPosition()   { return keyPosition; }
    public Position getDoorPosition()  { return doorPosition; }
}


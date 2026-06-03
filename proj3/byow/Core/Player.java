package byow.Core;

import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.io.Serializable;

/**
 * 玩家角色。
 *
 * 维护：
 *   - 位置 (Position)
 *   - 下层瓦片 (underTile)：离开格子时恢复原始瓦片
 *   - 生命值 (health)
 *   - 分数 (score)
 *   - 步数 / 回合数 (steps)
 *   - 是否持有钥匙 (hasKey)
 *
 * 交互逻辑：
 *   - 移动到 KEY_TILE 上 → 拾取钥匙，hasKey = true, score += 1
 *   - 移动到 LOCKED_DOOR 上 → 如果 hasKey，开门（变 UNLOCKED_DOOR），score += 5
 *   - 移动到 WALL / NOTHING → 不可通行
 */
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final int INITIAL_HEALTH = 5;

    private Position pos;
    private TETile underTile;
    private int health;
    private int score;
    private int steps;
    private boolean hasKey;
    private String lastMessage;   // 提示信息（HUD 右侧显示）

    public Player(Position start, TETile[][] world) {
        this.pos = start;
        this.underTile = world[start.x][start.y];
        this.health = INITIAL_HEALTH;
        this.score = 0;
        this.steps = 0;
        this.hasKey = false;
        this.lastMessage = "Find the key, then open the door!";
        world[start.x][start.y] = Tileset.AVATAR;
    }

    /**
     * 尝试向指定方向移动一格。
     *
     * @return true 如果成功移动
     */
    public boolean move(char dir, TETile[][] world) {
        int dx = 0, dy = 0;
        switch (Character.toUpperCase(dir)) {
            case 'W': dy = 1;  break;
            case 'S': dy = -1; break;
            case 'A': dx = -1; break;
            case 'D': dx = 1;  break;
            default:  return false;
        }

        int nx = pos.x + dx;
        int ny = pos.y + dy;

        if (!inBounds(world, nx, ny)) return false;

        TETile target = world[nx][ny];

        // ---- 判断能否通行 ----
        if (target == Tileset.WALL || target == Tileset.NOTHING) {
            return false;
        }

        // LOCKED_DOOR 特殊处理
        if (target == Tileset.LOCKED_DOOR) {
            if (!hasKey) {
                lastMessage = "You need a key to open this door!";
                return false;
            }
            // 开门：把门变成 UNLOCKED_DOOR
            world[nx][ny] = Tileset.UNLOCKED_DOOR;
            score += 5;
            lastMessage = "Door unlocked! You win!";
            // 注意：此处把 door 位置的 tile 设为 UNLOCKED_DOOR，
            // 下面走上去后 underTile 就是 UNLOCKED_DOOR
        }

        // ---- 执行移动 ----
        world[pos.x][pos.y] = underTile;           // 恢复原位
        underTile = world[nx][ny];                   // 记录新位置的下层
        world[nx][ny] = Tileset.AVATAR;              // 放上 avatar
        pos = new Position(nx, ny);
        steps++;

        // ---- 拾取钥匙 ----
        if (underTile == Tileset.KEY_TILE) {
            hasKey = true;
            score += 1;
            lastMessage = "You picked up the key!";
            underTile = Tileset.FLOOR;               // 拾取后地面变为普通 FLOOR
        }

        return true;
    }

    private static boolean inBounds(TETile[][] world, int x, int y) {
        return x >= 0 && y >= 0 && x < world.length && y < world[0].length;
    }

    // ---- Getters ----
    public Position getPosition() { return pos; }
    public TETile getUnderTile()  { return underTile; }
    public int getHealth()        { return health; }
    public int getScore()         { return score; }
    public int getSteps()         { return steps; }
    public boolean hasKey()       { return hasKey; }
    public String getLastMessage(){ return lastMessage; }

    // ---- Setters（供 Engine 或扩展用） ----
    public void setHealth(int h)  { this.health = h; }
    public void setLastMessage(String msg) { this.lastMessage = msg; }
}


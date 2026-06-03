package byow.Core;

import byow.TileEngine.TETile;

import java.io.Serializable;
import java.util.List;

/**
 * 封装一次游戏的全部可序列化状态。
 *
 * 需要持久化的内容：
 *   - seed:      生成世界的种子
 *   - world:     当前的 TETile[][]
 *   - player:    玩家位置、血量、分数、步数、是否持有钥匙
 *   - explored:  已经探索过的瓦片（用于 Line of Sight 记忆）
 *   - rooms:     房间列表（预留扩展）
 *
 * 存档时整个对象通过 Java 原生序列化写入磁盘，读档时反序列化即可完整恢复。
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long seed;
    private final TETile[][] world;
    private final Player player;
    private final boolean[][] explored;
    private final List<Room> rooms;

    public GameState(long seed, TETile[][] world, Player player,
                     boolean[][] explored, List<Room> rooms) {
        this.seed = seed;
        this.world = world;
        this.player = player;
        this.explored = explored;
        this.rooms = rooms;
    }

    // ---- Getters ----
    public long getSeed()              { return seed; }
    public TETile[][] getWorld()       { return world; }
    public Player getPlayer()          { return player; }
    public boolean[][] getExplored()   { return explored; }
    public List<Room> getRooms()       { return rooms; }
}


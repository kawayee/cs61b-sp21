package byow.Core;

import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

/**
 * Line of Sight（视野系统）。
 *
 * 算法：
 *   对视野半径内的每个瓦片，用 Bresenham 直线从玩家投射射线。
 *   如果射线沿途没有被 WALL 阻挡，则该瓦片「可见」。
 *   WALL 本身可见（否则玩家看不到墙），但 WALL 会阻挡后面的瓦片。
 *
 * 输出：
 *   - boolean[][] visible：当前帧可见的瓦片
 *   - 调用方负责更新 explored[][] 数组（visible 为 true 的格子标记为已探索）
 */
public final class LineOfSight {

    /** 默认视野半径。 */
    public static final int SIGHT_RADIUS = 7;

    private LineOfSight() { }

    /**
     * 计算从 playerPos 出发、半径 radius 内的可见瓦片。
     *
     * @param world     当前世界（用于判断哪些瓦片是墙）
     * @param playerPos 玩家位置
     * @param radius    视野半径
     * @return  与 world 同尺寸的 boolean[][]，true 表示可见
     */
    public static boolean[][] computeVisible(TETile[][] world, Position playerPos, int radius) {
        int w = world.length;
        int h = world[0].length;
        boolean[][] visible = new boolean[w][h];

        int px = playerPos.x;
        int py = playerPos.y;

        // 玩家自身始终可见
        visible[px][py] = true;

        // 遍历半径内的正方形区域
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int tx = px + dx;
                int ty = py + dy;
                if (tx < 0 || ty < 0 || tx >= w || ty >= h) continue;

                // 欧几里得距离过滤：只考虑圆形范围
                if (dx * dx + dy * dy > radius * radius) continue;

                // 从玩家向目标投射射线
                if (castRay(world, px, py, tx, ty)) {
                    visible[tx][ty] = true;
                }
            }
        }
        return visible;
    }

    /** 使用默认半径的便捷重载。 */
    public static boolean[][] computeVisible(TETile[][] world, Position playerPos) {
        return computeVisible(world, playerPos, SIGHT_RADIUS);
    }

    /**
     * 从 (x0, y0) 到 (x1, y1) 投射射线，判断目标是否可见。
     *
     * 使用 Bresenham 直线算法遍历路径上的每个格子。
     * 如果遇到 WALL，WALL 本身可见，但它后面的格子不可见。
     *
     * @return true 如果目标可见
     */
    private static boolean castRay(TETile[][] world, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx - dy;

        int cx = x0, cy = y0;

        while (true) {
            // 到达目标
            if (cx == x1 && cy == y1) {
                return true;
            }

            // 如果当前格是墙，后面的格子不可见（但墙自身在上一步已标记可见）
            if (cx != x0 || cy != y0) {
                // 非起点：检查是否是阻挡物
                if (isOpaque(world[cx][cy])) {
                    return false;
                }
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                cx += sx;
            }
            if (e2 < dx) {
                err += dx;
                cy += sy;
            }
        }
    }

    /**
     * 判断一个瓦片是否「不透光」（阻挡视线）。
     * WALL 阻挡视线；NOTHING 也阻挡；其余地面类和门不阻挡。
     */
    private static boolean isOpaque(TETile tile) {
        return tile == Tileset.WALL;
    }

    /**
     * 根据可见和已探索信息，更新 explored 数组。
     * 所有当前可见的格子标记为「已探索」。
     */
    public static void updateExplored(boolean[][] explored, boolean[][] visible) {
        for (int x = 0; x < visible.length; x++) {
            for (int y = 0; y < visible[0].length; y++) {
                if (visible[x][y]) {
                    explored[x][y] = true;
                }
            }
        }
    }
}


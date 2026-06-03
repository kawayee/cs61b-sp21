package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import edu.princeton.cs.introcs.StdDraw;

import java.awt.Color;
import java.awt.Font;
import java.time.LocalDate;

/**
 * Phase 2 主引擎。
 *
 * 两个入口：
 *   - interactWithKeyboard()      ：基于 StdDraw 的真实键盘交互
 *   - interactWithInputString(s)  ：模拟键盘的字符串输入（grader 用）
 *
 * 功能：
 *   - 主菜单（N / L / Q）
 *   - 种子输入（实时回显）
 *   - 游戏循环（WASD 移动）
 *   - HUD（左：tile/steps/score/health/key；右：seed/日期/提示信息）
 *   - Line of Sight 视野系统
 *   - 存档 (:Q) / 读档 (L)
 *   - Key/Door 交互
 */
public class Engine {

    public static final int WIDTH      = WorldGenerator.WIDTH;
    public static final int HEIGHT     = WorldGenerator.HEIGHT;
    public static final int HUD_HEIGHT = WorldGenerator.HUD_HEIGHT;

    private final TERenderer ter = new TERenderer();

    /** 当前游戏状态 */
    private GameState state;

    /** 是否处于 colon 模式（按了 : 等待 Q） */
    private boolean colonMode = false;

    /** 是否应该停止处理输入 */
    private boolean shouldStop = false;
    /** 用于存档的输入历史，例如 N12345SWWAAD */
    private StringBuilder inputHistory = new StringBuilder();

    // ===================================================================
    //  1. 两个公共入口
    // ===================================================================

    /** 键盘模式：渲染主菜单 → 游戏循环。 */
    public void interactWithKeyboard() {
        ter.initialize(WIDTH, HEIGHT);
        InputParser parser = new InputParser();  // 键盘模式
        mainMenuPhase(parser);
        if (state != null && !shouldStop) {
            keyboardGameLoop(parser);
        }
        System.exit(0);
    }

    /** 字符串模式：解析输入，返回最终世界状态（grader 用）。 */
    public TETile[][] interactWithInputString(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty.");
        }
        InputParser parser = new InputParser(input);
        mainMenuPhase(parser);
        if (state != null && !shouldStop) {
            stringGameLoop(parser);
        }
        return (state != null) ? state.getWorld() : null;
    }

    // ===================================================================
    //  2. 主菜单阶段
    // ===================================================================

    private void mainMenuPhase(InputParser parser) {
        if (!parser.isStringMode()) {
            drawMainMenu();
        }

        char choice = Character.toUpperCase(parser.next());

        switch (choice) {
            case 'N':
                long seed = readSeed(parser);
                state = buildNewGame(seed);
                // 新游戏开始时，记录可重放输入历史
                inputHistory = new StringBuilder("N" + seed + "S");
                break;
            case 'L':
                String savedInput = SaveLoad.load();
                if (savedInput == null || savedInput.isEmpty()) {
                    if (!parser.isStringMode()) {
                        drawCenteredMessage("No save file found.");
                        StdDraw.show();
                        StdDraw.pause(1500);
                    }
                    shouldStop = true;
                } else {
                    restoreFromInputHistory(savedInput);
                }
                break;
            case 'Q':
                shouldStop = true;
                break;
            default:
                shouldStop = true;
                break;
        }
    }

    /** 读取种子：逐字符积累数字，直到遇到 S。 */
    private long readSeed(InputParser parser) {
        StringBuilder buf = new StringBuilder();
        if (!parser.isStringMode()) {
            drawSeedPrompt(buf.toString());
        }
        while (parser.hasNext()) {
            char c = parser.next();
            if (c == 'S' || c == 's') {
                break;
            }
            if (Character.isDigit(c)) {
                buf.append(c);
                if (!parser.isStringMode()) {
                    drawSeedPrompt(buf.toString());
                }
            }
        }
        if (buf.length() == 0) {
            return 0L;
        }
        return Long.parseLong(buf.toString());
    }

    // ===================================================================
    //  3. 游戏循环
    // ===================================================================

    /** 键盘模式游戏循环：渲染 → 等输入 → 处理 → 重复。 */
    private void keyboardGameLoop(InputParser parser) {
        while (!shouldStop) {
            renderFrameWithLOS();
            // 非阻塞检测按键
            if (!parser.hasNextNonBlocking()) {
                StdDraw.pause(16);   // ~60fps 节省 CPU
                continue;
            }
            char c = parser.next();
            handleGameChar(c);
        }
    }

    /** 字符串模式游戏循环：顺序消费剩余字符。 */
    private void stringGameLoop(InputParser parser) {
        while (parser.hasNext() && !shouldStop) {
            char c = parser.next();
            handleGameChar(c);
        }
    }

    /**
     * 根据存档中的输入历史重建游戏状态。
     *
     * 例如 savedInput = "N12345SWWAAD"
     * 就等价于重新跑一遍 interactWithInputString("N12345SWWAAD")，
     * 但不会打开 GUI。
     */
    private void restoreFromInputHistory(String savedInput) {
        state = null;
        colonMode = false;
        shouldStop = false;
        inputHistory = new StringBuilder();

        InputParser replayParser = new InputParser(savedInput);

        mainMenuPhase(replayParser);

        if (state != null && !shouldStop) {
            stringGameLoop(replayParser);
        }

        // 确保存档历史保持为文件里的内容
        inputHistory = new StringBuilder(savedInput);
        colonMode = false;
        shouldStop = false;
    }

    /**
     * 处理游戏中的单个字符。
     *
     * 支持两种状态：
     *   - normal mode：WASD 移动 / ':' 进入 colon mode
     *   - colon mode：Q 保存并退出 / 其他取消 colon mode
     */
    private void handleGameChar(char c) {
        char up = Character.toUpperCase(c);

        if (colonMode) {
            if (up == 'Q') {
                SaveLoad.save(inputHistory.toString());
                shouldStop = true;
            }
            colonMode = false;
            return;
        }

        switch (up) {
            case 'W': case 'A': case 'S': case 'D':
                state.getPlayer().move(up, state.getWorld());
                // 记录移动输入，用于之后重放恢复
                inputHistory.append(up);
                break;
            case ':':
                colonMode = true;
                break;
            default:
                // 忽略其它输入
                break;
        }
    }

    // ===================================================================
    //  4. 新游戏构建
    // ===================================================================

    private GameState buildNewGame(long seed) {
        WorldGenerator gen = new WorldGenerator(seed);
        TETile[][] world = gen.generate();

        // 用同一个 Random 选玩家出生点，保持决定论
        java.util.Random rand = gen.getRandom();
        Room startRoom = gen.getRooms().get(rand.nextInt(gen.getRooms().size()));
        Position startPos = startRoom.randomFloor(rand);

        Player player = new Player(startPos, world);
        boolean[][] explored = new boolean[WIDTH][HEIGHT];

        return new GameState(seed, world, player, explored, gen.getRooms());
    }

    // ===================================================================
    //  5. 渲染（Line of Sight + HUD）
    // ===================================================================

    /**
     * 完整渲染一帧：
     *   1) 计算当前视野（Line of Sight）
     *   2) 更新已探索区域
     *   3) 构建显示用世界（可见 / 已探索暗色 / 未探索黑色）
     *   4) 用 TERenderer 渲染
     *   5) 叠加 HUD 文字
     */
    private void renderFrameWithLOS() {
        TETile[][] world = state.getWorld();
        Player player = state.getPlayer();
        boolean[][] explored = state.getExplored();

        // 计算视野
        boolean[][] visible = LineOfSight.computeVisible(world, player.getPosition());
        LineOfSight.updateExplored(explored, visible);

        // 构建显示世界
        TETile[][] display = buildDisplay(world, visible, explored);

        ter.renderFrame(display);
        drawHUD(visible);
        StdDraw.show();
    }

    /**
     * 构建用于显示的 TETile[][] 数组：
     *   - 可见：原始瓦片
     *   - 已探索但不可见：暗色版本
     *   - 未探索：NOTHING
     */
    private TETile[][] buildDisplay(TETile[][] world, boolean[][] visible, boolean[][] explored) {
        TETile[][] display = new TETile[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (visible[x][y]) {
                    display[x][y] = world[x][y];
                } else if (explored[x][y]) {
                    display[x][y] = dimTile(world[x][y]);
                } else {
                    display[x][y] = Tileset.NOTHING;
                }
            }
        }
        return display;
    }

    /** 创建一个瓦片的「暗色」版本（已探索但不在当前视野中）。 */
    private static TETile dimTile(TETile tile) {
        if (tile == Tileset.NOTHING) return Tileset.NOTHING;
        return new TETile(tile.character(),
                new Color(70, 70, 70),
                new Color(12, 12, 12),
                tile.description());
    }

    // ===================================================================
    //  6. HUD 绘制
    // ===================================================================

    /**
     * 在底部 2 行绘制 HUD。
     *
     * 第 1 行 (上)：
     *   左 → Tile: <鼠标悬停描述>
     *   右 → Seed: <种子>    <当前日期>
     *
     * 第 2 行 (下)：
     *   左 → Steps: <n>   Score: <n>   HP: <n>   Key: ✓/✗
     *   右 → <提示信息>
     */
    private void drawHUD(boolean[][] visible) {
        Player player = state.getPlayer();

        // 背景条
        StdDraw.setPenColor(new Color(20, 20, 20));
        StdDraw.filledRectangle(WIDTH / 2.0, 0.75, WIDTH / 2.0 + 1, 1.0);

        // 字体
        Font hudFont = new Font("Monaco", Font.PLAIN, 14);
        StdDraw.setFont(hudFont);
        StdDraw.setPenColor(Color.WHITE);

        // ---- 第 1 行 (y ≈ 1.2) ----
        // 左：鼠标悬停瓦片描述
        String tileDesc = getMouseTileDescription(visible);
        StdDraw.textLeft(1.0, 1.2, "Tile: " + tileDesc);

        // 右：种子 + 日期
        String dateStr = LocalDate.now().toString();
        String seedInfo = "Seed: " + state.getSeed() + "    " + dateStr;
        StdDraw.textRight(WIDTH - 1.0, 1.2, seedInfo);

        // ---- 第 2 行 (y ≈ 0.4) ----
        // 左：步数、分数、血量、钥匙
        String keySymbol = player.hasKey() ? "\u2713" : "\u2717";
        String stats = String.format("Steps: %d   Score: %d   HP: %d   Key: %s",
                player.getSteps(), player.getScore(), player.getHealth(), keySymbol);
        StdDraw.textLeft(1.0, 0.4, stats);

        // 右：提示信息
        StdDraw.setPenColor(new Color(255, 220, 100));
        StdDraw.textRight(WIDTH - 1.0, 0.4, player.getLastMessage());
    }

    /** 获取鼠标悬停位置的瓦片描述。 */
    private String getMouseTileDescription(boolean[][] visible) {
        int mx = (int) Math.floor(StdDraw.mouseX());
        int my = (int) Math.floor(StdDraw.mouseY());
        if (mx < 0 || my < 0 || mx >= WIDTH || my >= HEIGHT) {
            return "out of bounds";
        }
        if (!visible[mx][my]) {
            return "unknown";
        }
        return state.getWorld()[mx][my].description();
    }

    // ===================================================================
    //  7. 主菜单 UI
    // ===================================================================

    private void drawMainMenu() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);

        Font title = new Font("Monaco", Font.BOLD, 40);
        StdDraw.setFont(title);
        StdDraw.text(WIDTH / 2.0, HEIGHT * 0.72, "CS61B: BYOW");

        Font subtitle = new Font("Monaco", Font.PLAIN, 18);
        StdDraw.setFont(subtitle);
        StdDraw.setPenColor(new Color(180, 180, 180));
        StdDraw.text(WIDTH / 2.0, HEIGHT * 0.62, "Build Your Own World");

        Font item = new Font("Monaco", Font.PLAIN, 22);
        StdDraw.setFont(item);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(WIDTH / 2.0, HEIGHT * 0.46, "New Game  (N)");
        StdDraw.text(WIDTH / 2.0, HEIGHT * 0.38, "Load Game (L)");
        StdDraw.text(WIDTH / 2.0, HEIGHT * 0.30, "Quit      (Q)");

        StdDraw.show();
    }

    private void drawSeedPrompt(String soFar) {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);
        Font f = new Font("Monaco", Font.PLAIN, 24);
        StdDraw.setFont(f);
        StdDraw.text(WIDTH / 2.0, HEIGHT * 0.60, "Enter seed, then press S:");
        StdDraw.setPenColor(new Color(100, 255, 100));
        StdDraw.text(WIDTH / 2.0, HEIGHT * 0.48, soFar.isEmpty() ? "_" : soFar);
        StdDraw.show();
    }

    private void drawCenteredMessage(String msg) {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);
        Font f = new Font("Monaco", Font.PLAIN, 22);
        StdDraw.setFont(f);
        StdDraw.text(WIDTH / 2.0, HEIGHT / 2.0, msg);
    }
}


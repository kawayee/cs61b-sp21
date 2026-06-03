package byow.Core;

import edu.princeton.cs.introcs.StdDraw;

/**
 * InputParser 负责解析 BYOW 的输入字符串。
 *
 * 支持的输入格式包括：
 *
 * 1. 新游戏：
 *    N12345S
 *    n12345s
 *
 * 2. 新游戏 + 移动：
 *    N12345SWWAAD
 *
 * 3. 新游戏 + 保存退出：
 *    N12345SWWAAD:Q
 *
 * 4. 加载游戏：
 *    L
 *    l
 *
 * 5. 加载游戏 + 移动：
 *    LWWAAD
 *
 * 6. 加载游戏 + 保存退出：
 *    LWWAAD:Q
 *
 * InputParser 不应该调用 StdDraw，不应该修改 world，
 * 也不应该直接保存文件。它只负责把字符串解析成结构化信息。
 */
public class InputParser {
    private enum Mode { STRING, KEYBOARD }
    private final Mode mode;
    private final String input;

    private int index;      // 当前解析到的位置
    private boolean newGame;
    private boolean loadGame;
    private boolean saveAndQuit;
    private long seed;
    private String moves;       // 移动指令

    /** STRING 模式：从给定字符串读取。 */
    InputParser(String input){
        this.mode = Mode.STRING;
        this.input = input;
        this.index = 0;
        this.newGame = false;
        this.loadGame = false;
        this.saveAndQuit = false;
        this.seed = 0L;
        this.moves = "";
    }

    /** KEYBOARD 模式：通过 StdDraw 逐键读取。 */
    public InputParser() {
        this.mode = Mode.KEYBOARD;
        this.input = null;
        this.index = 0;
        this.newGame = false;
        this.loadGame = false;
        this.saveAndQuit = false;
        this.seed = 0L;
        this.moves = "";
    }

    /**
     * 是否还有更多输入。
     *
     * STRING 模式：index < input.length()。
     * KEYBOARD 模式：始终返回 true（由 Engine 决定何时退出循环）。
     */
    public boolean hasNext() {
        if (mode == Mode.STRING) {
            return index < input.length();
        }
        return true;   // 键盘模式下永远可以继续等待
    }

    /**
     * 读取下一个字符。
     *
     * STRING 模式：直接从字符串取。
     * KEYBOARD 模式：阻塞等待 StdDraw.nextKeyTyped()。
     */
    public char next() {
        if (mode == Mode.STRING) {
            if (index >= input.length()) {
                throw new IllegalStateException("No more input characters.");
            }
            return input.charAt(index++);
        }
        // KEYBOARD 模式：忙等待
        while (!StdDraw.hasNextKeyTyped()) {
            StdDraw.pause(20);
        }
        return StdDraw.nextKeyTyped();
    }

    /**
     * 非阻塞检测键盘是否有新按键（仅 KEYBOARD 模式有意义）。
     *
     * STRING 模式下始终返回 hasNext() 的值。
     */
    public boolean hasNextNonBlocking() {
        if (mode == Mode.STRING) {
            return hasNext();
        }
        return StdDraw.hasNextKeyTyped();
    }

    /** 是否是字符串模式。 */
    public boolean isStringMode() {
        return mode == Mode.STRING;
    }

    /** 返回当前已消费的字符数量（用于调试）。 */
    public int getConsumedCount() {
        return index;
    }
}

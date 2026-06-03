package byow.Core;

import java.io.*;
import java.util.Scanner;

/**
 * 存档与读档工具类。
 *
 * 使用 Java 原生序列化，将整个 GameState 写入 save-file.txt。
 * TETile 已实现 Serializable，所以 TETile[][] 可直接写出。
 *
 * 约束（来自课程要求）：
 *   - 只能创建 .txt 文件
 *   - :Q 保存并退出
 *   - L 加载上一次保存的世界，加载后必须与保存时完全一致
 */
public final class SaveLoad {

    private static final String SAVE_FILE = "save-file.txt";

    private SaveLoad() { }   // 工具类，禁止实例化

    /** 保存输入历史字符串。 */
    public static void save(String inputHistory) {
        try (FileWriter writer = new FileWriter(SAVE_FILE)) {
            writer.write(inputHistory);
        } catch (IOException e) {
            System.err.println("[SaveLoad] Save failed:");
            e.printStackTrace();
        }
    }

    /** 读取输入历史字符串。 */
    public static String load() {
        File f = new File(SAVE_FILE);
        if (!f.exists()) {
            return null;
        }

        try (Scanner scanner = new Scanner(f)) {
            if (!scanner.hasNextLine()) {
                return null;
            }
            return scanner.nextLine().trim();
        } catch (IOException e) {
            System.err.println("[SaveLoad] Load failed:");
            e.printStackTrace();
            return null;
        }
    }

    public static boolean saveExists() {
        return new File(SAVE_FILE).exists();
    }
}


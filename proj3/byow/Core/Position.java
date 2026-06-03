package byow.Core;

import java.io.Serializable;
import java.util.Objects;

/**
 * 二维坐标点，实现 Serializable 以支持存档。
 */
public class Position implements Serializable {
    private static final long serialVersionUID = 1L;

    public int x;
    public int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** 返回偏移后的新 Position（不修改自身）。 */
    public Position shift(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }

    /** 两点之间的欧几里得距离。 */
    public double distanceTo(Position other) {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position p = (Position) o;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}


package breachvault.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Только для тестов: все последовательности длины length, которые можно собрать в сетке по правилам. */
public final class Reachable {

    private Reachable() {
    }

    public static Set<List<String>> sequences(Grid grid, int length) {
        Set<List<String>> result = new HashSet<>();
        walk(grid, Axis.ROW, 0, new HashSet<>(), new ArrayList<>(), length, result);
        return result;
    }

    /** Перебор в глубину: пробуем каждую свободную клетку активной линии, потом откатываем ход. */
    private static void walk(Grid grid, Axis axis, int index, Set<Cell> used, List<String> picked,
                             int length, Set<List<String>> result) {
        if (picked.size() == length) {
            result.add(List.copyOf(picked));
            return;
        }
        for (int i = 0; i < grid.size(); i++) {
            Cell cell = axis == Axis.ROW ? new Cell(index, i) : new Cell(i, index);
            if (used.contains(cell)) {
                continue;
            }
            used.add(cell);
            picked.add(grid.get(cell));
            int nextIndex = axis == Axis.ROW ? cell.col() : cell.row();
            walk(grid, axis.next(), nextIndex, used, picked, length, result);
            picked.remove(picked.size() - 1);
            used.remove(cell);
        }
    }
}

package breachvault.game;

import java.util.List;

/** Результат генерации: сетка и путь, по которому в ней собирается секрет. */
public record Puzzle(Grid grid, List<Cell> solution) {
}

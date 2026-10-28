package breachvault.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Правила Breach Protocol.
 * Первый ход — в строке 0. Выбранная клетка задаёт следующую линию:
 * взяли клетку в строке — дальше активен её столбец, и наоборот.
 */
public class BreachGame {

    private final Grid grid;
    private final int bufferSize;
    private final List<String> buffer = new ArrayList<>();
    private final Set<Cell> used = new HashSet<>();

    private Axis axis = Axis.ROW;
    private int activeIndex = 0; // номер активной строки или столбца

    public BreachGame(Grid grid, int bufferSize) {
        this.grid = grid;
        this.bufferSize = bufferSize;
    }

    /** Лежит ли клетка на активной линии. */
    public boolean isOnActiveLine(Cell cell) {
        return axis == Axis.ROW ? cell.row() == activeIndex : cell.col() == activeIndex;
    }

    public boolean canPick(Cell cell) {
        return !isBufferFull() && !used.contains(cell) && isOnActiveLine(cell);
    }

    /** Делает ход. Возвращает false, если ход недопустим, и тогда ничего не меняется. */
    public boolean pick(Cell cell) {
        if (!canPick(cell)) {
            return false;
        }
        buffer.add(grid.get(cell));
        used.add(cell);
        // Переключаем ось: новая линия проходит через выбранную клетку.
        axis = axis.next();
        activeIndex = axis == Axis.ROW ? cell.row() : cell.col();
        return true;
    }

    /** Есть ли куда ходить: буфер не полон и на активной линии осталась свободная клетка. */
    public boolean hasMoves() {
        for (int i = 0; i < grid.size(); i++) {
            Cell cell = axis == Axis.ROW ? new Cell(activeIndex, i) : new Cell(i, activeIndex);
            if (canPick(cell)) {
                return true;
            }
        }
        return false;
    }

    public boolean isUsed(Cell cell) {
        return used.contains(cell);
    }

    public boolean isBufferFull() {
        return buffer.size() >= bufferSize;
    }

    /** Буфер только для чтения, чтобы снаружи его нельзя было испортить. */
    public List<String> getBuffer() {
        return Collections.unmodifiableList(buffer);
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public Axis getAxis() {
        return axis;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public Grid getGrid() {
        return grid;
    }
}

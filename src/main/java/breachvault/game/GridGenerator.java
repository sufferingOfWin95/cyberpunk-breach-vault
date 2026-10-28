package breachvault.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Делает сетку, в которой секрет точно можно собрать.
 * 1) прокладываем случайный путь по правилам игры;
 * 2) в клетки пути кладём коды секрета;
 * 3) остальные клетки заполняем случайными кодами.
 */
public class GridGenerator {

    private static final int MAX_ATTEMPTS = 1000;

    private final Random random;

    public GridGenerator(Random random) {
        this.random = random;
    }

    /**
     * Та же сетка, но с перемешанными столбцами и строками 1..N-1 (строка 0 остаётся первой).
     *
     * Почему пути не ломаются: клетки одной строки после перестановки остаются в одной строке,
     * клетки одного столбца — в одном столбце, а стартовая строка 0 на месте.
     * Значит, любой путь по правилам переходит в путь с теми же кодами, и наоборот.
     * Выглядит как новая сетка, а набор собираемых последовательностей тот же — лишних подсказок нет.
     */
    public static Grid shuffle(Grid grid, Random random) {
        int size = grid.size();
        List<Integer> rows = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            cols.add(i);
            if (i > 0) {
                rows.add(i);
            }
        }
        Collections.shuffle(rows, random);
        Collections.shuffle(cols, random);
        rows.add(0, 0); // строка 0 всегда первая

        String[][] codes = new String[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                codes[r][c] = grid.get(new Cell(rows.get(r), cols.get(c)));
            }
        }
        return new Grid(codes);
    }

    public Puzzle generate(int size, List<String> secret) {
        validate(size, secret);

        List<Cell> path = randomPath(size, secret.size());

        String[][] codes = new String[size][size];
        for (int i = 0; i < path.size(); i++) {
            Cell cell = path.get(i);
            codes[cell.row()][cell.col()] = secret.get(i);
        }
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (codes[r][c] == null) {
                    codes[r][c] = Grid.CODES.get(random.nextInt(Grid.CODES.size()));
                }
            }
        }
        return new Puzzle(new Grid(codes), List.copyOf(path));
    }

    private void validate(int size, List<String> secret) {
        if (size < 2) {
            throw new IllegalArgumentException("Сетка должна быть хотя бы 2×2");
        }
        if (secret.isEmpty() || secret.size() > size * size) {
            throw new IllegalArgumentException("Неверная длина секрета: " + secret.size());
        }
        for (String code : secret) {
            if (!Grid.CODES.contains(code)) {
                throw new IllegalArgumentException("Неизвестный код: " + code);
            }
        }
    }

    /**
     * Случайный путь заданной длины. Иногда путь заходит в тупик
     * (на активной линии все клетки заняты) — тогда просто пробуем заново.
     */
    private List<Cell> randomPath(int size, int length) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            List<Cell> path = tryPath(size, length);
            if (path != null) {
                return path;
            }
        }
        throw new IllegalStateException("Не удалось построить путь длины " + length);
    }

    /** Одна попытка. Правила те же, что в BreachGame: старт в строке 0, оси чередуются. */
    private List<Cell> tryPath(int size, int length) {
        List<Cell> path = new ArrayList<>();
        Set<Cell> used = new HashSet<>();
        Axis axis = Axis.ROW;
        int activeIndex = 0;

        while (path.size() < length) {
            // Все свободные клетки на активной линии
            List<Cell> free = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                Cell cell = axis == Axis.ROW ? new Cell(activeIndex, i) : new Cell(i, activeIndex);
                if (!used.contains(cell)) {
                    free.add(cell);
                }
            }
            if (free.isEmpty()) {
                return null; // тупик
            }
            Cell next = free.get(random.nextInt(free.size()));
            path.add(next);
            used.add(next);
            axis = axis.next();
            activeIndex = axis == Axis.ROW ? next.row() : next.col();
        }
        return path;
    }
}

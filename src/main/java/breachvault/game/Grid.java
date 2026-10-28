package breachvault.game;

import java.util.List;

/** Квадратная сетка кодов. Только хранит данные, правил игры не знает. */
public class Grid {

    /**
     * Коды, из которых состоит сетка: шесть как в Cyberpunk 2077 и ещё шесть своих.
     * Убирать коды нельзя — старые сейфы с ними перестанут открываться. Добавлять можно.
     */
    public static final List<String> CODES = List.of(
            "1C", "55", "BD", "E9", "7A", "FF",
            "0F", "3E", "8C", "A7", "C4", "D9");

    private final String[][] codes;

    public Grid(String[][] codes) {
        for (String[] row : codes) {
            if (row.length != codes.length) {
                throw new IllegalArgumentException("Сетка должна быть квадратной");
            }
        }
        this.codes = codes;
    }

    public int size() {
        return codes.length;
    }

    public String get(Cell cell) {
        return codes[cell.row()][cell.col()];
    }
}

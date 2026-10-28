package breachvault.game;

/** Какая линия сейчас активна: строка или столбец. */
public enum Axis {
    ROW,
    COLUMN;

    /** Следующая ось: после строки идёт столбец, после столбца — строка. */
    public Axis next() {
        return this == ROW ? COLUMN : ROW;
    }
}

package breachvault.game;

/** Координата клетки в сетке. record сам генерирует конструктор, equals и hashCode. */
public record Cell(int row, int col) {
}

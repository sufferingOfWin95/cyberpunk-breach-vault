package breachvault.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BreachGameTest {

    // Фиксированная сетка, чтобы тест всегда видел одни и те же коды
    private static final String[][] CODES = {
            {"1C", "55", "BD"},
            {"E9", "7A", "FF"},
            {"55", "1C", "E9"},
    };

    private BreachGame game;

    @BeforeEach
    void setUp() {
        game = new BreachGame(new Grid(CODES), 3);
    }

    @Test
    void firstMoveOnlyInRowZero() {
        assertTrue(game.canPick(new Cell(0, 2)));
        assertFalse(game.canPick(new Cell(1, 0)));
    }

    @Test
    void afterPickInRowColumnBecomesActive() {
        game.pick(new Cell(0, 1));

        assertEquals(Axis.COLUMN, game.getAxis());
        assertEquals(1, game.getActiveIndex());
        assertTrue(game.canPick(new Cell(2, 1)));
        assertFalse(game.canPick(new Cell(2, 0)));
    }

    @Test
    void axesAlternate() {
        game.pick(new Cell(0, 1)); // строка 0 -> столбец 1
        game.pick(new Cell(2, 1)); // столбец 1 -> строка 2

        assertEquals(Axis.ROW, game.getAxis());
        assertEquals(2, game.getActiveIndex());
    }

    @Test
    void cannotPickOutsideActiveLine() {
        assertFalse(game.pick(new Cell(2, 2)));
        assertTrue(game.getBuffer().isEmpty());
    }

    @Test
    void cannotPickSameCellTwice() {
        // Буфер побольше, чтобы успеть вернуться в строку 0
        BreachGame big = new BreachGame(new Grid(CODES), 5);
        big.pick(new Cell(0, 0)); // -> столбец 0
        big.pick(new Cell(2, 0)); // -> строка 2
        big.pick(new Cell(2, 2)); // -> столбец 2
        big.pick(new Cell(0, 2)); // -> снова строка 0, но (0,0) уже занята

        assertFalse(big.canPick(new Cell(0, 0)));
        assertTrue(big.canPick(new Cell(0, 1)));
    }

    @Test
    void bufferCollectsCodesAndDoesNotOverflow() {
        game.pick(new Cell(0, 0)); // 1C
        game.pick(new Cell(1, 0)); // E9
        game.pick(new Cell(1, 2)); // FF

        assertEquals(List.of("1C", "E9", "FF"), game.getBuffer());
        assertTrue(game.isBufferFull());
        assertFalse(game.pick(new Cell(2, 2)));
        assertEquals(3, game.getBuffer().size());
    }

    @Test
    void noMovesWhenBufferFull() {
        assertTrue(game.hasMoves());
        game.pick(new Cell(0, 0));
        game.pick(new Cell(1, 0));
        game.pick(new Cell(1, 2));

        assertFalse(game.hasMoves());
    }

    @Test
    void noMovesInDeadEnd() {
        // 2×2 и большой буфер: обходим все 4 клетки по кругу и возвращаемся в занятую строку 0
        BreachGame small = new BreachGame(new Grid(new String[][] {{"1C", "55"}, {"BD", "E9"}}), 6);
        small.pick(new Cell(0, 0)); // -> столбец 0
        small.pick(new Cell(1, 0)); // -> строка 1
        small.pick(new Cell(1, 1)); // -> столбец 1
        small.pick(new Cell(0, 1)); // -> строка 0: обе клетки заняты

        assertFalse(small.isBufferFull());
        assertFalse(small.hasMoves(), "буфер не полон, но ходить некуда — тупик");
    }
}

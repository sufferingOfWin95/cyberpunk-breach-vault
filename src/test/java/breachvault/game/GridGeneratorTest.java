package breachvault.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GridGeneratorTest {

    private static final List<String> SECRET = List.of("BD", "1C", "55", "E9");

    @Test
    void twelveCodes() {
        assertEquals(12, Grid.CODES.size());
        assertEquals(12, new java.util.HashSet<>(Grid.CODES).size(), "коды не повторяются");
    }

    @Test
    void shuffleKeepsExactlyTheSameReachableSequences() {
        for (int seed = 0; seed < 30; seed++) {
            Random random = new Random(seed);
            Grid grid = new GridGenerator(random).generate(5, SECRET).grid();
            Grid shuffled = GridGenerator.shuffle(grid, random);

            // Не «секрет всё ещё собирается», а сильнее: набор собираемых последовательностей тот же самый
            for (int length = 4; length <= 6; length++) {
                assertEquals(Reachable.sequences(grid, length), Reachable.sequences(shuffled, length),
                        "seed " + seed + ", длина " + length);
            }
            assertTrue(Reachable.sequences(shuffled, 4).contains(SECRET));
        }
    }

    @Test
    void shuffleActuallyChangesTheLook() {
        Random random = new Random(3);
        Grid grid = new GridGenerator(random).generate(5, SECRET).grid();
        boolean changed = false;
        for (int i = 0; i < 10 && !changed; i++) {
            Grid shuffled = GridGenerator.shuffle(grid, random);
            for (int r = 0; r < 5 && !changed; r++) {
                for (int c = 0; c < 5 && !changed; c++) {
                    changed = !grid.get(new Cell(r, c)).equals(shuffled.get(new Cell(r, c)));
                }
            }
        }
        assertTrue(changed, "перемешанная сетка должна выглядеть иначе");
    }

    @Test
    void solutionCollectsSecretByGameRules() {
        // Много разных seed — чтобы проверить не один удачный случай, а генератор вообще
        for (int seed = 0; seed < 500; seed++) {
            Puzzle puzzle = new GridGenerator(new Random(seed)).generate(5, SECRET);

            // Прогоняем путь через настоящую игру: каждый ход должен быть разрешён
            BreachGame game = new BreachGame(puzzle.grid(), 6);
            for (Cell cell : puzzle.solution()) {
                assertTrue(game.pick(cell), "seed " + seed + ": недопустимый ход " + cell);
            }
            assertEquals(SECRET, game.getBuffer(), "seed " + seed);
        }
    }

    @Test
    void gridHasRightSizeAndOnlyKnownCodes() {
        Grid grid = new GridGenerator(new Random(42)).generate(5, SECRET).grid();

        assertEquals(5, grid.size());
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                assertTrue(Grid.CODES.contains(grid.get(new Cell(r, c))));
            }
        }
    }

    @Test
    void sameSeedGivesSameGrid() {
        Puzzle a = new GridGenerator(new Random(7)).generate(5, SECRET);
        Puzzle b = new GridGenerator(new Random(7)).generate(5, SECRET);

        assertEquals(a.solution(), b.solution());
    }

    @Test
    void longSecretStillWorks() {
        // Секрет длиной с буфер — путь чаще упирается в тупики, генератор должен справиться
        List<String> longSecret = List.of("FF", "FF", "7A", "1C", "55", "BD");
        Puzzle puzzle = new GridGenerator(new Random(1)).generate(5, longSecret);

        assertEquals(6, puzzle.solution().size());
    }

    @Test
    void rejectsBadSecret() {
        GridGenerator generator = new GridGenerator(new Random());

        assertThrows(IllegalArgumentException.class, () -> generator.generate(5, List.of()));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(5, List.of("ZZ")));
    }
}

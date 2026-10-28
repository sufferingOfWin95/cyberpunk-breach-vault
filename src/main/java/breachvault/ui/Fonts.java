package breachvault.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import javafx.scene.text.Font;

/**
 * Регистрирует шрифт Rajdhani из resources.
 * После этого в CSS можно писать -fx-font-family: "Rajdhani".
 */
final class Fonts {

    // Только два начертания: JavaFX в CSS различает лишь normal и bold,
    // промежуточные веса (500, 600) он не находит.
    private static final String[] FILES = {"Rajdhani-Regular.ttf", "Rajdhani-Bold.ttf"};

    private Fonts() {
    }

    static void load() {
        for (String file : FILES) {
            try (InputStream in = Fonts.class.getResourceAsStream("/breachvault/fonts/" + file)) {
                if (in == null) {
                    throw new IllegalStateException("Нет шрифта в resources: " + file);
                }
                Font.loadFont(in, 14); // размер не важен — CSS всё равно задаст свой
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}

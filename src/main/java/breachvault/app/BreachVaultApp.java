package breachvault.app;

import breachvault.ui.AppShell;
import breachvault.ui.FitPane;
import breachvault.ui.Navigator;
import breachvault.vault.DebugSecretLog;
import java.nio.file.Path;
import java.util.List;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/** Точка входа: собирает каркас, открывает меню и разворачивает окно на весь экран. */
public class BreachVaultApp extends Application {

    /**
     * ТОЛЬКО ДЛЯ ОТЛАДКИ. true — при создании сейфа секрет дописывается открытым текстом
     * в debug-secrets.txt в корне проекта, а в углу окна горит красная метка DEBUG.
     * Не включай для папок с настоящими данными.
     */
    static final boolean DEBUG_SAVE_SECRETS = false;

    // Относительный путь — от рабочей папки. При запуске через gradlew run это корень проекта.
    private static final Path DEBUG_FILE = Path.of("debug-secrets.txt");

    @Override
    public void start(Stage stage) {
        DebugSecretLog debugLog = DEBUG_SAVE_SECRETS ? new DebugSecretLog(DEBUG_FILE) : null;

        AppShell shell = new AppShell(DEBUG_SAVE_SECRETS);
        Navigator navigator = new Navigator(shell, debugLog);
        // Двойной клик по .vault: Windows запускает программу с путём к файлу в аргументах
        List<String> args = getParameters().getRaw();
        if (!args.isEmpty()) {
            navigator.showOpen(Path.of(args.get(0)));
        } else {
            navigator.showMenu();
        }

        Scene scene = new Scene(new FitPane(shell, AppShell.WIDTH, AppShell.HEIGHT), AppShell.WIDTH, AppShell.HEIGHT);
        stage.setTitle("Breach Vault");
        for (int size : new int[] {16, 32, 48, 256}) {
            stage.getIcons().add(new Image(BreachVaultApp.class.getResource("/breachvault/icons/icon-" + size + ".png")
                    .toExternalForm()));
        }
        stage.setScene(scene);
        stage.setFullScreenExitHint(""); // убираем системную подсказку; про Esc написано в углу экрана
        stage.setFullScreen(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

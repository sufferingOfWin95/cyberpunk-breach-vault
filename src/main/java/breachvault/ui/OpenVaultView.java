package breachvault.ui;

import breachvault.crypto.WrongSecretException;
import breachvault.game.BreachGame;
import breachvault.game.GridGenerator;
import breachvault.vault.OneDriveGuard;
import breachvault.vault.VaultFile;
import breachvault.vault.VaultService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

/**
 * Режим открытия.
 *
 * Новый сейф (v2, сетка внутри файла): выбрал файл → сразу сетка с таймером.
 *   Собранный буфер и есть ключ: подошёл — ACCESS GRANTED, нет — DENIED и перемешанная сетка.
 *   Время вышло — тоже DENIED и перемешанная сетка.
 *
 * Старый сейф (v1, без сетки): сначала ввод секрета на панели, по нему строится сетка.
 *
 * После успешной расшифровки файл .vault удаляется — остаётся папка <имя>-restored.
 */
public class OpenVaultView extends StackPane {

    private enum Step { FILE, LEGACY_ENTER }

    /** Итог расшифровки: куда распаковали и что стало с файлом сейфа. */
    private record Restored(Path folder, String note) {
    }

    private final OneDriveGuard oneDrive = OneDriveGuard.fromSystem();
    private final Random random = new Random();
    private final Label stepLabel = new Label();
    private final Label fileLabel = new Label("NO VAULT SELECTED");
    private final Label message = new Label();
    private final CodePad pad = new CodePad();
    private final Button chooseFile = Hud.button("SELECT .VAULT");
    private final Button back = Hud.button("BACK");
    private final Node form; // экран выбора файла (и ввода секрета для старых сейфов)

    private Path vault;
    private VaultFile header;       // разобранный заголовок выбранного сейфа
    private List<String> candidate; // только для v1: набранный секрет, живёт лишь на время взлома

    /** preselected — файл, с которым программу запустили (двойной клик), или null. */
    public OpenVaultView(Path preselected, Runnable onBack) {
        fileLabel.getStyleClass().add("path-label");
        fileLabel.setWrapText(true);
        chooseFile.setOnAction(e -> chooseFile());
        pad.setOnSubmit(secret -> {
            candidate = secret;
            newGrid();
        });
        back.setOnAction(e -> onBack.run());

        form = Hud.twoPanelScreen("OPEN VAULT", stepLabel,
                Hud.panel(Hud.header(Icons.matrix(), "VAULT FILE", "header-filled", "TARGET", "LOCAL FS"),
                        fileLabel, chooseFile),
                Hud.panel(Hud.header(Icons.sequence(), "LEGACY VAULT INPUT", "header-outline", "V1 ONLY", "MASKED"),
                        pad),
                message, back);
        showForm(Step.FILE);
        if (preselected != null) {
            select(preselected);
        }
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Файл сейфа");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Breach Vault", "*" + VaultService.EXTENSION));
        // Начинаем там, где лежал прошлый сейф, иначе — в рабочей папке
        chooser.setInitialDirectory(vault != null && Files.isDirectory(vault.getParent())
                ? vault.getParent().toFile() : new File("").getAbsoluteFile());
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            select(file.toPath());
        }
    }

    private void select(Path file) {
        if (!Files.isRegularFile(file)) {
            Hud.status(message, "FILE NOT FOUND: " + file, "error");
            return;
        }
        if (oneDrive.isSynced(file)) {
            Hud.status(message, "VAULT IS IN ONEDRIVE — RESTORED FILES WOULD SYNC TO THE CLOUD. MOVE IT OUT (E.G. TO C:\\DEV)",
                    "error");
            return;
        }
        try {
            header = VaultService.read(file);
        } catch (IOException ex) {
            Hud.status(message, "NOT A BREACH VAULT: " + ex.getMessage(), "error");
            return;
        }
        vault = file.toAbsolutePath();
        candidate = null;
        fileLabel.setText(vault.toString());
        if (header.hasGrid()) {
            newGrid(); // сразу в бой
        } else {
            Hud.status(message, "LEGACY VAULT WITHOUT MATRIX — ENTER SECRET FIRST", "info");
            showForm(Step.LEGACY_ENTER);
        }
    }

    /** Новая сетка: для v2 — сохранённая, но перемешанная; для v1 — построенная по набранному секрету. */
    private void newGrid() {
        BreachGame game = header.hasGrid()
                ? new BreachGame(GridGenerator.shuffle(header.grid(), random), header.secretLength())
                : new BreachGame(new GridGenerator(random).generate(VaultService.GRID_SIZE, candidate).grid(),
                        candidate.size());
        getChildren().setAll(new BreachScreen(game, this::onBreachComplete, this::onTimeout, this::onAbort));
        Sounds.play(Sounds.Cue.OPEN);
    }

    private void onBreachComplete(List<String> buffer) {
        int expected = header.hasGrid() ? header.secretLength() : candidate.size();
        boolean mismatch = buffer.size() < expected // тупик: буфер не добран
                || (candidate != null && !buffer.equals(candidate)); // v1: промах мимо набранного
        if (mismatch) {
            ResultOverlay.show(this, false, "SEQUENCE INCOMPLETE // MATRIX RESHUFFLED", this::newGrid);
            return;
        }
        decrypt(buffer);
    }

    private void onTimeout() {
        ResultOverlay.show(this, false, "BREACH TIME EXPIRED // MATRIX RESHUFFLED", this::newGrid);
    }

    private void onAbort() {
        candidate = null;
        Hud.status(message, "BREACH ABORTED", "info");
        showForm(header.hasGrid() ? Step.FILE : Step.LEGACY_ENTER);
    }

    private void decrypt(List<String> secret) {
        Path source = vault;
        Task<Restored> task = new Task<>() {
            @Override
            protected Restored call() throws Exception {
                Path folder = VaultService.unlock(source, secret);
                // Сюда попадаем, только если всё расшифровано и распаковано целиком
                try {
                    VaultService.removeVault(source);
                    return new Restored(folder, "VAULT FILE REMOVED");
                } catch (IOException ex) {
                    return new Restored(folder, "VAULT FILE KEPT: " + ex.getMessage());
                }
            }
        };
        task.setOnSucceeded(e -> {
            Restored restored = task.getValue();
            ResultOverlay.show(this, true, "RESTORED TO " + restored.folder(), () -> {
                vault = null;
                header = null;
                candidate = null;
                fileLabel.setText("NO VAULT SELECTED");
                Hud.status(message, "ACCESS GRANTED — RESTORED TO " + restored.folder() + "   // " + restored.note(), "ok");
                showForm(Step.FILE);
            });
        });
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            if (error instanceof WrongSecretException && header.hasGrid()) {
                // Собрал не ту последовательность — та же сетка, но перемешанная
                ResultOverlay.show(this, false, "WRONG SEQUENCE // MATRIX RESHUFFLED", this::newGrid);
            } else if (error instanceof WrongSecretException) {
                // v1: сетку прошёл, но сам набранный секрет неверный — новая сетка не поможет
                ResultOverlay.show(this, false, "WRONG SECRET // RE-ENTER SEQUENCE", () -> {
                    candidate = null;
                    Hud.status(message, "ACCESS DENIED — WRONG SECRET", "error");
                    showForm(Step.LEGACY_ENTER);
                });
            } else {
                Hud.status(message, "ERROR: " + error.getMessage(), "error");
                showForm(Step.FILE);
            }
        });
        Background.run(task, "vault-unlock");
    }

    private void showForm(Step step) {
        getChildren().setAll(form);
        stepLabel.setText(switch (step) {
            case FILE -> "SELECT VAULT // MATRIX LOADS IMMEDIATELY";
            case LEGACY_ENTER -> "LEGACY VAULT // ENTER SECRET";
        });
        pad.clear();
        pad.setDisable(step != Step.LEGACY_ENTER);
    }
}

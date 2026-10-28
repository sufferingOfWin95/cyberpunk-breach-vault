package breachvault.ui;

import breachvault.crypto.WrongSecretException;
import breachvault.vault.DebugSecretLog;
import breachvault.vault.OneDriveGuard;
import breachvault.vault.VaultService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;

/**
 * Режим создания: папка → секрет → повтор секрета → шифрование в <имя>.vault.
 * По галочке оригинал после проверки уничтожается.
 * Шаги идут строго по порядку, текущий шаг хранится в поле step.
 */
public class CreateVaultView extends StackPane {

    private enum Step { FOLDER, ENTER, CONFIRM, WORKING }

    /** Итог фоновой работы: текст и вид сообщения ("ok" или "error"). */
    private record Outcome(String text, String kind) {
    }

    private final DebugSecretLog debugLog; // null — отладка выключена
    private final OneDriveGuard oneDrive = OneDriveGuard.fromSystem();
    private final Label stepLabel = new Label();
    private final Label folderLabel = new Label("NO FOLDER SELECTED");
    private final Label message = new Label();
    private final CodePad pad = new CodePad();
    private final Button chooseFolder = Hud.button("SELECT FOLDER");
    private final CheckBox destroyOriginal = new CheckBox("DESTROY ORIGINAL AFTER ENCRYPTION");
    private final Label destroyWarning = new Label("NO RECYCLE BIN. FORGOTTEN SECRET = FILES LOST FOREVER.");
    private final Button back = Hud.button("BACK");

    private Step step;
    private Path folder;
    private List<String> firstEntry; // первый ввод секрета, ждёт подтверждения

    public CreateVaultView(DebugSecretLog debugLog, Runnable onBack) {
        this.debugLog = debugLog;

        folderLabel.getStyleClass().add("path-label");
        folderLabel.setWrapText(true);
        chooseFolder.setOnAction(e -> chooseFolder());
        destroyOriginal.getStyleClass().add("hud-check");
        destroyOriginal.setFocusTraversable(false);
        destroyWarning.getStyleClass().add("destroy-warning");
        destroyWarning.setWrapText(true);
        destroyWarning.visibleProperty().bind(destroyOriginal.selectedProperty()); // видно только с галочкой
        pad.setOnSubmit(this::onSecret);
        back.setOnAction(e -> onBack.run());

        getChildren().add(Hud.twoPanelScreen("CREATE VAULT", stepLabel,
                Hud.panel(Hud.header(Icons.matrix(), "TARGET FOLDER", "header-filled", "SOURCE", "LOCAL FS"),
                        folderLabel, chooseFolder, destroyOriginal, destroyWarning),
                Hud.panel(Hud.header(Icons.sequence(), "SECRET SEQUENCE", "header-outline", "INPUT", "MASKED"),
                        pad),
                message, back));
        setStep(Step.FOLDER);
    }

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Папка для шифрования");
        chooser.setInitialDirectory(new File("").getAbsoluteFile()); // рабочая папка — корень проекта
        File dir = chooser.showDialog(getScene().getWindow());
        if (dir == null) {
            return; // пользователь нажал «Отмена»
        }
        Path picked = dir.toPath();
        if (oneDrive.isSynced(picked)) {
            Hud.status(message, "THIS FOLDER IS SYNCED BY ONEDRIVE — MOVE IT OUT (E.G. TO C:\\DEV)", "error");
            return;
        }
        if (debugLog != null && debugLog.isInside(picked)) {
            Hud.status(message, "THIS FOLDER CONTAINS " + debugLog.file().getFileName() + " — CHOOSE ANOTHER", "error");
            return;
        }
        Path vault;
        try {
            vault = VaultService.vaultPathFor(picked);
        } catch (IllegalArgumentException ex) {
            Hud.status(message, "CANNOT ENCRYPT A DRIVE ROOT", "error");
            return;
        }
        if (Files.exists(vault)) {
            Hud.status(message, vault.getFileName() + " ALREADY EXISTS — WILL NOT OVERWRITE", "error");
            return;
        }
        folder = picked;
        firstEntry = null;
        folderLabel.setText(folder.toString());
        Hud.status(message, "VAULT WILL BE SAVED AS " + vault, "info");
        setStep(Step.ENTER);
    }

    private void onSecret(List<String> secret) {
        if (step == Step.ENTER) {
            firstEntry = secret;
            Hud.status(message, "REPEAT THE SECRET TO CONFIRM", "info");
            setStep(Step.CONFIRM);
        } else if (step == Step.CONFIRM) {
            boolean match = secret.equals(firstEntry);
            firstEntry = null;
            if (match) {
                lock(secret);
            } else {
                Hud.status(message, "SECRETS DO NOT MATCH — ENTER AGAIN", "error");
                setStep(Step.ENTER);
            }
        }
    }

    private void lock(List<String> secret) {
        setStep(Step.WORKING);
        Hud.status(message, "ENCRYPTING...", "info");
        Path source = folder;
        boolean destroy = destroyOriginal.isSelected(); // читаем в потоке JavaFX, пока он доступен

        Task<Outcome> task = new Task<>() {
            @Override
            protected Outcome call() throws IOException {
                Path vault = VaultService.lock(source, secret);
                String text = "VAULT CREATED: " + vault;
                if (debugLog != null) {
                    // Сейф уже создан — ошибка отладочного лога не должна выглядеть как провал шифрования
                    try {
                        debugLog.append(vault, secret);
                        text += "   [DEBUG: SECRET SAVED]";
                    } catch (IOException ex) {
                        text += "   [DEBUG LOG FAILED: " + ex.getMessage() + "]";
                    }
                }
                if (!destroy) {
                    return new Outcome(text, "ok");
                }

                updateMessage("VERIFYING...");
                try {
                    VaultService.verify(vault, source, secret);
                } catch (IOException | WrongSecretException ex) {
                    return new Outcome(text + "   // VERIFY FAILED, ORIGINAL KEPT: " + ex.getMessage(), "error");
                }
                updateMessage("DESTROYING ORIGINAL...");
                try {
                    VaultService.destroy(source);
                } catch (IOException | IllegalArgumentException ex) {
                    return new Outcome(text + "   // DESTROY FAILED: " + ex.getMessage(), "error");
                }
                return new Outcome(text + "   // ORIGINAL DESTROYED", "ok");
            }
        };
        // updateMessage() из фонового потока безопасно доходит до интерфейса через это свойство
        task.messageProperty().addListener((obs, old, text) -> Hud.status(message, text, "info"));
        task.setOnSucceeded(e -> {
            Outcome outcome = task.getValue();
            Hud.status(message, outcome.text(), outcome.kind());
            folder = null;
            folderLabel.setText("NO FOLDER SELECTED");
            destroyOriginal.setSelected(false); // каждый раз решаем заново, осознанно
            setStep(Step.FOLDER);
        });
        task.setOnFailed(e -> {
            Hud.status(message, "ERROR: " + task.getException().getMessage(), "error");
            setStep(Step.FOLDER);
        });
        Background.run(task, "vault-lock");
    }

    /** Включает и выключает элементы под текущий шаг. */
    private void setStep(Step newStep) {
        step = newStep;
        stepLabel.setText(switch (step) {
            case FOLDER -> "STEP 1/3 // SELECT FOLDER";
            case ENTER -> "STEP 2/3 // ENTER SECRET";
            case CONFIRM -> "STEP 3/3 // CONFIRM SECRET";
            case WORKING -> "ENCRYPTING // DO NOT CLOSE";
        });
        pad.clear();
        pad.setDisable(step != Step.ENTER && step != Step.CONFIRM);
        chooseFolder.setDisable(step == Step.WORKING);
        destroyOriginal.setDisable(step == Step.WORKING);
        back.setDisable(step == Step.WORKING);
    }
}

package breachvault.ui;

import breachvault.vault.DebugSecretLog;
import java.nio.file.Path;
import javafx.application.Platform;

/** Переключает экраны внутри каркаса. Каждый экран получает колбэк «назад в меню». */
public class Navigator {

    private final AppShell shell;
    private final DebugSecretLog debugLog; // null — отладка выключена

    public Navigator(AppShell shell, DebugSecretLog debugLog) {
        this.shell = shell;
        this.debugLog = debugLog;
    }

    public void showMenu() {
        shell.setContent(new MenuView(this::showCreate, () -> showOpen(null), Platform::exit));
    }

    /** vault — файл, который сразу подставить (двойной клик по .vault), или null. */
    public void showOpen(Path vault) {
        shell.setContent(new OpenVaultView(vault, this::showMenu));
    }

    private void showCreate() {
        shell.setContent(new CreateVaultView(debugLog, this::showMenu));
    }
}

package breachvault.ui;

import javafx.concurrent.Task;

/**
 * Запуск долгой работы (PBKDF2, zip, запись на диск) в отдельном потоке.
 * Если делать это в потоке JavaFX, окно «замёрзнет» и перестанет отвечать.
 * Task сам вернёт результат обратно в поток JavaFX через setOnSucceeded / setOnFailed.
 */
final class Background {

    private Background() {
    }

    static void run(Task<?> task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true); // не мешает программе закрыться
        thread.start();
    }
}

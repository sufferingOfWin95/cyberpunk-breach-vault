package breachvault.vault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ТОЛЬКО ДЛЯ ОТЛАДКИ. Записывает секреты открытым текстом — это прямо противоречит
 * идее сейфа. Включается константой DEBUG_SAVE_SECRETS в BreachVaultApp.
 *
 * Формат строки: 2026-10-28 16:40:12 | test-data.vault | BD 1C 55 E9
 */
public final class DebugSecretLog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path file;

    public DebugSecretLog(Path file) {
        this.file = file.toAbsolutePath().normalize();
    }

    /**
     * Лежит ли лог внутри папки. Такую папку шифровать нельзя:
     * секреты других сейфов уехали бы внутрь нового сейфа.
     */
    public boolean isInside(Path folder) {
        return file.startsWith(folder.toAbsolutePath().normalize());
    }

    public void append(Path vault, List<String> secret) throws IOException {
        String line = LocalDateTime.now().format(TIME)
                + " | " + vault.getFileName()
                + " | " + String.join(" ", secret)
                + System.lineSeparator();
        Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public Path file() {
        return file;
    }
}

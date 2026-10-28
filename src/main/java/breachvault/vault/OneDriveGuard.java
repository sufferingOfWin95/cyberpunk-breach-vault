package breachvault.vault;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Определяет, синхронизирует ли путь OneDrive.
 *
 * Почему это важно для сейфа:
 *  - удалённая папка остаётся в облачной корзине OneDrive и в истории версий;
 *  - расшифрованные файлы сразу улетают в облако;
 *  - OneDrive может держать файлы открытыми или подменять их «заглушками», пока они не скачаны.
 *
 * Корни OneDrive Windows сообщает через переменные окружения.
 */
public final class OneDriveGuard {

    private static final List<String> VARIABLES = List.of("OneDrive", "OneDriveConsumer", "OneDriveCommercial");

    private final List<Path> roots = new ArrayList<>();

    /** env передаём снаружи — так в тестах можно подсунуть свои значения. */
    public OneDriveGuard(Map<String, String> env) {
        for (String name : VARIABLES) {
            String value = env.get(name);
            if (value != null && !value.isBlank()) {
                roots.add(Path.of(value).toAbsolutePath().normalize());
            }
        }
    }

    public static OneDriveGuard fromSystem() {
        return new OneDriveGuard(System.getenv());
    }

    public boolean isSynced(Path path) {
        Path abs = path.toAbsolutePath().normalize();
        // На Windows сравнение путей не зависит от регистра букв
        return roots.stream().anyMatch(abs::startsWith);
    }
}

package breachvault.vault;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OneDriveGuardTest {

    // Подсовываем свои переменные окружения, а не настоящие
    private final OneDriveGuard guard = new OneDriveGuard(Map.of("OneDrive", "C:\\Users\\someone\\OneDrive"));

    @Test
    void pathsInsideOneDriveAreSynced() {
        assertTrue(guard.isSynced(Path.of("C:\\Users\\someone\\OneDrive\\Рабочий стол\\test-data-copy")));
        assertTrue(guard.isSynced(Path.of("c:\\users\\SOMEONE\\onedrive\\docs")), "регистр букв не важен");
    }

    @Test
    void pathsOutsideAreNot() {
        assertFalse(guard.isSynced(Path.of("C:\\dev\\breach-vault\\test-data-copy")));
        // Похожее начало имени — не то же самое, что «внутри»
        assertFalse(guard.isSynced(Path.of("C:\\Users\\someone\\OneDriveBackup\\x")));
    }

    @Test
    void noVariablesMeansNothingIsSynced() {
        assertFalse(new OneDriveGuard(Map.of()).isSynced(Path.of("C:\\Users\\someone\\OneDrive\\x")));
    }
}

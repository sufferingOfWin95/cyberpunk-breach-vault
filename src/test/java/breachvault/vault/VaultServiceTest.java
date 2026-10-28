package breachvault.vault;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import breachvault.crypto.WrongSecretException;
import breachvault.game.Reachable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Работаем ТОЛЬКО с копией test-data во временной папке, которую JUnit сам удалит.
 * Настоящую test-data тест только читает.
 */
class VaultServiceTest {

    private static final Path ORIGINAL = Path.of("test-data"); // рабочая папка тестов — корень проекта
    private static final List<String> SECRET = List.of("BD", "1C", "55", "E9");

    @TempDir
    Path temp;

    private Path folder;

    @BeforeEach
    void copyTestData() throws IOException {
        folder = temp.resolve("test-data");
        copyTree(ORIGINAL, folder);
        Files.createDirectories(folder.resolve("empty-dir")); // проверим, что пустые папки не теряются
    }

    @Test
    void lockThenUnlockRestoresSameFiles() throws Exception {
        Path vault = VaultService.lock(folder, SECRET);
        Path restored = VaultService.unlock(vault, SECRET);

        assertEquals(temp.resolve("test-data.vault"), vault);
        assertEquals(temp.resolve("test-data-restored"), restored);
        assertEquals(snapshot(folder), snapshot(restored));
    }

    @Test
    void lockKeepsOriginalFolder() throws Exception {
        Map<String, String> before = snapshot(folder);

        VaultService.lock(folder, SECRET);

        assertEquals(before, snapshot(folder));
    }

    @Test
    void vaultFileDoesNotContainPlainText() throws Exception {
        Path vault = VaultService.lock(folder, SECRET);
        String raw = new String(Files.readAllBytes(vault), java.nio.charset.StandardCharsets.ISO_8859_1);

        assertTrue(raw.startsWith("BVLT"));
        assertFalse(raw.contains("secret-note"), "имена файлов не должны быть видны");
        assertFalse(raw.contains("BD-1C-55-E9") || raw.contains("BD 1C 55 E9"), "секрет не должен храниться");
    }

    @Test
    void wrongSecretCreatesNothing() throws Exception {
        Path vault = VaultService.lock(folder, SECRET);

        assertThrows(WrongSecretException.class,
                () -> VaultService.unlock(vault, List.of("BD", "1C", "55", "FF")));
        assertFalse(Files.exists(temp.resolve("test-data-restored")));
    }

    @Test
    void secondUnlockGoesToNewFolder() throws Exception {
        Path vault = VaultService.lock(folder, SECRET);

        VaultService.unlock(vault, SECRET);
        Path second = VaultService.unlock(vault, SECRET);

        assertEquals(temp.resolve("test-data-restored-2"), second);
    }

    @Test
    void existingVaultIsNeverOverwritten() throws Exception {
        Path vault = VaultService.lock(folder, SECRET);
        byte[] first = Files.readAllBytes(vault);

        assertThrows(FileAlreadyExistsException.class, () -> VaultService.lock(folder, SECRET));
        assertArrayEquals(first, Files.readAllBytes(vault));
    }

    @Test
    void foreignFileIsRejected() throws Exception {
        Path fake = temp.resolve("fake.vault");
        Files.writeString(fake, "это просто текстовый файл, а не сейф, но достаточно длинный");

        assertThrows(InvalidVaultException.class, () -> VaultService.unlock(fake, SECRET));
    }

    @Test
    void secretMustBeFourToSixKnownCodes() {
        assertThrows(IllegalArgumentException.class, () -> VaultService.lock(folder, List.of("BD", "1C", "55")));
        assertThrows(IllegalArgumentException.class,
                () -> VaultService.lock(folder, List.of("BD", "1C", "55", "E9", "7A", "FF", "BD")));
        assertThrows(IllegalArgumentException.class, () -> VaultService.lock(folder, List.of("BD", "1C", "55", "ZZ")));
    }

    @Test
    void zipSlipIsBlocked() throws Exception {
        // Специально собираем «злой» архив с выходом за пределы папки
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("../evil.txt"));
            zip.write("gotcha".getBytes());
            zip.closeEntry();
        }
        Path target = Files.createDirectory(temp.resolve("target"));

        assertThrows(IOException.class, () -> Zipper.unzip(bytes.toByteArray(), target));
        assertFalse(Files.exists(temp.resolve("evil.txt")));
    }

    @Test
    void debugLogAppendsLineAndKnowsWhereItLives() throws Exception {
        DebugSecretLog log = new DebugSecretLog(temp.resolve("debug-secrets.txt"));

        log.append(temp.resolve("test-data.vault"), SECRET);
        log.append(temp.resolve("other.vault"), List.of("FF", "FF", "7A", "1C"));

        List<String> lines = Files.readAllLines(log.file());
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).endsWith("| test-data.vault | BD 1C 55 E9"));
        assertTrue(log.isInside(temp), "лог внутри temp — temp шифровать нельзя");
        assertFalse(log.isInside(folder), "лог не внутри test-data — её шифровать можно");
    }

    @Test
    void newVaultCarriesGridWithPathForSecret() throws Exception {
        Path vault = VaultService.lock(folder, SECRET);

        VaultFile file = VaultService.read(vault);

        assertTrue(file.hasGrid());
        assertEquals(VaultService.GRID_SIZE, file.grid().size());
        assertEquals(SECRET.size(), file.secretLength());
        assertTrue(Reachable.sequences(file.grid(), SECRET.size()).contains(SECRET),
                "в сохранённой сетке секрет должен собираться");
    }

    @Test
    void tamperedGridIsRejected() throws Exception {
        Path vault = VaultService.lock(folder, SECRET);
        byte[] bytes = Files.readAllBytes(vault);
        // Байты 7–8 — первая клетка сетки ("BVLT" + версия + размер + длина = 7 байт до неё)
        String first = new String(bytes, 7, 2, java.nio.charset.StandardCharsets.US_ASCII);
        String other = first.equals("FF") ? "1C" : "FF";
        bytes[7] = (byte) other.charAt(0);
        bytes[8] = (byte) other.charAt(1);
        Files.write(vault, bytes);

        // Подмена заголовка ловится печатью GCM (AAD), даже с верным секретом
        assertThrows(WrongSecretException.class, () -> VaultService.unlock(vault, SECRET));
    }

    @Test
    void legacyV1VaultStillOpens() throws Exception {
        // Собираем сейф старого формата вручную: без сетки и без AAD
        byte[] salt = breachvault.crypto.KeyDeriver.newSalt();
        breachvault.crypto.AesGcmCipher.Encrypted enc = breachvault.crypto.AesGcmCipher.encrypt(
                breachvault.crypto.KeyDeriver.derive(SECRET, salt), Zipper.zip(folder));
        Path vault = temp.resolve("old.vault");
        Files.write(vault, new VaultFile(VaultFile.V1, null, 0, salt, enc.iv(), enc.ciphertext()).toBytes());

        assertFalse(VaultService.read(vault).hasGrid());
        assertEquals(snapshot(folder), snapshot(VaultService.unlock(vault, SECRET)));
    }

    @Test
    void removeVaultDeletesOnlyTheFile() throws Exception {
        Path vault = VaultService.lock(folder, SECRET);
        Path restored = VaultService.unlock(vault, SECRET);

        VaultService.removeVault(vault);

        assertFalse(Files.exists(vault));
        assertTrue(Files.exists(restored.resolve("secret-note.txt")));
    }

    @Test
    void zipIsDeterministic() throws Exception {
        assertArrayEquals(Zipper.zip(folder), Zipper.zip(folder));
    }

    @Test
    void verifyPassesForFreshVault() throws Exception {
        Path vault = VaultService.lock(folder, SECRET);

        VaultService.verify(vault, folder, SECRET); // не бросил — значит, совпало
    }

    @Test
    void verifyFailsIfFolderChangedAfterLock() throws Exception {
        Path vault = VaultService.lock(folder, SECRET);
        Files.writeString(folder.resolve("secret-note.txt"), "изменили после шифрования");

        assertThrows(VerificationException.class, () -> VaultService.verify(vault, folder, SECRET));
    }

    @Test
    void destroyRemovesWholeFolderEvenReadOnlyFiles() throws Exception {
        Path note = folder.resolve("secret-note.txt");
        Files.getFileAttributeView(note, java.nio.file.attribute.DosFileAttributeView.class).setReadOnly(true);

        VaultService.destroy(folder);

        assertFalse(Files.exists(folder));
        assertTrue(Files.exists(temp), "соседей и родителя не трогаем");
    }

    @Test
    void lockVerifyDestroyThenUnlockGivesFilesBack() throws Exception {
        Map<String, String> before = snapshot(folder);

        Path vault = VaultService.lock(folder, SECRET);
        VaultService.verify(vault, folder, SECRET);
        VaultService.destroy(folder);
        Path restored = VaultService.unlock(vault, SECRET);

        assertEquals(before, snapshot(restored));
    }

    @Test
    void refusesToDeleteDangerousFolders() {
        // Проверяем только правило, без настоящего удаления
        Path home = Path.of("C:/Users/someone");
        Path work = Path.of("C:/dev/breach-vault");

        assertThrows(IllegalArgumentException.class,
                () -> VaultService.checkSafeToDelete(Path.of("C:/"), home, work));
        assertThrows(IllegalArgumentException.class,
                () -> VaultService.checkSafeToDelete(Path.of("C:/Users"), home, work));
        assertThrows(IllegalArgumentException.class,
                () -> VaultService.checkSafeToDelete(Path.of("C:/dev"), home, work));
        VaultService.checkSafeToDelete(Path.of("C:/dev/breach-vault/test-data-copy"), home, work); // это можно
    }

    /** Карта «относительный путь → содержимое» для сравнения двух папок. Папки помечаем как "<dir>". */
    private static Map<String, String> snapshot(Path root) throws IOException {
        Map<String, String> result = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.filter(p -> !p.equals(root)).toList()) {
                String name = root.relativize(path).toString().replace('\\', '/');
                result.put(name, Files.isDirectory(path) ? "<dir>" : Files.readString(path));
            }
        }
        return result;
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path path : walk.sorted(Comparator.naturalOrder()).toList()) {
                Files.copy(path, to.resolve(from.relativize(path).toString()));
            }
        }
    }
}

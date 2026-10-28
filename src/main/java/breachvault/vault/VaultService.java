package breachvault.vault;

import breachvault.crypto.AesGcmCipher;
import breachvault.crypto.KeyDeriver;
import breachvault.crypto.WrongSecretException;
import breachvault.game.Grid;
import breachvault.game.GridGenerator;
import java.io.IOException;
import java.security.SecureRandom;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Сейф целиком: папка → zip → шифрование → <имя>.vault и обратно.
 * Правило: ничего не перезаписываем. lock() оставляет исходную папку как была;
 * удалить её можно только отдельным вызовом destroy() и только после verify().
 */
public final class VaultService {

    public static final int MIN_SECRET = 4;
    public static final int MAX_SECRET = 6;
    public static final int GRID_SIZE = 5;
    public static final String EXTENSION = ".vault";

    private static final SecureRandom RANDOM = new SecureRandom();

    private VaultService() {
    }

    /**
     * Шифрует папку в <имя>.vault рядом с ней. Возвращает путь к созданному файлу.
     * Сразу строит сетку с гарантированным путём для секрета и кладёт её в заголовок (формат v2).
     */
    public static Path lock(Path folder, List<String> secret) throws IOException {
        validateSecret(secret);
        if (!Files.isDirectory(folder)) {
            throw new NotDirectoryException(folder.toString());
        }
        Path vault = vaultPathFor(folder);
        if (Files.exists(vault)) {
            throw new FileAlreadyExistsException(vault.toString());
        }

        byte[] zip = Zipper.zip(folder);
        byte[] salt = KeyDeriver.newSalt();
        Grid grid = new GridGenerator(RANDOM).generate(GRID_SIZE, secret).grid();
        byte[] aad = VaultFile.associatedDataFor(grid, secret.size(), salt);
        AesGcmCipher.Encrypted encrypted = AesGcmCipher.encrypt(KeyDeriver.derive(secret, salt), zip, aad);
        byte[] fileBytes = VaultFile.v2(grid, secret.size(), salt, encrypted.iv(), encrypted.ciphertext()).toBytes();

        // CREATE_NEW — вторая страховка: если файл успел появиться, будет ошибка, а не перезапись
        Files.write(vault, fileBytes, StandardOpenOption.CREATE_NEW);
        return vault;
    }

    /** Читает заголовок сейфа (версия, сетка, длина секрета), ничего не расшифровывая. */
    public static VaultFile read(Path vault) throws IOException {
        return VaultFile.parse(Files.readAllBytes(vault));
    }

    /**
     * Расшифровывает .vault в новую папку <имя>-restored (или -restored-2, -3...).
     * При неверном секрете бросает WrongSecretException и ничего не создаёт на диске.
     */
    public static Path unlock(Path vault, List<String> secret) throws IOException, WrongSecretException {
        validateSecret(secret);
        VaultFile file = read(vault);
        // Сначала расшифровка (она же проверка секрета), и только потом что-то пишем на диск
        byte[] zip = decrypt(file, secret);

        Path target = restoreTargetFor(vault);
        Files.createDirectory(target); // createDirectory падает, если папка уже есть
        Zipper.unzip(zip, target);
        return target;
    }

    /** Удаляет файл сейфа. Вызывать только после успешного unlock(): данные уже распакованы. */
    public static void removeVault(Path vault) throws IOException {
        DosFileAttributeView dos = Files.getFileAttributeView(vault, DosFileAttributeView.class);
        if (dos != null) {
            dos.setReadOnly(false);
        }
        Files.delete(vault);
    }

    private static byte[] decrypt(VaultFile file, List<String> secret) throws WrongSecretException {
        return AesGcmCipher.decrypt(KeyDeriver.derive(secret, file.salt()), file.iv(), file.ciphertext(),
                file.associatedData());
    }

    /**
     * Проверка перед удалением оригинала: читаем .vault заново С ДИСКА,
     * расшифровываем и сравниваем байт в байт со свежим zip папки.
     * Ловит и сбой записи, и изменения в папке, сделанные во время шифрования.
     */
    public static void verify(Path vault, Path folder, List<String> secret)
            throws IOException, WrongSecretException {
        byte[] stored = decrypt(read(vault), secret);
        if (!Arrays.equals(stored, Zipper.zip(folder))) {
            throw new VerificationException("Сейф не совпадает с папкой");
        }
    }

    /**
     * Удаляет папку целиком, БЕЗ корзины. Вызывать только после verify().
     * Сначала полностью проверяем, потом удаляем — чтобы не остановиться на полпути.
     */
    public static void destroy(Path folder) throws IOException {
        Path root = folder.toRealPath(); // настоящий путь, без ярлыков
        checkSafeToDelete(root, Path.of(System.getProperty("user.home")), Path.of(""));

        List<Path> paths;
        try (Stream<Path> walk = Files.walk(root)) {
            paths = walk.sorted(Comparator.reverseOrder()).toList(); // дети раньше родителей
        }
        for (Path path : paths) {
            // Ссылка (symlink или junction) внутри папки, ведущая наружу, — не трогаем ничего
            if (!path.toRealPath().startsWith(root)) {
                throw new IOException("Внутри папки ссылка наружу: " + path + " — удаление отменено");
            }
        }
        for (Path path : paths) {
            DosFileAttributeView dos = Files.getFileAttributeView(path, DosFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (dos != null) {
                dos.setReadOnly(false); // файлы «только для чтения» Windows иначе не даст удалить
            }
            Files.delete(path);
        }
    }

    /** Страховка от катастрофы: нельзя удалить корень диска, домашнюю папку и то, что их содержит. */
    static void checkSafeToDelete(Path folder, Path home, Path workDir) {
        Path abs = folder.toAbsolutePath().normalize();
        if (abs.getParent() == null) {
            throw new IllegalArgumentException("Нельзя удалить корень диска");
        }
        if (home.toAbsolutePath().normalize().startsWith(abs)) {
            throw new IllegalArgumentException("Папка содержит домашнюю папку пользователя: " + abs);
        }
        if (workDir.toAbsolutePath().normalize().startsWith(abs)) {
            throw new IllegalArgumentException("Папка содержит рабочую папку программы: " + abs);
        }
    }

    public static Path vaultPathFor(Path folder) {
        Path abs = folder.toAbsolutePath().normalize();
        if (abs.getFileName() == null) {
            throw new IllegalArgumentException("Нельзя зашифровать корень диска");
        }
        return abs.resolveSibling(abs.getFileName() + EXTENSION);
    }

    static Path restoreTargetFor(Path vault) {
        Path abs = vault.toAbsolutePath().normalize();
        String name = abs.getFileName().toString();
        String base = name.endsWith(EXTENSION) ? name.substring(0, name.length() - EXTENSION.length()) : name;

        Path candidate = abs.resolveSibling(base + "-restored");
        for (int n = 2; Files.exists(candidate); n++) {
            candidate = abs.resolveSibling(base + "-restored-" + n);
        }
        return candidate;
    }

    public static void validateSecret(List<String> secret) {
        if (secret.size() < MIN_SECRET || secret.size() > MAX_SECRET) {
            throw new IllegalArgumentException("Секрет должен быть из " + MIN_SECRET + "–" + MAX_SECRET + " кодов");
        }
        for (String code : secret) {
            if (!Grid.CODES.contains(code)) {
                throw new IllegalArgumentException("Неизвестный код: " + code);
            }
        }
    }
}

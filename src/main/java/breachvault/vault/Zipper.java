package breachvault.vault;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Упаковывает папку в zip (в памяти) и распаковывает обратно.
 * В памяти — потому что дальше байты целиком идут в шифрование.
 * Для учебных папок это нормально; для гигабайтных понадобились бы потоки.
 */
public final class Zipper {

    // Одинаковое время у всех записей: тогда одна и та же папка всегда даёт одни и те же байты.
    // Это нужно для проверки перед удалением оригинала (VaultService.verify).
    private static final long FIXED_TIME = 315532800000L; // 1980-01-01 — минимум для формата zip

    private Zipper() {
    }

    public static byte[] zip(Path folder) throws IOException {
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(folder)) {
            paths = walk.sorted().toList(); // sorted — одинаковый порядок записей при каждом запуске
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Path path : paths) {
                if (path.equals(folder)) {
                    continue;
                }
                // Внутри zip пути всегда через "/", даже на Windows
                String name = folder.relativize(path).toString().replace('\\', '/');
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    zip.putNextEntry(entry(name + "/")); // пустые папки тоже сохраняем
                    zip.closeEntry();
                } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    zip.putNextEntry(entry(name));
                    Files.copy(path, zip);
                    zip.closeEntry();
                }
                // Ярлыки (symlink) и прочие особые файлы пропускаем
            }
        }
        return bytes.toByteArray();
    }

    private static ZipEntry entry(String name) {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(FIXED_TIME);
        return entry;
    }

    /** Распаковывает в target. Существующие файлы не перезаписывает — будет ошибка. */
    public static void unzip(byte[] data, Path target) throws IOException {
        Path root = target.toAbsolutePath().normalize();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path out = root.resolve(entry.getName()).normalize();
                // Защита от «Zip Slip»: запись "../../Windows/evil.dll" вылезла бы за пределы target
                if (!out.startsWith(root) || out.equals(root)) {
                    throw new IOException("Подозрительный путь в архиве: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zip, out); // без REPLACE_EXISTING: если файл есть — исключение
                }
            }
        }
    }
}

package breachvault.vault;

import breachvault.crypto.AesGcmCipher;
import breachvault.crypto.KeyDeriver;
import breachvault.game.Cell;
import breachvault.game.Grid;
import java.io.ByteArrayOutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Содержимое файла .vault. Две версии формата:
 *
 * v1 (старая, без сетки):
 *   "BVLT" | 1 | соль (16) | IV (12) | шифротекст + tag
 *
 * v2 (сетка внутри — при открытии сразу показываем её):
 *   "BVLT" | 2 | размер сетки N (1) | длина секрета (1) | сетка (N×N кодов по 2 символа) | соль (16)
 *   | IV (12) | шифротекст + tag
 *
 * Секрета и ключа здесь нет. Сетка лежит открыто — без неё не начать игру.
 * В v2 всё до IV (заголовок) идёт в AAD шифра: подменишь сетку или длину — расшифровка откажет.
 */
public record VaultFile(int version, Grid grid, int secretLength, byte[] salt, byte[] iv, byte[] ciphertext) {

    public static final int V1 = 1;
    public static final int V2 = 2;

    private static final byte[] MAGIC = {'B', 'V', 'L', 'T'};
    private static final int TAG_BYTES = 16;

    /** Новый сейф: всегда v2. */
    public static VaultFile v2(Grid grid, int secretLength, byte[] salt, byte[] iv, byte[] ciphertext) {
        return new VaultFile(V2, grid, secretLength, salt, iv, ciphertext);
    }

    public boolean hasGrid() {
        return version == V2;
    }

    /** Байты, которые шифр «запечатывает» вместе с данными. В v1 их не было. */
    public byte[] associatedData() {
        return version == V2 ? header() : new byte[0];
    }

    public byte[] toBytes() {
        return ByteBuffer.allocate(header().length + iv.length + ciphertext.length)
                .put(header())
                .put(iv)
                .put(ciphertext)
                .array();
    }

    /** Всё до IV: magic, версия, (в v2 — размер, длина, сетка), соль. */
    private byte[] header() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(MAGIC);
        out.write(version);
        if (version == V2) {
            out.write(grid.size());
            out.write(secretLength);
            for (int r = 0; r < grid.size(); r++) {
                for (int c = 0; c < grid.size(); c++) {
                    out.writeBytes(grid.get(new Cell(r, c)).getBytes(StandardCharsets.US_ASCII));
                }
            }
        }
        out.writeBytes(salt);
        return out.toByteArray();
    }

    /**
     * AAD для нового сейфа. Курица и яйцо: заголовок нужен до шифрования (он идёт в AAD),
     * а IV и шифротекст появляются только после. Поэтому собираем заголовок отдельно —
     * он от IV и шифротекста не зависит.
     */
    static byte[] associatedDataFor(Grid grid, int secretLength, byte[] salt) {
        return new VaultFile(V2, grid, secretLength, salt, new byte[0], new byte[0]).header();
    }

    public static VaultFile parse(byte[] data) throws InvalidVaultException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        try {
            byte[] magic = new byte[MAGIC.length];
            buffer.get(magic);
            if (!Arrays.equals(magic, MAGIC)) {
                throw new InvalidVaultException("Это не файл Breach Vault");
            }
            int version = buffer.get();
            Grid grid = null;
            int secretLength = 0;
            if (version == V2) {
                int size = buffer.get();
                secretLength = buffer.get();
                if (size < 2 || size > 10 || secretLength < VaultService.MIN_SECRET
                        || secretLength > VaultService.MAX_SECRET) {
                    throw new InvalidVaultException("Повреждённый заголовок сейфа");
                }
                grid = readGrid(buffer, size);
            } else if (version != V1) {
                throw new InvalidVaultException("Неизвестная версия сейфа: " + version);
            }

            byte[] salt = new byte[KeyDeriver.SALT_BYTES];
            buffer.get(salt);
            byte[] iv = new byte[AesGcmCipher.IV_BYTES];
            buffer.get(iv);
            if (buffer.remaining() < TAG_BYTES) {
                throw new InvalidVaultException("Файл обрезан");
            }
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            return new VaultFile(version, grid, secretLength, salt, iv, ciphertext);
        } catch (BufferUnderflowException e) {
            throw new InvalidVaultException("Файл слишком короткий для сейфа");
        }
    }

    private static Grid readGrid(ByteBuffer buffer, int size) throws InvalidVaultException {
        String[][] codes = new String[size][size];
        byte[] pair = new byte[2];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                buffer.get(pair);
                String code = new String(pair, StandardCharsets.US_ASCII);
                if (!Grid.CODES.contains(code)) {
                    throw new InvalidVaultException("Неизвестный код в сетке: " + code);
                }
                codes[r][c] = code;
            }
        }
        return new Grid(codes);
    }
}

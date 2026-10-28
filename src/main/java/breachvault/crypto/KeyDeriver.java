package breachvault.crypto;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Превращает секрет (последовательность кодов) в 256-битный ключ AES.
 *
 * PBKDF2 нарочно медленный: 600 000 повторов HMAC-SHA256. Для нас это доли секунды,
 * а для того, кто перебирает секреты, — те же доли секунды на КАЖДУЮ попытку.
 * Соль — случайные байты, разные для каждого сейфа: одинаковый секрет
 * в двух сейфах даст два разных ключа.
 */
public final class KeyDeriver {

    public static final int ITERATIONS = 600_000;
    public static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private KeyDeriver() {
    }

    public static byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return salt;
    }

    public static SecretKey derive(List<String> secret, byte[] salt) {
        if (salt.length != SALT_BYTES) {
            throw new IllegalArgumentException("Соль должна быть " + SALT_BYTES + " байт");
        }
        // "BD-1C-55-E9" — char[], а не String: массив можно затереть после использования
        char[] password = String.join("-", secret).toCharArray();
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            byte[] keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (GeneralSecurityException e) {
            // Алгоритм входит в любую Java 21 — сюда попасть не должны
            throw new IllegalStateException("PBKDF2 недоступен", e);
        } finally {
            Arrays.fill(password, '\0');
            spec.clearPassword();
        }
    }
}

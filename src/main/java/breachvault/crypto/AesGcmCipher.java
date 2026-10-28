package breachvault.crypto;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES-256-GCM: шифрует и одновременно добавляет «печать» (tag, 16 байт).
 * При расшифровке печать проверяется: неверный ключ или изменённый хоть один байт —
 * и расшифровка отказывает, а не выдаёт мусор.
 *
 * IV (12 байт) — новый случайный при каждом шифровании.
 * Повторить IV с тем же ключом в GCM нельзя ни в коем случае: это ломает защиту.
 */
public final class AesGcmCipher {

    public static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Результат шифрования. IV не секретный — его хранят рядом с шифротекстом. */
    public record Encrypted(byte[] iv, byte[] ciphertext) {
    }

    private AesGcmCipher() {
    }

    public static Encrypted encrypt(SecretKey key, byte[] plain) {
        return encrypt(key, plain, new byte[0]);
    }

    /**
     * aad (associated data) — открытые данные, которые не шифруются, но входят в «печать».
     * Изменишь в них хоть байт — расшифровка откажет. Так защищаем открытый заголовок файла.
     */
    public static Encrypted encrypt(SecretKey key, byte[] plain, byte[] aad) {
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad);
            return new Encrypted(iv, cipher.doFinal(plain)); // tag дописывается в конец шифротекста
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Не удалось зашифровать", e);
        }
    }

    public static byte[] decrypt(SecretKey key, byte[] iv, byte[] ciphertext) throws WrongSecretException {
        return decrypt(key, iv, ciphertext, new byte[0]);
    }

    /** aad должен быть тем же, что при шифровании, иначе — WrongSecretException. */
    public static byte[] decrypt(SecretKey key, byte[] iv, byte[] ciphertext, byte[] aad) throws WrongSecretException {
        if (iv.length != IV_BYTES) {
            throw new IllegalArgumentException("IV должен быть " + IV_BYTES + " байт");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad);
            return cipher.doFinal(ciphertext);
        } catch (AEADBadTagException e) {
            throw new WrongSecretException(e); // печать не сошлась
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Не удалось расшифровать", e);
        }
    }
}

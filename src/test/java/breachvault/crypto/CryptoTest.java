package breachvault.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** PBKDF2 нарочно медленный, поэтому основной ключ считаем один раз на все тесты. */
class CryptoTest {

    private static final List<String> SECRET = List.of("BD", "1C", "55", "E9");
    private static final String TEXT = "Секретная заметка: код от шкафчика 4417";

    private static byte[] salt;
    private static SecretKey key;

    @BeforeAll
    static void deriveKey() {
        salt = KeyDeriver.newSalt();
        key = KeyDeriver.derive(SECRET, salt);
    }

    @Test
    void encryptThenDecryptGivesSameText() throws WrongSecretException {
        AesGcmCipher.Encrypted enc = AesGcmCipher.encrypt(key, TEXT.getBytes(StandardCharsets.UTF_8));

        byte[] plain = AesGcmCipher.decrypt(key, enc.iv(), enc.ciphertext());

        assertEquals(TEXT, new String(plain, StandardCharsets.UTF_8));
    }

    @Test
    void sizesMatchSpec() {
        AesGcmCipher.Encrypted enc = AesGcmCipher.encrypt(key, new byte[10]);

        assertEquals(16, salt.length);
        assertEquals(12, enc.iv().length);
        assertEquals(32, key.getEncoded().length);         // 256 бит
        assertEquals(10 + 16, enc.ciphertext().length);    // данные + tag 16 байт
    }

    @Test
    void sameTextEncryptsDifferentlyEachTime() {
        byte[] data = TEXT.getBytes(StandardCharsets.UTF_8);
        AesGcmCipher.Encrypted a = AesGcmCipher.encrypt(key, data);
        AesGcmCipher.Encrypted b = AesGcmCipher.encrypt(key, data);

        assertFalse(Arrays.equals(a.iv(), b.iv()), "IV должен быть новым каждый раз");
        assertFalse(Arrays.equals(a.ciphertext(), b.ciphertext()));
    }

    @Test
    void wrongSecretIsRejected() {
        AesGcmCipher.Encrypted enc = AesGcmCipher.encrypt(key, TEXT.getBytes(StandardCharsets.UTF_8));
        SecretKey wrong = KeyDeriver.derive(List.of("BD", "1C", "55", "FF"), salt); // один код другой

        assertThrows(WrongSecretException.class, () -> AesGcmCipher.decrypt(wrong, enc.iv(), enc.ciphertext()));
    }

    @Test
    void tamperedDataIsRejected() {
        AesGcmCipher.Encrypted enc = AesGcmCipher.encrypt(key, TEXT.getBytes(StandardCharsets.UTF_8));
        enc.ciphertext()[0] ^= 1; // меняем один бит

        assertThrows(WrongSecretException.class, () -> AesGcmCipher.decrypt(key, enc.iv(), enc.ciphertext()));
    }

    @Test
    void changedAssociatedDataIsRejected() throws WrongSecretException {
        byte[] header = "BVLT-header-with-grid".getBytes(StandardCharsets.US_ASCII);
        AesGcmCipher.Encrypted enc = AesGcmCipher.encrypt(key, TEXT.getBytes(StandardCharsets.UTF_8), header);

        // С тем же заголовком — расшифровывается
        AesGcmCipher.decrypt(key, enc.iv(), enc.ciphertext(), header);

        // Заголовок изменили на один символ — отказ, хотя сам шифротекст не трогали
        byte[] forged = header.clone();
        forged[forged.length - 1] = 'X';
        assertThrows(WrongSecretException.class, () -> AesGcmCipher.decrypt(key, enc.iv(), enc.ciphertext(), forged));
    }

    @Test
    void keyDependsOnSecretAndSalt() {
        assertArrayEquals(key.getEncoded(), KeyDeriver.derive(SECRET, salt).getEncoded());
        assertFalse(Arrays.equals(key.getEncoded(), KeyDeriver.derive(SECRET, KeyDeriver.newSalt()).getEncoded()),
                "другая соль — другой ключ");
    }
}

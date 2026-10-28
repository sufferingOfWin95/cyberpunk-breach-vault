package breachvault.vault;

import java.io.IOException;

/** Проверка сейфа не прошла: расшифрованное не совпало с папкой. Оригинал удалять нельзя. */
public class VerificationException extends IOException {

    public VerificationException(String message) {
        super(message);
    }
}

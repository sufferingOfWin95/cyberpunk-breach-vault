package breachvault.vault;

import java.io.IOException;

/** Файл не похож на наш сейф: другой формат, другая версия или обрезан. */
public class InvalidVaultException extends IOException {

    public InvalidVaultException(String message) {
        super(message);
    }
}

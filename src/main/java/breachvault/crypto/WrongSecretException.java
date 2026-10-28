package breachvault.crypto;

/**
 * Расшифровка не прошла проверку: секрет неверный или данные испорчены.
 * GCM не умеет отличать одно от другого — и это хорошо: атакующему тоже ничего не подсказывает.
 */
public class WrongSecretException extends Exception {

    public WrongSecretException(Throwable cause) {
        super("Неверный секрет или повреждённые данные", cause);
    }
}

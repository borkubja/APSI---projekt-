package pl.apsi.repo.config;

/** Żądanie jest poprawne, ale koliduje ze stanem danych (np. duplikat nazwy) - HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

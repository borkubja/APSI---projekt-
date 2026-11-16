package pl.apsi.repo.config;

/** Brak upoważnienia do folderu (lub upoważnienie tylko do odczytu przy próbie zapisu). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}

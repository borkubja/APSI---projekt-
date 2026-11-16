package pl.apsi.repo.config;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String what, Long id) {
        super(what + " o id " + id + " nie istnieje");
    }
}

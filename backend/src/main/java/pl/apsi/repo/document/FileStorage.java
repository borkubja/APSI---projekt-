package pl.apsi.repo.document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Pliki wersji leżą na dysku pod losowym kluczem; nazwa oryginalna jest tylko w bazie. */
@Service
public class FileStorage {

    public record Stored(String key, String sha256, long size) {
    }

    private final Path root;

    public FileStorage(@Value("${app.storage-dir:storage}") String dir) throws IOException {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public Stored save(MultipartFile file) {
        String key = UUID.randomUUID().toString();
        try (InputStream in = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream din = new DigestInputStream(in, digest)) {
                long size = Files.copy(din, root.resolve(key));
                return new Stored(key, HexFormat.of().formatHex(digest.digest()), size);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Nie udało się zapisać pliku", e);
        }
    }

    public Resource load(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root) || !Files.isReadable(path)) {
            throw new IllegalStateException("Brak pliku w magazynie: " + key);
        }
        return new FileSystemResource(path);
    }
}

package com.tabibma.shared.storage;

import com.tabibma.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Dev/test ObjectStorageClient backed by the local filesystem. The storage key never derives
 * from client input beyond a whitelisted extension — the filename itself is never used to build
 * a path, so a malicious filename (e.g. "../../etc/passwd") cannot escape the base directory.
 */
@Component
public class LocalFilesystemObjectStorageClient implements ObjectStorageClient {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^[a-zA-Z0-9/_-]+$");

    private final Path baseDir;

    public LocalFilesystemObjectStorageClient(@Value("${app.storage.local-dir}") String localDir) {
        this.baseDir = Path.of(localDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create local storage directory: " + baseDir, e);
        }
    }

    @Override
    public String store(String keyPrefix, String filename, InputStream content, long contentLength, String contentType) {
        if (!PREFIX_PATTERN.matcher(keyPrefix).matches()) {
            throw new IllegalArgumentException("keyPrefix must be alphanumeric/underscore/hyphen path segments.");
        }
        String extension = extensionOf(filename);
        String storageKey = keyPrefix + "/" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path target = resolveWithinBase(storageKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store object at " + storageKey, e);
        }
        return storageKey;
    }

    @Override
    public InputStream load(String storageKey) {
        Path target = resolveWithinBase(storageKey);
        if (!Files.exists(target)) {
            throw new NotFoundException("Stored object not found.");
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load object at " + storageKey, e);
        }
    }

    private Path resolveWithinBase(String storageKey) {
        Path resolved = baseDir.resolve(storageKey).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new IllegalArgumentException("Storage key escapes the storage root.");
        }
        return resolved;
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        String extension = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.contains(extension) ? extension : "";
    }
}

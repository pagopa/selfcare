package it.pagopa.selfcare.onboarding.storage;

import com.azure.storage.blob.models.BlobProperties;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Applies the trusted {@code pathPrefix} of a tenant storage binding to every blob path, using the
 * same normalization rules as the {@code document-ms} storage layer: control characters, absolute
 * paths, drive letters and {@code ..} segments are rejected.
 */
public class PrefixingAzureBlobClient implements AzureBlobClient {

    private static final String INVALID_STORAGE_PATH = "Invalid storage path";

    private final AzureBlobClient delegate;
    private final String pathPrefix;

    public PrefixingAzureBlobClient(AzureBlobClient delegate, String pathPrefix) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.pathPrefix = normalizePrefix(pathPrefix);
    }

    @Override
    public File retrieveFile(String filePath) {
        return delegate.retrieveFile(prefixed(filePath));
    }

    @Override
    public byte[] getFile(String filePath) {
        return delegate.getFile(prefixed(filePath));
    }

    @Override
    public String getFileAsText(String filePath) {
        return delegate.getFileAsText(prefixed(filePath));
    }

    @Override
    public File getFileAsPdf(String contractTemplate) {
        return delegate.getFileAsPdf(prefixed(contractTemplate));
    }

    @Override
    public String uploadFile(String path, String filename, byte[] data) {
        return unprefixed(delegate.uploadFile(prefixed(path), normalizeBlobPath(filename), data));
    }

    @Override
    public String uploadFilePath(String filePath, byte[] data) {
        return unprefixed(delegate.uploadFilePath(prefixed(filePath), data));
    }

    @Override
    public void removeFile(String fileName) {
        delegate.removeFile(prefixed(fileName));
    }

    @Override
    public BlobProperties getProperties(String filePath) {
        return delegate.getProperties(prefixed(filePath));
    }

    @Override
    public List<String> getFiles() {
        if (pathPrefix.isBlank()) {
            return delegate.getFiles();
        }
        return listWithinPrefix(pathPrefix + "/");
    }

    @Override
    public List<String> getFiles(String path) {
        String prefixedPath = prefixed(path);
        if (pathPrefix.isBlank()) {
            return delegate.getFiles(prefixedPath);
        }
        // the trailing separator keeps sibling prefixes (e.g. "ar2" for "ar") out of the listing
        return listWithinPrefix(prefixedPath.equals(pathPrefix) ? pathPrefix + "/" : prefixedPath);
    }

    private List<String> listWithinPrefix(String listPrefix) {
        String boundary = pathPrefix + "/";
        return delegate.getFiles(listPrefix).stream()
                .filter(name -> name != null && name.startsWith(boundary))
                .map(this::unprefixed)
                .toList();
    }

    String prefixed(String path) {
        String normalized = normalizeBlobPath(path);
        if (pathPrefix.isBlank()) {
            return normalized;
        }
        if (normalized.isBlank()) {
            return pathPrefix;
        }
        return pathPrefix + "/" + normalized;
    }

    String unprefixed(String path) {
        if (path == null || pathPrefix.isBlank()) {
            return path;
        }
        if (path.equals(pathPrefix)) {
            return "";
        }
        String prefix = pathPrefix + "/";
        return path.startsWith(prefix) ? path.substring(prefix.length()) : path;
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        return normalizeBlobPath(prefix);
    }

    static String normalizeBlobPath(String path) {
        if (path == null || containsControlCharacter(path)) {
            throw new IllegalArgumentException(INVALID_STORAGE_PATH);
        }
        String normalizedSeparators = path.replace('\\', '/').trim();
        if (normalizedSeparators.startsWith("/") || normalizedSeparators.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException(INVALID_STORAGE_PATH);
        }
        StringBuilder normalized = new StringBuilder();
        for (String segment : normalizedSeparators.split("/")) {
            if (segment.isBlank() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                throw new IllegalArgumentException(INVALID_STORAGE_PATH);
            }
            if (normalized.length() > 0) {
                normalized.append('/');
            }
            normalized.append(segment);
        }
        return normalized.toString();
    }

    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(ch -> ch < 0x20 || ch == 0x7F);
    }
}

package it.pagopa.selfcare.commons.tenant.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import java.time.Duration;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class TenantBlobClient {

    private final BlobContainerClient containerClient;
    private final String pathPrefix;

    TenantBlobClient(BlobContainerClient containerClient, String pathPrefix) {
        this.containerClient = containerClient;
        this.pathPrefix = normalizePrefix(pathPrefix);
    }

    public BlobClient blob(String path) {
        return containerClient.getBlobClient(prefixed(path));
    }

    public Stream<BlobItem> list() {
        ListBlobsOptions options = new ListBlobsOptions();
        if (!pathPrefix.isBlank()) {
            options.setPrefix(pathPrefix + "/");
        }
        return StreamSupport.stream(
                containerClient.listBlobs(options, (Duration) null).spliterator(), false);
    }

    String prefixed(String path) {
        if (path == null || path.isBlank()) {
            return pathPrefix;
        }
        String normalizedPath = path.trim();
        if (normalizedPath.startsWith("/") || containsParentTraversal(normalizedPath)) {
            throw new IllegalArgumentException("Blob path must be relative and cannot contain '..'");
        }
        if (pathPrefix.isBlank()) {
            return normalizedPath;
        }
        if (normalizedPath.equals(pathPrefix)) {
            normalizedPath = "";
        } else if (normalizedPath.startsWith(pathPrefix + "/")) {
            normalizedPath = normalizedPath.substring(pathPrefix.length() + 1);
        }
        return normalizedPath.isBlank()
                ? pathPrefix
                : pathPrefix + "/" + normalizedPath;
    }

    private static boolean containsParentTraversal(String path) {
        return java.util.Arrays.stream(path.split("/"))
                .anyMatch(".."::equals);
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String normalized = prefix.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

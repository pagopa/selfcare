package it.pagopa.selfcare.onboarding.storage;

import com.azure.storage.blob.models.BlobProperties;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import java.io.File;
import java.util.List;

final class PrefixingAzureBlobClient implements AzureBlobClient {

    private final AzureBlobClient delegate;
    private final String pathPrefix;

    private PrefixingAzureBlobClient(AzureBlobClient delegate, String pathPrefix) {
        this.delegate = delegate;
        this.pathPrefix = pathPrefix;
    }

    static AzureBlobClient wrap(AzureBlobClient delegate, String pathPrefix) {
        if (pathPrefix == null || pathPrefix.isBlank()) {
            return delegate;
        }
        String normalized = pathPrefix.endsWith("/") ? pathPrefix.substring(0, pathPrefix.length() - 1) : pathPrefix;
        return new PrefixingAzureBlobClient(delegate, normalized);
    }

    private String prefixed(String path) {
        if (path == null || path.isBlank()) {
            return pathPrefix;
        }
        if (path.startsWith(pathPrefix + "/") || path.equals(pathPrefix)) {
            return path;
        }
        return pathPrefix + "/" + path;
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
        return delegate.uploadFile(prefixed(path), filename, data);
    }

    @Override
    public String uploadFilePath(String filePath, byte[] data) {
        return delegate.uploadFilePath(prefixed(filePath), data);
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
        return delegate.getFiles(pathPrefix);
    }

    @Override
    public List<String> getFiles(String path) {
        return delegate.getFiles(prefixed(path));
    }
}

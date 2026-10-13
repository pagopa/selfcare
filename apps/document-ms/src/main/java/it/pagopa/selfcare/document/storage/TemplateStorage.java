package it.pagopa.selfcare.document.storage;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.document.exception.InvalidRequestException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class TemplateStorage {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".html", ".pdf");

    private final TenantBlobClientProvider blobClientProvider;

    @Inject
    public TemplateStorage(TenantBlobClientProvider blobClientProvider) {
        this.blobClientProvider = blobClientProvider;
    }

    public File getFileAsPdf(String templatePath) {
        return contractsClient().getFileAsPdf(validateTemplatePath(templatePath));
    }

    public String getFileAsText(String templatePath) {
        return contractsClient().getFileAsText(validateTemplatePath(templatePath));
    }

    public String validateTemplatePath(String templatePath) {
        String normalized = PrefixingAzureBlobClient.normalizeBlobPath(templatePath);
        String lower = normalized.toLowerCase(Locale.ROOT);
        boolean allowed = ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
        if (!allowed) {
            throw new InvalidRequestException("Invalid template path", "400");
        }
        return normalized;
    }

    private AzureBlobClient contractsClient() {
        return blobClientProvider.clientForCurrentTenant(StorageKeys.CONTRACTS);
    }
}

package it.pagopa.selfcare.document.storage;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.document.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplateStorageTest {

    private final TenantBlobClientProvider provider = mock(TenantBlobClientProvider.class);
    private final AzureBlobClient contractsClient = mock(AzureBlobClient.class);
    private final TemplateStorage storage = new TemplateStorage(provider);

    @Test
    void getFileAsPdf_shouldReadFromContractsBinding() {
        File file = mock(File.class);
        when(provider.clientForCurrentTenant(StorageKeys.CONTRACTS)).thenReturn(contractsClient);
        when(contractsClient.getFileAsPdf("templates/contract.pdf")).thenReturn(file);

        assertEquals(file, storage.getFileAsPdf("templates/contract.pdf"));
        verify(contractsClient).getFileAsPdf("templates/contract.pdf");
    }

    @Test
    void getFileAsText_shouldAllowHtmlTemplates() {
        when(provider.clientForCurrentTenant(StorageKeys.CONTRACTS)).thenReturn(contractsClient);
        when(contractsClient.getFileAsText("templates/contract.html")).thenReturn("<html></html>");

        assertEquals("<html></html>", storage.getFileAsText("templates/contract.html"));
    }

    @Test
    void validateTemplatePath_shouldRejectUnsupportedExtensions() {
        assertThrows(InvalidRequestException.class, () -> storage.validateTemplatePath("templates/contract.ftl"));
        assertThrows(InvalidRequestException.class, () -> storage.validateTemplatePath("templates/contract.txt"));
    }

    @Test
    void validateTemplatePath_shouldRejectTraversalAndAbsolutePaths() {
        assertThrows(InvalidRequestException.class, () -> storage.validateTemplatePath("../contract.pdf"));
        assertThrows(InvalidRequestException.class, () -> storage.validateTemplatePath("/templates/contract.pdf"));
    }
}

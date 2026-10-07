package it.pagopa.selfcare.document.storage;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.document.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrefixingAzureBlobClientTest {

    private final AzureBlobClient delegate = mock(AzureBlobClient.class);

    @Test
    void getFileAsPdf_shouldApplyTrustedPrefix() {
        PrefixingAzureBlobClient client = new PrefixingAzureBlobClient(delegate, "tenant/ar");

        client.getFileAsPdf("contracts/template.pdf");

        verify(delegate).getFileAsPdf("tenant/ar/contracts/template.pdf");
    }

    @Test
    void uploadFilePath_shouldReturnLogicalPathWithoutPrefix() {
        PrefixingAzureBlobClient client = new PrefixingAzureBlobClient(delegate, "tenant/ar");
        when(delegate.uploadFilePath("tenant/ar/contracts/file.pdf", new byte[]{1})).thenReturn("tenant/ar/contracts/file.pdf");

        String result = client.uploadFilePath("contracts/file.pdf", new byte[]{1});

        assertEquals("contracts/file.pdf", result);
    }

    @Test
    void getFiles_shouldStripTrustedPrefixFromResults() {
        PrefixingAzureBlobClient client = new PrefixingAzureBlobClient(delegate, "tenant/ar");
        when(delegate.getFiles("tenant/ar/contracts")).thenReturn(List.of("tenant/ar/contracts/a.pdf"));

        assertEquals(List.of("contracts/a.pdf"), client.getFiles("contracts"));
    }

    @Test
    void getFiles_shouldListWholeTenantPrefixWithBoundarySeparator() {
        PrefixingAzureBlobClient client = new PrefixingAzureBlobClient(delegate, "tenant/ar");
        when(delegate.getFiles("tenant/ar/")).thenReturn(List.of("tenant/ar/a.pdf", "tenant/ar/contracts/b.pdf"));

        assertEquals(List.of("a.pdf", "contracts/b.pdf"), client.getFiles());
        assertEquals(List.of("a.pdf", "contracts/b.pdf"), client.getFiles(""));
        verify(delegate, times(2)).getFiles("tenant/ar/");
    }

    @Test
    void getFiles_shouldDropEntriesOutsideTheTenantPrefix() {
        PrefixingAzureBlobClient client = new PrefixingAzureBlobClient(delegate, "tenant/ar");
        when(delegate.getFiles("tenant/ar/")).thenReturn(List.of("tenant/ar2/leak.pdf", "tenant/ar/a.pdf", "other/b.pdf"));
        when(delegate.getFiles("tenant/ar/contracts")).thenReturn(List.of("tenant/ar/contracts/a.pdf", "tenant/arx/c.pdf"));

        assertEquals(List.of("a.pdf"), client.getFiles());
        assertEquals(List.of("contracts/a.pdf"), client.getFiles("contracts"));
    }

    @Test
    void getFiles_shouldKeepLegacyBehaviorWhenThereIsNoPrefix() {
        PrefixingAzureBlobClient client = new PrefixingAzureBlobClient(delegate, "");
        when(delegate.getFiles()).thenReturn(List.of("a.pdf", "contracts/b.pdf"));
        when(delegate.getFiles("contracts")).thenReturn(List.of("contracts/b.pdf"));

        assertEquals(List.of("a.pdf", "contracts/b.pdf"), client.getFiles());
        assertEquals(List.of("contracts/b.pdf"), client.getFiles("contracts"));
    }

    @Test
    void normalizeBlobPath_shouldRejectTraversal() {
        assertThrows(InvalidRequestException.class, () -> PrefixingAzureBlobClient.normalizeBlobPath("../contracts/a.pdf"));
        assertThrows(InvalidRequestException.class, () -> PrefixingAzureBlobClient.normalizeBlobPath("contracts/../a.pdf"));
    }

    @Test
    void normalizeBlobPath_shouldRejectAbsoluteAndControlCharacters() {
        assertThrows(InvalidRequestException.class, () -> PrefixingAzureBlobClient.normalizeBlobPath("/contracts/a.pdf"));
        assertThrows(InvalidRequestException.class, () -> PrefixingAzureBlobClient.normalizeBlobPath("C:/contracts/a.pdf"));
        assertThrows(InvalidRequestException.class, () -> PrefixingAzureBlobClient.normalizeBlobPath("contracts/\u0000/a.pdf"));
    }

    @Test
    void normalizeBlobPath_shouldNormalizeSeparatorsAndDots() {
        assertEquals("contracts/a.pdf", PrefixingAzureBlobClient.normalizeBlobPath("contracts//./a.pdf"));
        assertEquals("contracts/a.pdf", PrefixingAzureBlobClient.normalizeBlobPath("contracts\\a.pdf"));
    }
}

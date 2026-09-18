package it.pagopa.selfcare.onboarding.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.models.BlobProperties;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrefixingAzureBlobClientTest {

    @Test
    void wrap_returnsDelegateWhenPrefixIsBlank() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);

        assertSame(delegate, PrefixingAzureBlobClient.wrap(delegate, ""));
        assertSame(delegate, PrefixingAzureBlobClient.wrap(delegate, null));
        assertSame(delegate, PrefixingAzureBlobClient.wrap(delegate, "   "));
    }

    @Test
    void wrap_prefixesBlobPaths() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);
        AzureBlobClient prefixed = PrefixingAzureBlobClient.wrap(delegate, "onboarding/");

        prefixed.getFileAsText("products.json");
        prefixed.getProperties("products.json");

        verify(delegate).getFileAsText("onboarding/products.json");
        verify(delegate).getProperties("onboarding/products.json");
    }

    @Test
    void wrap_normalizesTrailingSlashInPrefix() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);
        AzureBlobClient prefixed = PrefixingAzureBlobClient.wrap(delegate, "onboarding/");

        prefixed.getFileAsText("products.json");

        verify(delegate).getFileAsText("onboarding/products.json");
    }

    @Test
    void prefixed_returnsPrefixWhenPathIsBlank() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);
        AzureBlobClient prefixed = PrefixingAzureBlobClient.wrap(delegate, "onboarding");

        prefixed.getFileAsText(null);
        prefixed.getProperties("");

        verify(delegate).getFileAsText("onboarding");
        verify(delegate).getProperties("onboarding");
    }

    @Test
    void prefixed_doesNotDoublePrefixWhenPathAlreadyPrefixed() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);
        AzureBlobClient prefixed = PrefixingAzureBlobClient.wrap(delegate, "onboarding");

        prefixed.getFileAsText("onboarding/products.json");
        prefixed.getFileAsText("onboarding");

        verify(delegate).getFileAsText("onboarding/products.json");
        verify(delegate).getFileAsText("onboarding");
    }

    @Test
    void retrieveFile_delegatesWithPrefixedPath() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);
        File expected = new File("contract.pdf");
        when(delegate.retrieveFile("onboarding/contract.pdf")).thenReturn(expected);
        AzureBlobClient prefixed = PrefixingAzureBlobClient.wrap(delegate, "onboarding");

        assertSame(expected, prefixed.retrieveFile("contract.pdf"));
    }

    @Test
    void getFile_delegatesWithPrefixedPath() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);
        byte[] expected = new byte[] {1, 2, 3};
        when(delegate.getFile("onboarding/data.bin")).thenReturn(expected);
        AzureBlobClient prefixed = PrefixingAzureBlobClient.wrap(delegate, "onboarding");

        assertSame(expected, prefixed.getFile("data.bin"));
    }

    @Test
    void getFileAsPdf_delegatesWithPrefixedPath() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);
        File expected = new File("contract.pdf");
        when(delegate.getFileAsPdf("onboarding/contract-template")).thenReturn(expected);
        AzureBlobClient prefixed = PrefixingAzureBlobClient.wrap(delegate, "onboarding");

        assertSame(expected, prefixed.getFileAsPdf("contract-template"));
    }

    @Test
    void uploadFile_delegatesWithPrefixedPath() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);
        byte[] data = new byte[] {4, 5, 6};
        when(delegate.uploadFile("onboarding/dir", "file.txt", data)).thenReturn("onboarding/dir/file.txt");
        AzureBlobClient prefixed = PrefixingAzureBlobClient.wrap(delegate, "onboarding");

        assertEquals("onboarding/dir/file.txt", prefixed.uploadFile("dir", "file.txt", data));
    }

    @Test
    void uploadFilePath_delegatesWithPrefixedPath() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);
        byte[] data = new byte[] {7, 8, 9};
        when(delegate.uploadFilePath("onboarding/dir/file.txt", data)).thenReturn("onboarding/dir/file.txt");
        AzureBlobClient prefixed = PrefixingAzureBlobClient.wrap(delegate, "onboarding");

        assertEquals("onboarding/dir/file.txt", prefixed.uploadFilePath("dir/file.txt", data));
    }

    @Test
    void removeFile_delegatesWithPrefixedPath() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);
        AzureBlobClient prefixed = PrefixingAzureBlobClient.wrap(delegate, "onboarding");

        prefixed.removeFile("obsolete.json");

        verify(delegate).removeFile("onboarding/obsolete.json");
    }

    @Test
    void getProperties_delegatesWithPrefixedPath() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);
        BlobProperties properties = new BlobProperties(
                java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(), null, 0L, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, java.util.Map.of(), null);
        when(delegate.getProperties("onboarding/products.json")).thenReturn(properties);
        AzureBlobClient prefixed = PrefixingAzureBlobClient.wrap(delegate, "onboarding");

        assertSame(properties, prefixed.getProperties("products.json"));
    }

    @Test
    void getFilesWithoutArgs_delegatesUsingPathPrefixOnly() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);
        List<String> expected = List.of("onboarding/a.json", "onboarding/b.json");
        when(delegate.getFiles("onboarding")).thenReturn(expected);
        AzureBlobClient prefixed = PrefixingAzureBlobClient.wrap(delegate, "onboarding");

        assertSame(expected, prefixed.getFiles());
    }

    @Test
    void getFilesWithPath_delegatesWithPrefixedPath() {
        AzureBlobClient delegate = mock(AzureBlobClient.class);
        List<String> expected = List.of("onboarding/sub/a.json");
        when(delegate.getFiles("onboarding/sub")).thenReturn(expected);
        AzureBlobClient prefixed = PrefixingAzureBlobClient.wrap(delegate, "onboarding");

        assertSame(expected, prefixed.getFiles("sub"));
    }
}

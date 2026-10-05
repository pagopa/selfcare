package it.pagopa.selfcare.onboarding.storage;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PrefixingAzureBlobClientTest {

    private AzureBlobClient delegate;

    @BeforeEach
    void setUp() {
        delegate = mock(AzureBlobClient.class);
    }

    @Test
    void emptyPrefixKeepsNormalizedPath() {
        PrefixingAzureBlobClient client = new PrefixingAzureBlobClient(delegate, "");
        when(delegate.getFileAsText("contracts/template/mail/a.json")).thenReturn("body");

        assertEquals("body", client.getFileAsText("./contracts//template\\mail/a.json"));
    }

    @Test
    void blankAndNullPrefixAreTreatedAsEmpty() {
        assertEquals("a/b.json", new PrefixingAzureBlobClient(delegate, null).prefixed("a/b.json"));
        assertEquals("a/b.json", new PrefixingAzureBlobClient(delegate, "  ").prefixed("a/b.json"));
    }

    @Test
    void prefixIsPrependedToEveryRead() {
        PrefixingAzureBlobClient client = new PrefixingAzureBlobClient(delegate, "tenant-a/");
        when(delegate.getFileAsText("tenant-a/contracts/a.json")).thenReturn("body");
        when(delegate.getFile("tenant-a/logo.png")).thenReturn(new byte[] {1});

        assertEquals("body", client.getFileAsText("contracts/a.json"));
        assertEquals(1, client.getFile("logo.png").length);
    }

    @Test
    void uploadReturnsPathWithoutPrefix() {
        PrefixingAzureBlobClient client = new PrefixingAzureBlobClient(delegate, "tenant-a");
        when(delegate.uploadFile("tenant-a/dir", "f.pdf", new byte[] {1})).thenReturn("tenant-a/dir/f.pdf");

        assertEquals("dir/f.pdf", client.uploadFile("dir", "f.pdf", new byte[] {1}));
    }

    @Test
    void listingIsConfinedToThePrefix() {
        PrefixingAzureBlobClient client = new PrefixingAzureBlobClient(delegate, "tenant-a");
        when(delegate.getFiles("tenant-a/")).thenReturn(List.of("tenant-a/x.json", "tenant-a2/y.json"));
        when(delegate.getFiles("tenant-a/docs")).thenReturn(List.of("tenant-a/docs/z.json"));

        assertEquals(List.of("x.json"), client.getFiles());
        assertEquals(List.of("docs/z.json"), client.getFiles("docs"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../a.json", "a/../../b.json", "a/..", "/abs/a.json", "\\abs\\a.json",
            "C:\\a.json", "c:/a.json", "a\u0000b.json", "a\nb.json", "a\u007Fb.json"})
    void rejectsUnsafePaths(String path) {
        PrefixingAzureBlobClient client = new PrefixingAzureBlobClient(delegate, "tenant-a");

        assertThrows(IllegalArgumentException.class, () -> client.getFileAsText(path));
        assertThrows(IllegalArgumentException.class, () -> client.getFile(path));
        verifyNoInteractions(delegate);
    }

    @Test
    void rejectsNullPath() {
        PrefixingAzureBlobClient client = new PrefixingAzureBlobClient(delegate, "");

        assertThrows(IllegalArgumentException.class, () -> client.getFileAsText(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "a/../b", "/abs", "C:\\x", "a\u0000"})
    void rejectsUnsafePrefix(String prefix) {
        assertThrows(IllegalArgumentException.class, () -> new PrefixingAzureBlobClient(delegate, prefix));
    }

    @Test
    void removeFileUsesPrefixedPath() {
        new PrefixingAzureBlobClient(delegate, "tenant-a").removeFile("a.json");

        verify(delegate).removeFile("tenant-a/a.json");
    }
}

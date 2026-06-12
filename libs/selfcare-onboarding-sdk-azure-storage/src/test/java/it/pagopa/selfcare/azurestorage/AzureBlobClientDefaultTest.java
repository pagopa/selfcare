package it.pagopa.selfcare.azurestorage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ListBlobsOptions;
import it.pagopa.selfcare.azurestorage.error.SelfcareAzureStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureBlobClientDefaultTest {

    private static final String CONTAINER_NAME = "container";
    private static final String FILE_PATH = "folder/file.txt";

    private BlobContainerClient blobContainerClient;
    private BlobClient blobClient;
    private AzureBlobClientDefault client;

    @BeforeEach
    void setUp() {
        BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        blobContainerClient = mock(BlobContainerClient.class);
        blobClient = mock(BlobClient.class);

        when(blobServiceClient.getBlobContainerClient(CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);

        try (MockedConstruction<BlobServiceClientBuilder> ignored = stubBuilder(blobServiceClient)) {
            client = new AzureBlobClientDefault(CONTAINER_NAME, "acct", null);
        }
    }

    private static MockedConstruction<BlobServiceClientBuilder> stubBuilder(BlobServiceClient built) {
        return mockConstruction(BlobServiceClientBuilder.class, (mock, ctx) -> {
            when(mock.endpoint(anyString())).thenReturn(mock);
            when(mock.credential(any(com.azure.core.credential.TokenCredential.class))).thenReturn(mock);
            when(mock.connectionString(anyString())).thenReturn(mock);
            when(mock.buildClient()).thenReturn(built);
        });
    }

    private BlobStorageException blobStorageException(int status) {
        BlobStorageException ex = mock(BlobStorageException.class);
        when(ex.getStatusCode()).thenReturn(status);
        return ex;
    }

    // ---------------- Constructor tests ----------------

    @Test
    void constructor_withAccountNameOnly_buildsClient() {
        // given
        BlobServiceClient built = mock(BlobServiceClient.class);
        try (MockedConstruction<BlobServiceClientBuilder> mc = stubBuilder(built)) {
            // when
            AzureBlobClientDefault c = new AzureBlobClientDefault(CONTAINER_NAME, "acct", null);
            // then
            assertNotNull(c);
            assertEquals(1, mc.constructed().size());
            verify(mc.constructed().get(0)).endpoint("https://acct.blob.core.windows.net");
            verify(mc.constructed().get(0)).buildClient();
        }
    }

    @Test
    void constructor_withAccountNameAndManagedIdentity_buildsClient() {
        // given
        BlobServiceClient built = mock(BlobServiceClient.class);
        try (MockedConstruction<BlobServiceClientBuilder> mc = stubBuilder(built)) {
            // when
            AzureBlobClientDefault c = new AzureBlobClientDefault(CONTAINER_NAME, "acct", "mi-client-id");
            // then
            assertNotNull(c);
            verify(mc.constructed().get(0)).endpoint("https://acct.blob.core.windows.net");
        }
    }

    @Test
    void constructor_withTrimmedAccountName_buildsClient() {
        // given
        BlobServiceClient built = mock(BlobServiceClient.class);
        try (MockedConstruction<BlobServiceClientBuilder> mc = stubBuilder(built)) {
            // when
            AzureBlobClientDefault c = new AzureBlobClientDefault(CONTAINER_NAME, "  acct  ", "");
            // then
            assertNotNull(c);
            verify(mc.constructed().get(0)).endpoint("https://acct.blob.core.windows.net");
        }
    }

    @Test
    void constructor_withoutAccountName_throws() {
        // given / when / then
        SelfcareAzureStorageException ex = assertThrows(SelfcareAzureStorageException.class,
                () -> new AzureBlobClientDefault(CONTAINER_NAME, "  ", ""));
        assertEquals("BLOB_CONFIG_ERROR", ex.getCode());
    }

    @Test
    void constructor_withNullAccountName_throws() {
        // given / when / then
        SelfcareAzureStorageException ex = assertThrows(SelfcareAzureStorageException.class,
                () -> new AzureBlobClientDefault(CONTAINER_NAME, null, "mi-client-id"));
        assertEquals("BLOB_CONFIG_ERROR", ex.getCode());
    }

    @Test
    void constructor_withConnectionString_buildsClient() {
        // given
        BlobServiceClient built = mock(BlobServiceClient.class);
        try (MockedConstruction<BlobServiceClientBuilder> mc = stubBuilder(built)) {
            // when
            AzureBlobClientDefault c = new AzureBlobClientDefault("UseDevelopmentStorage=true", CONTAINER_NAME);
            // then
            assertNotNull(c);
            verify(mc.constructed().get(0)).connectionString("UseDevelopmentStorage=true");
            verify(mc.constructed().get(0)).buildClient();
        }
    }

    // ---------------- getFile ----------------

    @Test
    void getFile_returnsBytes() {
        // given
        byte[] expected = "hello".getBytes();
        doAnswer(inv -> {
            OutputStream os = inv.getArgument(0);
            os.write(expected);
            return null;
        }).when(blobClient).downloadStream(any(ByteArrayOutputStream.class));

        // when
        byte[] result = client.getFile(FILE_PATH);

        // then
        assertArrayEquals(expected, result);
    }

    @Test
    void getFile_whenNotFound_throws() {
        // given
        doThrow(blobStorageException(404)).when(blobClient).downloadStream(any(OutputStream.class));
        // when / then
        assertThrows(SelfcareAzureStorageException.class, () -> client.getFile(FILE_PATH));
    }

    @Test
    void getFile_whenOtherError_throws() {
        // given
        doThrow(blobStorageException(500)).when(blobClient).downloadStream(any(OutputStream.class));
        // when / then
        assertThrows(SelfcareAzureStorageException.class, () -> client.getFile(FILE_PATH));
    }

    // ---------------- getFileAsText ----------------

    @Test
    void getFileAsText_returnsText() {
        // given
        when(blobClient.downloadContent()).thenReturn(BinaryData.fromString("body"));
        // when
        String result = client.getFileAsText(FILE_PATH);
        // then
        assertEquals("body", result);
    }

    @Test
    void getFileAsText_whenError_throws() {
        // given
        BlobStorageException ex = blobStorageException(500);
        when(blobClient.downloadContent()).thenThrow(ex);
        // when / then
        assertThrows(SelfcareAzureStorageException.class, () -> client.getFileAsText(FILE_PATH));
    }

    // ---------------- getFileAsPdf ----------------

    @Test
    void getFileAsPdf_returnsTempFile() {
        // given
        doAnswer(inv -> null).when(blobClient).downloadToFile(anyString(), anyBoolean());
        // when
        File result = client.getFileAsPdf(FILE_PATH);
        // then
        assertNotNull(result);
        assertTrue(result.getName().endsWith(".pdf"));
        result.deleteOnExit();
    }

    @Test
    void getFileAsPdf_whenError_throws() {
        // given
        doThrow(blobStorageException(500)).when(blobClient).downloadToFile(anyString(), anyBoolean());
        // when / then
        assertThrows(SelfcareAzureStorageException.class, () -> client.getFileAsPdf(FILE_PATH));
    }

    // ---------------- retrieveFile ----------------

    @Test
    void retrieveFile_withExtension_returnsTempFile() {
        // given
        doAnswer(inv -> null).when(blobClient).downloadToFile(anyString(), anyBoolean());
        // when
        File result = client.retrieveFile("dir/file.json");
        // then
        assertNotNull(result);
        result.deleteOnExit();
    }

    @Test
    void retrieveFile_withoutExtension_defaultsToPdf() {
        // given
        doAnswer(inv -> null).when(blobClient).downloadToFile(anyString(), anyBoolean());
        // when
        File result = client.retrieveFile("noext");
        // then
        assertNotNull(result);
        result.deleteOnExit();
    }

    @Test
    void retrieveFile_whenError_throws() {
        // given
        doThrow(blobStorageException(500)).when(blobClient).downloadToFile(anyString(), anyBoolean());
        // when / then
        assertThrows(SelfcareAzureStorageException.class, () -> client.retrieveFile(FILE_PATH));
    }

    // ---------------- uploadFile ----------------

    @Test
    void uploadFile_returnsFilepath() {
        // given
        byte[] data = "x".getBytes();
        // when
        String result = client.uploadFile("dir", "f.txt", data);
        // then
        assertEquals(java.nio.file.Paths.get("dir", "f.txt").toString(), result);
        ArgumentCaptor<BinaryData> captor = ArgumentCaptor.forClass(BinaryData.class);
        verify(blobClient).upload(captor.capture(), eq(true));
        assertArrayEquals(data, captor.getValue().toBytes());
    }

    @Test
    void uploadFile_whenError_throws() {
        // given
        doThrow(blobStorageException(500)).when(blobClient).upload(any(BinaryData.class), anyBoolean());
        // when / then
        assertThrows(SelfcareAzureStorageException.class,
                () -> client.uploadFile("dir", "f.txt", new byte[]{1}));
    }

    // ---------------- uploadFilePath ----------------

    @Test
    void uploadFilePath_returnsFilepath() {
        // when
        String result = client.uploadFilePath(FILE_PATH, new byte[]{1});
        // then
        assertEquals(FILE_PATH, result);
        verify(blobClient).upload(any(BinaryData.class), eq(true));
    }

    @Test
    void uploadFilePath_whenError_throws() {
        // given
        doThrow(blobStorageException(500)).when(blobClient).upload(any(BinaryData.class), anyBoolean());
        // when / then
        assertThrows(SelfcareAzureStorageException.class,
                () -> client.uploadFilePath(FILE_PATH, new byte[]{1}));
    }

    // ---------------- removeFile ----------------

    @Test
    void removeFile_invokesDelete() {
        // when
        assertDoesNotThrow(() -> client.removeFile(FILE_PATH));
        // then
        verify(blobClient).deleteIfExists();
    }

    @Test
    void removeFile_whenError_throws() {
        // given
        doThrow(blobStorageException(500)).when(blobClient).deleteIfExists();
        // when / then
        assertThrows(SelfcareAzureStorageException.class, () -> client.removeFile(FILE_PATH));
    }

    // ---------------- getProperties ----------------

    @Test
    void getProperties_returnsProps() {
        // given
        BlobProperties props = mock(BlobProperties.class);
        when(blobClient.getProperties()).thenReturn(props);
        // when
        BlobProperties result = client.getProperties(FILE_PATH);
        // then
        assertEquals(props, result);
    }

    @Test
    void getProperties_whenNotFound_throws() {
        // given
        BlobStorageException ex = blobStorageException(404);
        when(blobClient.getProperties()).thenThrow(ex);
        // when / then
        assertThrows(SelfcareAzureStorageException.class, () -> client.getProperties(FILE_PATH));
    }

    @Test
    void getProperties_whenOtherError_throws() {
        // given
        BlobStorageException ex = blobStorageException(500);
        when(blobClient.getProperties()).thenThrow(ex);
        // when / then
        assertThrows(SelfcareAzureStorageException.class, () -> client.getProperties(FILE_PATH));
    }

    // ---------------- getFiles ----------------

    @Test
    @SuppressWarnings("unchecked")
    void getFiles_returnsAllNames() {
        // given
        BlobItem b1 = mock(BlobItem.class);
        when(b1.getName()).thenReturn("a");
        BlobItem b2 = mock(BlobItem.class);
        when(b2.getName()).thenReturn("b");
        PagedIterable<BlobItem> paged = mock(PagedIterable.class);
        doAnswer(inv -> {
            Consumer<BlobItem> c = inv.getArgument(0);
            c.accept(b1);
            c.accept(b2);
            return null;
        }).when(paged).forEach(any(Consumer.class));
        when(blobContainerClient.listBlobs()).thenReturn(paged);

        // when
        List<String> result = client.getFiles();

        // then
        assertEquals(List.of("a", "b"), result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFilesByPath_returnsFilteredNames() {
        // given
        BlobItem b1 = mock(BlobItem.class);
        when(b1.getName()).thenReturn("dir/a");
        PagedIterable<BlobItem> paged = mock(PagedIterable.class);
        doAnswer(inv -> {
            Consumer<BlobItem> c = inv.getArgument(0);
            c.accept(b1);
            return null;
        }).when(paged).forEach(any(Consumer.class));
        when(blobContainerClient.listBlobs(any(ListBlobsOptions.class), eq(null))).thenReturn(paged);

        // when
        List<String> result = client.getFiles("dir/\n\r");

        // then
        assertEquals(List.of("dir/a"), result);
        ArgumentCaptor<ListBlobsOptions> optionsCaptor = ArgumentCaptor.forClass(ListBlobsOptions.class);
        verify(blobContainerClient).listBlobs(optionsCaptor.capture(), eq(null));
        assertEquals("dir/", optionsCaptor.getValue().getPrefix());
    }

    @Test
    void getFilesByPath_emptyPath_returnsEmpty() {
        // when
        List<String> result = client.getFiles("");
        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void getFilesByPath_nullPath_returnsEmpty() {
        // when
        List<String> result = client.getFiles(null);
        // then
        assertTrue(result.isEmpty());
    }
}


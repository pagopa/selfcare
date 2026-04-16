package it.pagopa.selfcare.party.connector.azure_storage.client;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ProxyRegistryException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AzureBlobClientTest {

    private final String filename = "logo-pagopa-spa.png";
    private AzureBlobClient blobClient;
    @BeforeEach
    public void init() throws URISyntaxException, InvalidKeyException {
        blobClient = new AzureBlobClient("UseDevelopmentStorage=true;", "$web");
    }

    @Test
    void getFileOk() throws URISyntaxException, IOException, NoSuchFieldException, IllegalAccessException, StorageException{
        //given
        CloudBlockBlob blockBlobMock = Mockito.mock(CloudBlockBlob.class);
        Mockito.when(blockBlobMock.getProperties())
                .thenReturn(new BlobProperties());
        Mockito.doNothing().
                when(blockBlobMock).upload(any(), Mockito.anyByte());
        CloudBlobContainer blobContainerMock = Mockito.mock(CloudBlobContainer.class);
        Mockito.when(blobContainerMock.getBlockBlobReference(Mockito.anyString()))
                .thenReturn(blockBlobMock);
        CloudBlobClient blobClientMock = Mockito.mock(CloudBlobClient.class);
        Mockito.when(blobClientMock.getContainerReference("$web"))
                .thenReturn(blobContainerMock);
        mockCloudBlobClient(blobClient, blobClientMock);

        Executable executable = () -> blobClient.getFile(filename);
        // then
        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    void getFileKO() throws URISyntaxException, NoSuchFieldException, IllegalAccessException, StorageException {
        //given
        CloudStorageAccount storageAccountMock = mock(CloudStorageAccount.class);
        CloudBlobClient blobClientMock = mock(CloudBlobClient.class);
        CloudBlobContainer blobContainerMock = mock(CloudBlobContainer.class);
        CloudBlockBlob blobMock = mock(CloudBlockBlob.class);
        BlobProperties propertiesMock = mock(BlobProperties.class);

        when(storageAccountMock.createCloudBlobClient()).thenReturn(blobClientMock);
        when(blobClientMock.getContainerReference(anyString())).thenReturn(blobContainerMock);
        when(blobContainerMock.getBlockBlobReference(anyString())).thenReturn(blobMock);
        when(blobMock.getProperties()).thenReturn(propertiesMock);
        when(blobMock.getName()).thenReturn(filename);

        mockCloudBlobClient(blobClient, blobClientMock);

        doThrow(new StorageException("1000" ,"Not found", 404, null, null))
                .when(blobMock).download(any(ByteArrayOutputStream.class));
        //when
        Executable executable = () -> blobClient.getFile(filename);
        //then
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, executable);
        assertEquals(String.format("Error during download file %s", filename), e.getMessage());

    }

    @Test
    void getFile500() throws URISyntaxException, NoSuchFieldException, IllegalAccessException, StorageException {
        //given
        CloudStorageAccount storageAccountMock = mock(CloudStorageAccount.class);
        CloudBlobClient blobClientMock = mock(CloudBlobClient.class);
        CloudBlobContainer blobContainerMock = mock(CloudBlobContainer.class);
        CloudBlockBlob blobMock = mock(CloudBlockBlob.class);
        BlobProperties propertiesMock = mock(BlobProperties.class);

        when(storageAccountMock.createCloudBlobClient()).thenReturn(blobClientMock);
        when(blobClientMock.getContainerReference(anyString())).thenReturn(blobContainerMock);
        when(blobContainerMock.getBlockBlobReference(anyString())).thenReturn(blobMock);
        when(blobMock.getProperties()).thenReturn(propertiesMock);
        when(blobMock.getName()).thenReturn(filename);

        mockCloudBlobClient(blobClient, blobClientMock);

        doThrow(new StorageException("1000" ,"Not found", 500, null, null))
                .when(blobMock).download(any(ByteArrayOutputStream.class));
        //when
        Executable executable = () -> blobClient.getFile(filename);
        //then
        ProxyRegistryException e = assertThrows(ProxyRegistryException.class, executable);
        assertEquals(String.format("Error during download file %s", filename), e.getMessage());

    }

    @Test
    void uploadFileOk() throws URISyntaxException, StorageException, NoSuchFieldException, IllegalAccessException, IOException {
        CloudBlockBlob blockBlobMock = Mockito.mock(CloudBlockBlob.class);
        Mockito.when(blockBlobMock.getProperties())
                .thenReturn(new BlobProperties());
        CloudBlobContainer blobContainerMock = Mockito.mock(CloudBlobContainer.class);
        Mockito.when(blobContainerMock.getBlockBlobReference(Mockito.anyString()))
                .thenReturn(blockBlobMock);
        CloudBlobClient blobClientMock = Mockito.mock(CloudBlobClient.class);
        Mockito.when(blobClientMock.getContainerReference("$web"))
                .thenReturn(blobContainerMock);
        Mockito.doNothing().
                when(blockBlobMock).upload(any(), Mockito.anyByte());

        mockCloudBlobClient(blobClient, blobClientMock);

        Executable executable = () -> blobClient.uploadFile(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)), filename);
        // then
        Assertions.assertDoesNotThrow(executable);
    }

    private void mockCloudBlobClient(AzureBlobClient blobClient, CloudBlobClient blobClientMock) throws NoSuchFieldException, IllegalAccessException {
        Field field = AzureBlobClient.class.getDeclaredField("blobClient");
        field.setAccessible(true);
        field.set(blobClient, blobClientMock);
    }

}

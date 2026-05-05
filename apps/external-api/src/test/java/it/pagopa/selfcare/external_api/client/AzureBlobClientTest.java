package it.pagopa.selfcare.external_api.client;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import it.pagopa.selfcare.external_api.exception.AzureRestClientException;
import it.pagopa.selfcare.external_api.exception.ResourceNotFoundException;
import it.pagopa.selfcare.external_api.model.document.ResourceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import static it.pagopa.selfcare.external_api.model.constant.GenericError.ERROR_DURING_DOWNLOAD_FILE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AzureBlobClientTest {

    @Test
    void testGetFile_Success() throws Exception {
        // given
        String fileName = "your_file_name";
        byte[] fileData = "your_file_data".getBytes();
        CloudStorageAccount storageAccountMock = mock(CloudStorageAccount.class);
        CloudBlobClient blobClientMock = mock(CloudBlobClient.class);
        CloudBlobContainer blobContainerMock = mock(CloudBlobContainer.class);
        CloudBlockBlob blobMock = mock(CloudBlockBlob.class);
        BlobProperties propertiesMock = mock(BlobProperties.class);

        when(storageAccountMock.createCloudBlobClient()).thenReturn(blobClientMock);
        when(blobClientMock.getContainerReference(anyString())).thenReturn(blobContainerMock);
        when(blobContainerMock.getBlockBlobReference(anyString())).thenReturn(blobMock);
        when(blobMock.getProperties()).thenReturn(propertiesMock);
        when(blobMock.getName()).thenReturn(fileName);

        String connectionString = "UseDevelopmentStorage=true;";
        String containerReference = "your_container_reference";
        AzureBlobClient azureBlobClient = new AzureBlobClient(connectionString, containerReference);
        mockCloudBlobClient(azureBlobClient, blobClientMock);


        ByteArrayOutputStream outputStreamMock = new ByteArrayOutputStream();
        when(blobMock.getProperties()).thenReturn(propertiesMock);
        when(propertiesMock.getContentType()).thenReturn("text/plain");
        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(0);
            outputStream.write(fileData);
            return null;
        }).when(blobMock).download(any());

        // when
        ResourceResponse response = azureBlobClient.getFile(fileName);

        // then
        assertEquals(fileName, response.getFileName());
        assertEquals("text/plain", response.getMimetype());
        assertArrayEquals(fileData, response.getData());
        verify(blobClientMock).getContainerReference(containerReference);
        verify(blobContainerMock).getBlockBlobReference(fileName);
        verify(blobMock).download(notNull());
    }

    @Test
    void getFile_notFound() throws URISyntaxException, StorageException, InvalidKeyException, NoSuchFieldException, IllegalAccessException {
        // given
        String fileName = "your_file_name";
        byte[] fileData = "your_file_data".getBytes();
        CloudStorageAccount storageAccountMock = mock(CloudStorageAccount.class);
        CloudBlobClient blobClientMock = mock(CloudBlobClient.class);
        CloudBlobContainer blobContainerMock = mock(CloudBlobContainer.class);
        CloudBlockBlob blobMock = mock(CloudBlockBlob.class);
        BlobProperties propertiesMock = mock(BlobProperties.class);

        when(storageAccountMock.createCloudBlobClient()).thenReturn(blobClientMock);
        when(blobClientMock.getContainerReference(anyString())).thenReturn(blobContainerMock);
        when(blobContainerMock.getBlockBlobReference(anyString())).thenReturn(blobMock);
        when(blobMock.getProperties()).thenReturn(propertiesMock);
        when(blobMock.getName()).thenReturn(fileName);

        String connectionString = "UseDevelopmentStorage=true;";
        String containerReference = "your_container_reference";
        AzureBlobClient azureBlobClient = new AzureBlobClient(connectionString, containerReference);
        mockCloudBlobClient(azureBlobClient, blobClientMock);
        doThrow(new StorageException("1000" ,"Not found", 404, null, null))
                .when(blobMock).download(any(ByteArrayOutputStream.class));

        //when
        Executable executable = () -> azureBlobClient.getFile(fileName);
        //then
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, executable);
        assertEquals(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), fileName), e.getMessage());
    }
    @Test
    void getFile_otherException() throws URISyntaxException, StorageException, InvalidKeyException, NoSuchFieldException, IllegalAccessException {
        // given
        String fileName = "your_file_name";
        byte[] fileData = "your_file_data".getBytes();
        CloudStorageAccount storageAccountMock = mock(CloudStorageAccount.class);
        CloudBlobClient blobClientMock = mock(CloudBlobClient.class);
        CloudBlobContainer blobContainerMock = mock(CloudBlobContainer.class);
        CloudBlockBlob blobMock = mock(CloudBlockBlob.class);
        BlobProperties propertiesMock = mock(BlobProperties.class);

        when(storageAccountMock.createCloudBlobClient()).thenReturn(blobClientMock);
        when(blobClientMock.getContainerReference(anyString())).thenReturn(blobContainerMock);
        when(blobContainerMock.getBlockBlobReference(anyString())).thenReturn(blobMock);
        when(blobMock.getProperties()).thenReturn(propertiesMock);
        when(blobMock.getName()).thenReturn(fileName);

        String connectionString = "UseDevelopmentStorage=true;";
        String containerReference = "your_container_reference";
        AzureBlobClient azureBlobClient = new AzureBlobClient(connectionString, containerReference);
        mockCloudBlobClient(azureBlobClient, blobClientMock);
        doThrow(new StorageException("1000" ,"InternalError", 500, null, null))
                .when(blobMock).download(any(ByteArrayOutputStream.class));

        //when
        Executable executable = () -> azureBlobClient.getFile(fileName);
        //then
        AzureRestClientException e = assertThrows(AzureRestClientException.class, executable);
        assertEquals(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), fileName), e.getMessage());
    }

    @Test
    void getFile_uriException() throws URISyntaxException, StorageException, InvalidKeyException, NoSuchFieldException, IllegalAccessException {
        // given
        String fileName = "your_file_name";
        byte[] fileData = "your_file_data".getBytes();
        CloudStorageAccount storageAccountMock = mock(CloudStorageAccount.class);
        CloudBlobClient blobClientMock = mock(CloudBlobClient.class);
        CloudBlobContainer blobContainerMock = mock(CloudBlobContainer.class);
        CloudBlockBlob blobMock = mock(CloudBlockBlob.class);
        BlobProperties propertiesMock = mock(BlobProperties.class);

        when(storageAccountMock.createCloudBlobClient()).thenReturn(blobClientMock);
        doThrow(URISyntaxException.class).when(blobClientMock).getContainerReference(anyString());
        when(blobContainerMock.getBlockBlobReference(anyString())).thenReturn(blobMock);
        when(blobMock.getProperties()).thenReturn(propertiesMock);
        when(blobMock.getName()).thenReturn(fileName);

        String connectionString = "UseDevelopmentStorage=true;";
        String containerReference = "your_container_reference";
        AzureBlobClient azureBlobClient = new AzureBlobClient(connectionString, containerReference);
        mockCloudBlobClient(azureBlobClient, blobClientMock);
        //when
        Executable executable = () -> azureBlobClient.getFile(fileName);
        //then
        AzureRestClientException e = assertThrows(AzureRestClientException.class, executable);
        assertEquals(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), fileName), e.getMessage());
    }

    private void mockCloudBlobClient(AzureBlobClient blobClient, CloudBlobClient blobClientMock) throws NoSuchFieldException, IllegalAccessException {
        Field field = AzureBlobClient.class.getDeclaredField("blobClient");
        field.setAccessible(true);
        field.set(blobClient, blobClientMock);
    }

}
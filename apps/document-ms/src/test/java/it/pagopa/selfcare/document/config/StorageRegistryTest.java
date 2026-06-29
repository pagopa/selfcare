package it.pagopa.selfcare.document.config;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.document.model.StorageOrigin;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class StorageRegistryTest {

    @Inject
    StorageRegistry storageRegistry;

    @InjectMock
    AzureBlobClient azureBlobClient;

    @Test
    void clientFor_whenOriginIsSystem_shouldDelegateToSystemClient() {
        // given
        File mockFile = mock(File.class);
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(mockFile);

        // when
        AzureBlobClient client = storageRegistry.clientFor(StorageOrigin.SYSTEM);
        File result = client.getFileAsPdf("any/path");

        // then
        assertNotNull(client);
        assertSame(mockFile, result);
        verify(azureBlobClient).getFileAsPdf("any/path");
    }

    @Test
    void clientFor_whenOriginIsNull_shouldFallbackToSystemClient() {
        // given
        File mockFile = mock(File.class);
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(mockFile);

        // when
        AzureBlobClient client = storageRegistry.clientFor(null);
        File result = client.getFileAsPdf("any/path");

        // then
        assertNotNull(client);
        assertSame(mockFile, result);
        verify(azureBlobClient).getFileAsPdf("any/path");
    }

    @Test
    void clientFor_whenOriginIsUser_andUserStorageNotConfigured_shouldFallbackToSystemClient() {
        // given
        File mockFile = mock(File.class);
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(mockFile);

        // when
        AzureBlobClient userClient = storageRegistry.clientFor(StorageOrigin.USER);
        File result = userClient.getFileAsPdf("any/path");

        // then
        assertNotNull(userClient);
        assertSame(mockFile, result);
        verify(azureBlobClient).getFileAsPdf("any/path");
    }

    @Test
    void clientFor_nullAndSystem_shouldBothDelegateToSameClient() {
        // given
        File mockFile = mock(File.class);
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(mockFile);

        // when
        File fromSystem = storageRegistry.clientFor(StorageOrigin.SYSTEM).getFileAsPdf("path");
        File fromNull = storageRegistry.clientFor(null).getFileAsPdf("path");

        // then
        assertSame(fromSystem, fromNull);
        verify(azureBlobClient, times(2)).getFileAsPdf("path");
    }
}

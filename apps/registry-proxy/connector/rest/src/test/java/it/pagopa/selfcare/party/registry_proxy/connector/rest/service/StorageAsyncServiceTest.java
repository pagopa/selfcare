package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import it.pagopa.selfcare.commons.tenant.UnknownStorageException;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClient;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClientProvider;
import org.junit.jupiter.api.Test;

class StorageAsyncServiceTest {

  @Test
  void archivalUsesThePersistedTenantRatherThanAWorkerThreadContext() {
    var provider = mock(TenantBlobClientProvider.class);
    var ar = mock(TenantBlobClient.class);
    var pnpg = mock(TenantBlobClient.class);
    var arBlob = mock(BlobClient.class);
    var pnpgBlob = mock(BlobClient.class);
    when(provider.clientFor("AR", "visure")).thenReturn(ar);
    when(provider.clientFor("PNPG", "visure")).thenReturn(pnpg);
    when(ar.blob("document.xml")).thenReturn(arBlob);
    when(pnpg.blob("document.xml")).thenReturn(pnpgBlob);
    var service = new StorageAsyncService(provider);
    service.saveStringToStorage("AR", "ar-document", "document.xml").join();
    service.saveStringToStorage("PNPG", "pnpg-document", "document.xml").join();
    verify(arBlob).upload(argThat((BinaryData data) -> "ar-document".equals(data.toString())), eq(true));
    verify(pnpgBlob).upload(argThat((BinaryData data) -> "pnpg-document".equals(data.toString())), eq(true));
  }

  @Test
  void missingBindingFailsInsteadOfUsingTheLegacyDaprStore() {
    var provider = mock(TenantBlobClientProvider.class);
    when(provider.clientFor("PNPG", "visure")).thenThrow(new UnknownStorageException("PNPG", "visure"));
    assertThrows(UnknownStorageException.class,
        () -> new StorageAsyncService(provider).saveStringToStorage("PNPG", "document", "document.xml"));
  }
}

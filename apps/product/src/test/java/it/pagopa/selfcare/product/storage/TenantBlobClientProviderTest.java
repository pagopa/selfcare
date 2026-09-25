package it.pagopa.selfcare.product.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.BlobServiceAsyncClient;
import it.pagopa.selfcare.tenant.StorageAuthenticationType;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.UnknownStorageException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantBlobClientProviderTest {

  private TenantRegistry tenantRegistry;
  private TenantContext tenantContext;
  private BlobServiceAsyncClient createdClient;
  private TenantBlobClientProvider provider;

  @BeforeEach
  void setUp() {
    tenantRegistry = mock(TenantRegistry.class);
    tenantContext = mock(TenantContext.class);
    createdClient = mock(BlobServiceAsyncClient.class);
    provider =
        new TenantBlobClientProvider(tenantRegistry, tenantContext) {
          @Override
          protected BlobServiceAsyncClient createClient(
              String tenantId,
              String logicalStorageKey,
              TenantDefinition.StorageDefinition storage) {
            return createdClient;
          }
        };
  }

  @Test
  void clientFor_reusesCachedClientForSameAccountAndCredentials() {
    TenantDefinition.StorageDefinition contracts = storage("st-ar", "contracts", "BLOB_AR");
    when(tenantRegistry.storage("AR", StorageKeys.CONTRACTS)).thenReturn(contracts);

    TenantBlobBinding first = provider.clientFor("AR", StorageKeys.CONTRACTS);
    TenantBlobBinding second = provider.clientFor("AR", StorageKeys.CONTRACTS);

    assertSame(createdClient, first.client());
    assertSame(first.client(), second.client());
    assertEquals("contracts", first.container());
  }

  @Test
  void clientFor_doesNotReuseClientForDifferentTenantAccount() {
    BlobServiceAsyncClient pnpgClient = mock(BlobServiceAsyncClient.class);
    TenantBlobClientProvider multiProvider =
        new TenantBlobClientProvider(tenantRegistry, tenantContext) {
          @Override
          protected BlobServiceAsyncClient createClient(
              String tenantId,
              String logicalStorageKey,
              TenantDefinition.StorageDefinition storage) {
            return "PNPG".equals(tenantId) ? pnpgClient : createdClient;
          }
        };
    when(tenantRegistry.storage("AR", StorageKeys.CONTRACTS))
        .thenReturn(storage("st-ar", "contracts", "BLOB_AR"));
    when(tenantRegistry.storage("PNPG", StorageKeys.CONTRACTS))
        .thenReturn(storage("st-pnpg", "contracts", "BLOB_PNPG"));

    assertNotSame(
        multiProvider.clientFor("AR", StorageKeys.CONTRACTS).client(),
        multiProvider.clientFor("PNPG", StorageKeys.CONTRACTS).client());
  }

  @Test
  void clientForCurrentTenant_usesTenantContext() {
    when(tenantContext.requiredTenantId()).thenReturn("AR");
    when(tenantRegistry.storage("AR", StorageKeys.CONTRACTS))
        .thenReturn(storage("st-ar", "contracts", "BLOB_AR"));

    assertSame(createdClient, provider.clientForCurrentTenant(StorageKeys.CONTRACTS).client());
  }

  @Test
  void clientFor_unknownStorageFailsClosed() {
    when(tenantRegistry.storage("AR", "unknown"))
        .thenThrow(new UnknownStorageException("AR", "unknown"));

    assertThrows(UnknownStorageException.class, () -> provider.clientFor("AR", "unknown"));
  }

  @Test
  void createClient_connectionString_missingEnvVar_throwsIllegalState() {
    TenantDefinition.StorageDefinition storage =
        new TenantDefinition.StorageDefinition(
            "st-ar",
            "contracts",
            "",
            new TenantDefinition.StorageAuthentication(
                StorageAuthenticationType.CONNECTION_STRING, null, "BLOB_AR_CONNECTION_STRING"));
    when(tenantRegistry.storageConnectionString("AR", StorageKeys.CONTRACTS))
        .thenReturn(Optional.empty());

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                new TenantBlobClientProvider(tenantRegistry, tenantContext)
                    .createClient("AR", StorageKeys.CONTRACTS, storage));

    assertEquals(
        "Missing storage connection string for tenant AR and key contracts",
        exception.getMessage());
  }

  @Test
  void initialize_eagerlyCreatesClientsForEverySupportedTenantAndMandatoryStorageKey()
      throws Exception {
    when(tenantRegistry.supportedTenantIds()).thenReturn(Set.of("AR", "PNPG"));
    when(tenantRegistry.mandatoryStorageKeys()).thenReturn(Set.of(StorageKeys.CONTRACTS));
    when(tenantRegistry.storage("AR", StorageKeys.CONTRACTS))
        .thenReturn(storage("st-ar", "contracts", "BLOB_AR"));
    when(tenantRegistry.storage("PNPG", StorageKeys.CONTRACTS))
        .thenReturn(storage("st-pnpg", "contracts", "BLOB_PNPG"));

    Method initialize = TenantBlobClientProvider.class.getDeclaredMethod("initialize");
    initialize.setAccessible(true);
    initialize.invoke(provider);

    verify(tenantRegistry).storage("AR", StorageKeys.CONTRACTS);
    verify(tenantRegistry).storage("PNPG", StorageKeys.CONTRACTS);
  }

  @Test
  void initialize_doesNothingWhenNoMandatoryStorageKeysConfigured() throws Exception {
    when(tenantRegistry.supportedTenantIds()).thenReturn(Set.of("AR"));
    when(tenantRegistry.mandatoryStorageKeys()).thenReturn(Set.of());

    Method initialize = TenantBlobClientProvider.class.getDeclaredMethod("initialize");
    initialize.setAccessible(true);
    initialize.invoke(provider);

    verify(tenantRegistry, never())
        .storage(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void clientFor_appliesPathPrefix() {
    TenantDefinition.StorageDefinition storage =
        new TenantDefinition.StorageDefinition(
            "st-ar",
            "contracts",
            "ar/contracts",
            new TenantDefinition.StorageAuthentication(
                StorageAuthenticationType.CONNECTION_STRING, null, "BLOB_AR"));
    when(tenantRegistry.storage("AR", StorageKeys.CONTRACTS)).thenReturn(storage);

    assertEquals("ar/contracts", provider.clientFor("AR", StorageKeys.CONTRACTS).pathPrefix());
  }

  @Test
  void clientFor_removesTrailingSlashFromPathPrefix() {
    TenantDefinition.StorageDefinition storage =
        new TenantDefinition.StorageDefinition(
            "st-ar",
            "contracts",
            "ar/contracts/",
            new TenantDefinition.StorageAuthentication(
                StorageAuthenticationType.CONNECTION_STRING, null, "BLOB_AR"));
    when(tenantRegistry.storage("AR", StorageKeys.CONTRACTS)).thenReturn(storage);

    assertEquals("ar/contracts", provider.clientFor("AR", StorageKeys.CONTRACTS).pathPrefix());
  }

  @Test
  void clientFor_rejectsPathTraversalPrefix() {
    TenantDefinition.StorageDefinition storage =
        new TenantDefinition.StorageDefinition(
            "st-ar",
            "contracts",
            "../secret",
            new TenantDefinition.StorageAuthentication(
                StorageAuthenticationType.CONNECTION_STRING, null, "BLOB_AR"));
    when(tenantRegistry.storage("AR", StorageKeys.CONTRACTS)).thenReturn(storage);

    assertThrows(
        IllegalArgumentException.class, () -> provider.clientFor("AR", StorageKeys.CONTRACTS));
  }

  private static TenantDefinition.StorageDefinition storage(
      String account, String container, String envVar) {
    return new TenantDefinition.StorageDefinition(
        account,
        container,
        "",
        new TenantDefinition.StorageAuthentication(
            StorageAuthenticationType.CONNECTION_STRING, null, envVar));
  }
}

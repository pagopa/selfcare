package it.pagopa.selfcare.onboarding.storage;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.UnknownStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantBlobClientProviderTest {

    private TenantRegistry tenantRegistry;
    private TenantContext tenantContext;
    private AzureBlobClient createdClient;
    private TenantBlobClientProvider provider;

    @BeforeEach
    void setUp() {
        tenantRegistry = mock(TenantRegistry.class);
        tenantContext = mock(TenantContext.class);
        createdClient = mock(AzureBlobClient.class);
        provider = new TenantBlobClientProvider(tenantRegistry, tenantContext) {
            @Override
            protected AzureBlobClient createClient(
                    String tenantId, String logicalStorageKey, TenantDefinition.StorageDefinition storage) {
                return createdClient;
            }
        };
    }

    @Test
    void clientFor_reusesCachedClientForSameAccountAndCredentials() {
        TenantDefinition.StorageDefinition products = storage("st-ar", "products", "BLOB_AR");
        TenantDefinition.StorageDefinition contracts = storage("st-ar", "products", "BLOB_AR");
        when(tenantRegistry.storage("AR", "products")).thenReturn(products);
        when(tenantRegistry.storage("AR", "contracts")).thenReturn(contracts);

        AzureBlobClient first = provider.clientFor("AR", "products");
        AzureBlobClient second = provider.clientFor("AR", "contracts");

        assertSame(createdClient, first);
        assertSame(first, second);
    }

    @Test
    void clientFor_doesNotReuseClientForDifferentTenantAccount() {
        AzureBlobClient pnpgClient = mock(AzureBlobClient.class);
        TenantBlobClientProvider multiProvider = new TenantBlobClientProvider(tenantRegistry, tenantContext) {
            @Override
            protected AzureBlobClient createClient(
                    String tenantId, String logicalStorageKey, TenantDefinition.StorageDefinition storage) {
                return "PNPG".equals(tenantId) ? pnpgClient : createdClient;
            }
        };
        when(tenantRegistry.storage("AR", "products")).thenReturn(storage("st-ar", "products", "BLOB_AR"));
        when(tenantRegistry.storage("PNPG", "products")).thenReturn(storage("st-pnpg", "products", "BLOB_PNPG"));

        assertNotSame(multiProvider.clientFor("AR", "products"), multiProvider.clientFor("PNPG", "products"));
    }

    @Test
    void clientForCurrentTenant_usesTenantContext() {
        when(tenantContext.requiredTenantId()).thenReturn("AR");
        when(tenantRegistry.storage("AR", StorageKeys.PRODUCTS)).thenReturn(storage("st-ar", "products", "BLOB_AR"));

        assertSame(createdClient, provider.clientForCurrentTenant(StorageKeys.PRODUCTS));
    }

    @Test
    void clientFor_unknownStorageFailsClosed() {
        when(tenantRegistry.storage("AR", "unknown")).thenThrow(new UnknownStorageException("AR", "unknown"));

        assertThrows(UnknownStorageException.class, () -> provider.clientFor("AR", "unknown"));
    }

    private static TenantDefinition.StorageDefinition storage(String account, String container, String envVar) {
        return new TenantDefinition.StorageDefinition(
                account,
                container,
                "",
                new TenantDefinition.StorageAuthentication(
                        it.pagopa.selfcare.tenant.StorageAuthenticationType.CONNECTION_STRING, null, envVar));
    }
}

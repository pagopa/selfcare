package it.pagopa.selfcare.onboarding.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
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

    @Test
    void createClient_connectionString_missingEnvVar_throwsIllegalState() {
        TenantDefinition.StorageDefinition storage = new TenantDefinition.StorageDefinition(
                "st-ar", "products", "",
                new TenantDefinition.StorageAuthentication(
                        StorageAuthenticationType.CONNECTION_STRING, null, "BLOB_AR_CONNECTION_STRING"));
        when(tenantRegistry.storageConnectionString("AR", "products")).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new TenantBlobClientProvider(tenantRegistry, tenantContext)
                        .createClient("AR", "products", storage));

        assertEquals(
                "Missing storage connection string for tenant AR and key products",
                exception.getMessage());
    }

    @Test
    void createClient_connectionString_present_buildsAzureBlobClientDefault() {
        TenantDefinition.StorageDefinition storage = new TenantDefinition.StorageDefinition(
                "st-ar", "products", "",
                new TenantDefinition.StorageAuthentication(
                        StorageAuthenticationType.CONNECTION_STRING, null, "BLOB_AR_CONNECTION_STRING"));
        when(tenantRegistry.storageConnectionString("AR", "products"))
                .thenReturn(Optional.of("DefaultEndpointsProtocol=https;AccountName=test;AccountKey=dGVzdA==;EndpointSuffix=core.windows.net"));

        AzureBlobClient client = new TenantBlobClientProvider(tenantRegistry, tenantContext)
                .createClient("AR", "products", storage);

        assertInstanceOf(AzureBlobClientDefault.class, client);
    }

    @Test
    void createClient_managedIdentity_withClientId_buildsAzureBlobClientDefault() {
        TenantDefinition.StorageDefinition storage = new TenantDefinition.StorageDefinition(
                "st-ar", "products", "",
                new TenantDefinition.StorageAuthentication(
                        StorageAuthenticationType.MANAGED_IDENTITY, "BLOB_AR_CLIENT_ID", null));
        when(tenantRegistry.storageManagedIdentityClientId("AR", "products"))
                .thenReturn(Optional.of("client-id-123"));

        AzureBlobClient client = new TenantBlobClientProvider(tenantRegistry, tenantContext)
                .createClient("AR", "products", storage);

        assertInstanceOf(AzureBlobClientDefault.class, client);
    }

    @Test
    void createClient_managedIdentity_withoutClientId_buildsAzureBlobClientDefault() {
        TenantDefinition.StorageDefinition storage = new TenantDefinition.StorageDefinition(
                "st-ar", "products", "",
                new TenantDefinition.StorageAuthentication(
                        StorageAuthenticationType.MANAGED_IDENTITY, null, null));
        when(tenantRegistry.storageManagedIdentityClientId("AR", "products")).thenReturn(Optional.empty());

        AzureBlobClient client = new TenantBlobClientProvider(tenantRegistry, tenantContext)
                .createClient("AR", "products", storage);

        assertInstanceOf(AzureBlobClientDefault.class, client);
    }

    @Test
    void initialize_eagerlyCreatesClientsForEverySupportedTenantAndMandatoryStorageKey() throws Exception {
        when(tenantRegistry.supportedTenantIds()).thenReturn(Set.of("AR", "PNPG"));
        when(tenantRegistry.mandatoryStorageKeys()).thenReturn(Set.of("products"));
        when(tenantRegistry.storage("AR", "products")).thenReturn(storage("st-ar", "products", "BLOB_AR"));
        when(tenantRegistry.storage("PNPG", "products")).thenReturn(storage("st-pnpg", "products", "BLOB_PNPG"));

        Method initialize = TenantBlobClientProvider.class.getDeclaredMethod("initialize");
        initialize.setAccessible(true);
        initialize.invoke(provider);

        verify(tenantRegistry).storage("AR", "products");
        verify(tenantRegistry).storage("PNPG", "products");
    }

    @Test
    void initialize_doesNothingWhenNoMandatoryStorageKeysConfigured() throws Exception {
        when(tenantRegistry.supportedTenantIds()).thenReturn(Set.of("AR"));
        when(tenantRegistry.mandatoryStorageKeys()).thenReturn(Set.of());

        Method initialize = TenantBlobClientProvider.class.getDeclaredMethod("initialize");
        initialize.setAccessible(true);
        initialize.invoke(provider);

        verify(tenantRegistry, never()).storage(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void clientFor_wrapsClientWithPathPrefixWhenConfigured() {
        TenantDefinition.StorageDefinition storage = new TenantDefinition.StorageDefinition(
                "st-ar", "products", "ar/products",
                new TenantDefinition.StorageAuthentication(
                        StorageAuthenticationType.CONNECTION_STRING, null, "BLOB_AR"));
        when(tenantRegistry.storage("AR", "products")).thenReturn(storage);

        AzureBlobClient wrapped = provider.clientFor("AR", "products");
        wrapped.getFileAsText("file.json");

        verify(createdClient, times(1)).getFileAsText("ar/products/file.json");
    }

    @Test
    void clientFor_doesNotWrapClientWhenNoPathPrefixConfigured() {
        when(tenantRegistry.storage("AR", "products")).thenReturn(storage("st-ar", "products", "BLOB_AR"));

        AzureBlobClient client = provider.clientFor("AR", "products");
        client.getFileAsText("file.json");

        assertSame(createdClient, client);
        verify(createdClient, times(1)).getFileAsText("file.json");
    }
}

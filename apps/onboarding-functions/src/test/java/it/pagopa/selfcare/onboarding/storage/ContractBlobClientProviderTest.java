package it.pagopa.selfcare.onboarding.storage;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.context.TenantContext;
import it.pagopa.selfcare.tenant.StorageAuthenticationType;
import it.pagopa.selfcare.tenant.TenantDefinition.StorageAuthentication;
import it.pagopa.selfcare.tenant.TenantDefinition.StorageDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.UnknownStorageException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ContractBlobClientProviderTest {

    @Inject ContractBlobClientProvider configuredProvider;

    private static StorageDefinition managedIdentity(String account, String container, String prefix) {
        return new StorageDefinition(
                account,
                container,
                prefix,
                new StorageAuthentication(StorageAuthenticationType.MANAGED_IDENTITY, "MI_CLIENT_ID", null));
    }

    @Test
    void routesEachTenantToItsOwnRegistryStorageAndPrefix() {
        AzureBlobClient arBlob = mock(AzureBlobClient.class);
        AzureBlobClient pnpgBlob = mock(AzureBlobClient.class);
        when(arBlob.getFileAsText("contracts/template/mail/a.json")).thenReturn("ar-template");
        when(pnpgBlob.getFileAsText("pnpg/contracts/template/mail/a.json")).thenReturn("pnpg-template");
        TenantRegistry registry = mock(TenantRegistry.class);
        when(registry.storage("AR", "contracts"))
                .thenReturn(managedIdentity("stselcdocuments", "sc-d-documents-blob", ""));
        when(registry.storage("PNPG", "contracts"))
                .thenReturn(managedIdentity("stpnpgdocuments", "pnpg-container", "pnpg"));
        List<String> created = new ArrayList<>();
        ContractBlobClientProvider provider = new ContractBlobClientProvider(
                registry,
                (tenant, storage) -> {
                    created.add(tenant + ":" + storage.account());
                    return "AR".equals(tenant) ? arBlob : pnpgBlob;
                });

        try (TenantContext.Scope ignored = TenantContext.open("AR")) {
            assertEquals("ar-template", provider.forCurrentTenant().getFileAsText("contracts/template/mail/a.json"));
            provider.forCurrentTenant();
        }
        try (TenantContext.Scope ignored = TenantContext.open("PNPG")) {
            assertEquals("pnpg-template", provider.forCurrentTenant().getFileAsText("contracts/template/mail/a.json"));
        }

        assertEquals(List.of("AR:stselcdocuments", "PNPG:stpnpgdocuments"), created);
    }

    @Test
    void nullPrefixReadsPathsUnchanged() {
        AzureBlobClient blob = mock(AzureBlobClient.class);
        TenantRegistry registry = mock(TenantRegistry.class);
        when(registry.storage("AR", "contracts")).thenReturn(managedIdentity("st", "container", null));
        ContractBlobClientProvider provider = new ContractBlobClientProvider(registry, (tenant, storage) -> blob);

        try (TenantContext.Scope ignored = TenantContext.open("ar")) {
            provider.forCurrentTenant().getFileAsText("resources/logo.png");
        }

        verify(blob).getFileAsText("resources/logo.png");
    }

    @Test
    void failsClosedWithoutTenantContext() {
        ContractBlobClientProvider provider = new ContractBlobClientProvider(
                mock(TenantRegistry.class),
                (tenant, storage) -> {
                    throw new AssertionError("no client must be created");
                });

        assertThrows(IllegalArgumentException.class, provider::forCurrentTenant);
    }

    @Test
    void failsClosedForTenantWithoutContractsStorage() {
        TenantRegistry registry = mock(TenantRegistry.class);
        when(registry.storage("PNPG", "contracts")).thenThrow(new UnknownStorageException("PNPG", "contracts"));
        ContractBlobClientProvider provider = new ContractBlobClientProvider(
                registry,
                (tenant, storage) -> {
                    throw new AssertionError("no client must be created");
                });

        try (TenantContext.Scope ignored = TenantContext.open("PNPG")) {
            IllegalStateException error = assertThrows(IllegalStateException.class, provider::forCurrentTenant);
            assertEquals("No contract storage configured for tenant PNPG", error.getMessage());
        }
    }

    @Test
    void invalidPrefixInRegistryIsRejected() {
        TenantRegistry registry = mock(TenantRegistry.class);
        when(registry.storage("AR", "contracts")).thenReturn(managedIdentity("st", "container", "../other"));
        ContractBlobClientProvider provider =
                new ContractBlobClientProvider(registry, (tenant, storage) -> mock(AzureBlobClient.class));

        try (TenantContext.Scope ignored = TenantContext.open("AR")) {
            assertThrows(IllegalArgumentException.class, provider::forCurrentTenant);
        }
    }

    @Test
    void applicationRegistryBindsEveryConfiguredTenant() {
        AzureBlobClient arClient;
        try (TenantContext.Scope ignored = TenantContext.open("AR")) {
            arClient = configuredProvider.forCurrentTenant();
            assertInstanceOf(PrefixingAzureBlobClient.class, arClient);
            assertSame(arClient, configuredProvider.forCurrentTenant());
        }
        try (TenantContext.Scope ignored = TenantContext.open("PNPG")) {
            assertInstanceOf(PrefixingAzureBlobClient.class, configuredProvider.forCurrentTenant());
        }
    }
}

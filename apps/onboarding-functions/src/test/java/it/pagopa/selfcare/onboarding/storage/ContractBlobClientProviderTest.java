package it.pagopa.selfcare.onboarding.storage;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.config.ContractStorageConfig;
import it.pagopa.selfcare.onboarding.config.ContractStorageConfig.TenantBinding;
import it.pagopa.selfcare.onboarding.context.TenantContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ContractBlobClientProviderTest {

    @Inject ContractBlobClientProvider configuredProvider;

    record Binding(
            Optional<String> accountName,
            Optional<String> container,
            Optional<String> pathPrefix,
            Optional<String> managedIdentityClientId,
            Optional<String> connectionString)
            implements TenantBinding {

        static Binding managedIdentity(String account, String container, String prefix) {
            return new Binding(
                    Optional.of(account),
                    Optional.of(container),
                    Optional.ofNullable(prefix),
                    Optional.of("client-id"),
                    Optional.empty());
        }
    }

    private static ContractStorageConfig config(Map<String, TenantBinding> tenants) {
        return () -> tenants;
    }

    @Test
    void routesEachTenantToItsOwnBindingAndPrefix() {
        AzureBlobClient arBlob = mock(AzureBlobClient.class);
        AzureBlobClient pnpgBlob = mock(AzureBlobClient.class);
        when(arBlob.getFileAsText("contracts/template/mail/a.json")).thenReturn("ar-template");
        when(pnpgBlob.getFileAsText("pnpg/contracts/template/mail/a.json")).thenReturn("pnpg-template");
        List<TenantBinding> created = new ArrayList<>();
        ContractBlobClientProvider provider = new ContractBlobClientProvider(
                config(Map.of(
                        "AR", Binding.managedIdentity("stselcdocuments", "sc-d-documents-blob", ""),
                        "PNPG", Binding.managedIdentity("stpnpgdocuments", "pnpg-container", "pnpg"))),
                binding -> {
                    created.add(binding);
                    return "sc-d-documents-blob".equals(binding.container().orElseThrow()) ? arBlob : pnpgBlob;
                });

        try (TenantContext.Scope ignored = TenantContext.open("AR")) {
            assertEquals("ar-template", provider.forCurrentTenant().getFileAsText("contracts/template/mail/a.json"));
            provider.forCurrentTenant();
        }
        try (TenantContext.Scope ignored = TenantContext.open("PNPG")) {
            assertEquals("pnpg-template", provider.forCurrentTenant().getFileAsText("contracts/template/mail/a.json"));
        }

        assertEquals(2, created.size());
        assertEquals("stselcdocuments", created.get(0).accountName().orElseThrow());
        assertEquals("stpnpgdocuments", created.get(1).accountName().orElseThrow());
    }

    @Test
    void arWithEmptyPrefixReadsPathsUnchanged() {
        AzureBlobClient blob = mock(AzureBlobClient.class);
        ContractBlobClientProvider provider = new ContractBlobClientProvider(
                config(Map.of("AR", Binding.managedIdentity("stselcdocuments", "sc-d-documents-blob", ""))),
                binding -> blob);

        try (TenantContext.Scope ignored = TenantContext.open("ar")) {
            provider.forCurrentTenant().getFileAsText("resources/logo.png");
        }

        verify(blob).getFileAsText("resources/logo.png");
    }

    @Test
    void connectionStringTakesPrecedenceOverManagedIdentity() {
        Binding binding = new Binding(
                Optional.of("devstoreaccount1"),
                Optional.of("contracts-blob"),
                Optional.empty(),
                Optional.of("client-id"),
                Optional.of("UseDevelopmentStorage=true"));
        List<TenantBinding> created = new ArrayList<>();
        ContractBlobClientProvider provider = new ContractBlobClientProvider(
                config(Map.of("AR", binding)),
                resolved -> {
                    created.add(resolved);
                    return mock(AzureBlobClient.class);
                });

        try (TenantContext.Scope ignored = TenantContext.open("AR")) {
            assertNotNull(provider.forCurrentTenant());
        }

        assertSame(binding, created.get(0));
    }

    @Test
    void failsClosedWithoutTenantContext() {
        ContractBlobClientProvider provider = new ContractBlobClientProvider(
                config(Map.of("AR", Binding.managedIdentity("st", "container", ""))),
                binding -> {
                    throw new AssertionError("no client must be created");
                });

        assertThrows(IllegalArgumentException.class, provider::forCurrentTenant);
    }

    @Test
    void failsClosedForTenantWithoutBinding() {
        ContractBlobClientProvider provider = new ContractBlobClientProvider(
                config(Map.of("AR", Binding.managedIdentity("st", "container", ""))),
                binding -> {
                    throw new AssertionError("no client must be created");
                });

        try (TenantContext.Scope ignored = TenantContext.open("PNPG")) {
            IllegalStateException error = assertThrows(IllegalStateException.class, provider::forCurrentTenant);
            assertEquals("No contract storage binding configured for tenant PNPG", error.getMessage());
        }
    }

    @Test
    void failsClosedForIncompleteBinding() {
        Binding withoutContainer = new Binding(
                Optional.of("st"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        Binding withoutAuthTarget = new Binding(
                Optional.empty(), Optional.of("container"), Optional.empty(), Optional.of("client-id"), Optional.of(" "));
        ContractBlobClientProvider provider = new ContractBlobClientProvider(
                config(Map.of("AR", withoutContainer, "PNPG", withoutAuthTarget)),
                binding -> {
                    throw new AssertionError("no client must be created");
                });

        try (TenantContext.Scope ignored = TenantContext.open("AR")) {
            assertThrows(IllegalStateException.class, provider::forCurrentTenant);
        }
        try (TenantContext.Scope ignored = TenantContext.open("PNPG")) {
            assertThrows(IllegalStateException.class, provider::forCurrentTenant);
        }
    }

    @Test
    void invalidPrefixInBindingIsRejected() {
        ContractBlobClientProvider provider = new ContractBlobClientProvider(
                config(Map.of("AR", Binding.managedIdentity("st", "container", "../other"))),
                binding -> mock(AzureBlobClient.class));

        try (TenantContext.Scope ignored = TenantContext.open("AR")) {
            assertThrows(IllegalArgumentException.class, provider::forCurrentTenant);
        }
    }

    @Test
    void applicationConfigurationBindsArAndRejectsUnconfiguredTenants() {
        try (TenantContext.Scope ignored = TenantContext.open("AR")) {
            assertInstanceOf(PrefixingAzureBlobClient.class, configuredProvider.forCurrentTenant());
        }
        try (TenantContext.Scope ignored = TenantContext.open("PNPG")) {
            assertThrows(IllegalStateException.class, configuredProvider::forCurrentTenant);
        }
    }

    @Test
    void emptyConfigurationHasNoBinding() {
        ContractBlobClientProvider provider = new ContractBlobClientProvider(config(Map.of()), binding -> null);

        try (TenantContext.Scope ignored = TenantContext.open("AR")) {
            assertTrue(assertThrows(IllegalStateException.class, provider::forCurrentTenant)
                    .getMessage().contains("AR"));
        }
    }
}

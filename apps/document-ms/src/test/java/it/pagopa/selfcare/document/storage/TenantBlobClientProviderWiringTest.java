package it.pagopa.selfcare.document.storage;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.model.StorageOrigin;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.UnknownStorageException;
import it.pagopa.selfcare.tenant.UnknownTenantException;
import it.pagopa.selfcare.tenant.UnresolvedTenantException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.binding;
import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.connectionString;
import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.managedIdentity;
import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.registry;
import static it.pagopa.selfcare.document.storage.TenantRegistryFixtures.tenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the CDI wiring of {@link TenantBlobClientProvider} and the creation of real Azure clients, which need the
 * Quarkus Vert.x HTTP provider. Building a client never contacts Azure.
 */
@QuarkusTest
class TenantBlobClientProviderWiringTest {

    private static final String MI_ENV = "DMS05_WIRING_MI_CLIENT_ID";
    private static final String CS_ENV = "DMS05_WIRING_CONNECTION_STRING";
    private static final String MI_SECRET = "wiring-managed-identity-secret-value";
    private static final String CS_SECRET_KEY = "d2lyaW5nLXNlY3JldC1rZXk=";
    private static final String VALID_CONNECTION_STRING =
            "DefaultEndpointsProtocol=https;AccountName=stwiring;AccountKey=" + CS_SECRET_KEY + ";EndpointSuffix=core.windows.net";

    @Inject
    TenantBlobClientProvider provider;

    @Inject
    TenantContext tenantContext;

    @BeforeEach
    void defineEnvironment() {
        System.setProperty(MI_ENV, MI_SECRET);
        System.setProperty(CS_ENV, VALID_CONNECTION_STRING);
    }

    @AfterEach
    void clearEnvironment() {
        System.clearProperty(MI_ENV);
        System.clearProperty(CS_ENV);
    }

    @Test
    void clientForCurrentTenant_shouldBuildClientsFromTheConfiguredRegistry() {
        AzureBlobClient system = inRequest("AR", () -> provider.clientForCurrentTenant(StorageOrigin.SYSTEM));
        AzureBlobClient user = inRequest("AR", () -> provider.clientForCurrentTenant(StorageOrigin.USER));
        AzureBlobClient legacy = inRequest("ar", () -> provider.clientForCurrentTenant((StorageOrigin) null));

        assertThat(system).isInstanceOf(PrefixingAzureBlobClient.class);
        assertThat(user).isInstanceOf(PrefixingAzureBlobClient.class);
        assertThat(legacy).isInstanceOf(PrefixingAzureBlobClient.class);
    }

    @Test
    void clientForCurrentTenant_shouldFailClosedWithoutTenantInTheRequest() {
        assertThatThrownBy(() -> inRequest(null, () -> provider.clientForCurrentTenant(StorageKeys.CONTRACTS)))
                .isInstanceOf(UnresolvedTenantException.class);
    }

    @Test
    void clientForCurrentTenant_shouldRejectUnknownLogicalKeyAndUnsupportedTenant() {
        assertThatThrownBy(() -> inRequest("AR", () -> provider.clientForCurrentTenant("unknown")))
                .isInstanceOf(UnknownStorageException.class);
        assertThatThrownBy(() -> inRequest("PNPG", () -> provider.clientForCurrentTenant(StorageKeys.CONTRACTS)))
                .isInstanceOf(UnknownTenantException.class);
    }

    @Test
    void clientFor_shouldBuildRealClientsForBothAuthenticationTypes() throws Exception {
        TenantRegistry registry = registry(
                "{\"AR\":" + tenant(
                        binding("stwiringdocs", "documents", "tenants/ar", managedIdentity(MI_ENV)),
                        binding("stwiringattach", "attachments", "tenants/ar", connectionString(CS_ENV))) + "}",
                "AR",
                "contracts,user-attachments");
        TenantBlobClientProvider realProvider = new TenantBlobClientProvider(registry, tenantContext);

        AzureBlobClient managedIdentityClient = realProvider.clientFor("AR", StorageKeys.CONTRACTS);
        AzureBlobClient connectionStringClient = realProvider.clientFor("AR", StorageKeys.USER_ATTACHMENTS);

        assertThat(managedIdentityClient).isInstanceOf(PrefixingAzureBlobClient.class);
        assertThat(connectionStringClient).isInstanceOf(PrefixingAzureBlobClient.class);
        assertThat(managedIdentityClient.toString()).doesNotContain(MI_SECRET, CS_SECRET_KEY);
        assertThat(connectionStringClient.toString()).doesNotContain(MI_SECRET, CS_SECRET_KEY);
    }

    @Test
    void clientFor_shouldRejectMaliciousPathsBeforeAnyCallToAzure() throws Exception {
        TenantRegistry registry = registry(
                "{\"AR\":" + tenant(
                        binding("stwiringdocs", "documents", "tenants/ar", managedIdentity(MI_ENV)),
                        binding("stwiringattach", "attachments", "", managedIdentity(MI_ENV))) + "}",
                "AR",
                "contracts,user-attachments");
        TenantBlobClientProvider realProvider = new TenantBlobClientProvider(registry, tenantContext);
        AzureBlobClient client = realProvider.clientFor("AR", StorageKeys.CONTRACTS);

        // a real call to Azure would fail with a storage/credential error, not with a path validation error
        assertThatThrownBy(() -> client.getFile("../other-tenant/a.pdf"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> client.getFiles("/absolute"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void clientFor_shouldNotLeakAMalformedConnectionStringInTheFailure() throws Exception {
        String malformed = "not-a-connection-string-malformed-secret-value";
        System.setProperty(CS_ENV, malformed);
        TenantRegistry registry = registry(
                "{\"AR\":" + tenant(
                        binding("stwiringdocs", "documents", "", connectionString(CS_ENV)),
                        binding("stwiringattach", "attachments", "", connectionString(CS_ENV))) + "}",
                "AR",
                "contracts,user-attachments");
        TenantBlobClientProvider realProvider = new TenantBlobClientProvider(registry, tenantContext);

        Throwable failure = null;
        try {
            realProvider.clientFor("AR", StorageKeys.CONTRACTS);
        } catch (RuntimeException e) {
            failure = e;
        }

        assertThat(failure).as("a malformed connection string must not produce a client").isNotNull();
        for (Throwable t = failure; t != null; t = t.getCause()) {
            assertThat(String.valueOf(t.getMessage())).doesNotContain("malformed-secret-value");
        }
    }

    private <T> T inRequest(String tenantId, Supplier<T> action) {
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            if (tenantId != null) {
                tenantContext.setTenantId(tenantId);
            }
            return action.get();
        } finally {
            requestContext.terminate();
        }
    }
}

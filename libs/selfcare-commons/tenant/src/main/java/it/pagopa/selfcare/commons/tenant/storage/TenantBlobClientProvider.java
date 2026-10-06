package it.pagopa.selfcare.commons.tenant.storage;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import it.pagopa.selfcare.commons.tenant.StorageAuthenticationType;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantDefinition;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class TenantBlobClientProvider {

    private final TenantRegistry tenantRegistry;
    private final TenantContext tenantContext;
    private final ConcurrentHashMap<ClientKey, BlobContainerClient> clients =
            new ConcurrentHashMap<>();

    public TenantBlobClientProvider(TenantRegistry tenantRegistry, TenantContext tenantContext) {
        this.tenantRegistry = tenantRegistry;
        this.tenantContext = tenantContext;
    }

    @PostConstruct
    void initialize() {
        tenantRegistry.supportedTenantIds().forEach(tenantId ->
                tenantRegistry.mandatoryStorageKeys().forEach(
                        logicalKey -> clientFor(tenantId, logicalKey)));
    }

    public TenantBlobClient clientForCurrentTenant(String logicalKey) {
        return clientFor(tenantContext.requiredTenantId(), logicalKey);
    }

    public TenantBlobClient clientFor(String tenantId, String logicalKey) {
        TenantDefinition.StorageDefinition storage =
                tenantRegistry.storage(tenantId, logicalKey);
        BlobContainerClient client = clients.computeIfAbsent(
                clientKey(storage),
                ignored -> createContainerClient(tenantId, logicalKey, storage));
        return new TenantBlobClient(client, storage.pathPrefix());
    }

    protected BlobContainerClient createContainerClient(
            String tenantId,
            String logicalKey,
            TenantDefinition.StorageDefinition storage) {
        TenantDefinition.StorageAuthentication authentication = storage.authentication();
        BlobServiceClientBuilder builder = new BlobServiceClientBuilder();
        if (authentication.type() == StorageAuthenticationType.CONNECTION_STRING) {
            String connectionString = tenantRegistry
                    .storageConnectionString(tenantId, logicalKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing storage connection string for tenant "
                                    + tenantId + " and key " + logicalKey));
            builder.connectionString(connectionString);
        } else {
            String clientId = tenantRegistry
                    .storageManagedIdentityClientId(tenantId, logicalKey)
                    .orElse("");
            DefaultAzureCredentialBuilder credentialBuilder =
                    new DefaultAzureCredentialBuilder();
            if (!clientId.isBlank()) {
                credentialBuilder.managedIdentityClientId(clientId);
            }
            builder.endpoint("https://" + storage.account() + ".blob.core.windows.net")
                    .credential(credentialBuilder.build());
        }
        return builder.buildClient().getBlobContainerClient(storage.container());
    }

    private static ClientKey clientKey(TenantDefinition.StorageDefinition storage) {
        TenantDefinition.StorageAuthentication authentication = storage.authentication();
        String credentialReference =
                authentication.type() == StorageAuthenticationType.CONNECTION_STRING
                        ? authentication.connectionStringEnvVar()
                        : Objects.toString(authentication.managedIdentityClientIdEnvVar(), "");
        return new ClientKey(
                storage.account(),
                storage.container(),
                authentication.type(),
                credentialReference);
    }

    private record ClientKey(
            String account,
            String container,
            StorageAuthenticationType authenticationType,
            String credentialReference) {
    }
}

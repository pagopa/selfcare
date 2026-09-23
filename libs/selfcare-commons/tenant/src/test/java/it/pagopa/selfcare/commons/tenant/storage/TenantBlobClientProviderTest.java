package it.pagopa.selfcare.commons.tenant.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.azure.storage.blob.BlobContainerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantDefinition;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class TenantBlobClientProviderTest {

    @Test
    void selectsStorageAndAppliesTrustedPrefix() {
        TenantRegistry registry = registry();
        TenantContext context = new TenantContext();
        TenantBlobClientProvider provider = new TenantBlobClientProvider(registry, context);
        context.setTenantId("AR");

        TenantBlobClient client = provider.clientForCurrentTenant("documents");

        assertEquals("groups/contracts/test.pdf", client.blob("contracts/test.pdf").getBlobName());
        assertEquals("groups/contracts/test.pdf",
                client.blob("groups/contracts/test.pdf").getBlobName());
    }

    @Test
    void cachesContainerClientForEquivalentBindings() {
        TenantRegistry registry = registry();
        TenantContext context = new TenantContext();
        class CountingProvider extends TenantBlobClientProvider {
            private int creations;

            CountingProvider() {
                super(registry, context);
            }

            @Override
            protected BlobContainerClient createContainerClient(
                    String tenantId,
                    String logicalKey,
                    TenantDefinition.StorageDefinition storage) {
                creations++;
                return super.createContainerClient(tenantId, logicalKey, storage);
            }
        }
        CountingProvider provider = new CountingProvider();

        provider.clientFor("AR", "documents");
        provider.clientFor("AR", "archive");

        assertEquals(1, provider.creations);
    }

    private static TenantRegistry registry() {
        String registryJson = """
                {
                  "AR": {
                    "mongo": {
                      "account": "local",
                      "database": "groups",
                      "connectionStringEnvVar": "MONGO_AR"
                    },
                    "storages": {
                      "documents": {
                        "account": "storage-ar",
                        "container": "documents",
                        "pathPrefix": "groups",
                        "authentication": {
                          "type": "CONNECTION_STRING",
                          "connectionStringEnvVar": "BLOB_AR"
                        }
                      },
                      "archive": {
                        "account": "storage-ar",
                        "container": "documents",
                        "pathPrefix": "archive",
                        "authentication": {
                          "type": "CONNECTION_STRING",
                          "connectionStringEnvVar": "BLOB_AR"
                        }
                      }
                    }
                  }
                }
                """;
        MockEnvironment environment = new MockEnvironment()
                .withProperty("MONGO_AR", "mongodb://localhost:27017")
                .withProperty(
                        "BLOB_AR",
                        "DefaultEndpointsProtocol=https;AccountName=test;"
                                + "AccountKey=dGVzdA==;EndpointSuffix=core.windows.net");
        TenantRegistry registry =
                new TenantRegistry(new ObjectMapper(), environment, registryJson, "AR", "");
        registry.initialize();
        return registry;
    }
}

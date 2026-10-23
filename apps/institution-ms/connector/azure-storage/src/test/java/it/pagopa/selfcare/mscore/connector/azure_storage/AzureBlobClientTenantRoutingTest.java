package it.pagopa.selfcare.mscore.connector.azure_storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantDefinition;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClient;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClientProvider;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AzureBlobClientTenantRoutingTest {

    @Test
    void probesCurrentAndExplicitTenantContainers() {
        TenantBlobClientProvider provider = mock(TenantBlobClientProvider.class);
        TenantBlobClient arStorage = mock(TenantBlobClient.class);
        TenantBlobClient pnpgStorage = mock(TenantBlobClient.class);
        BlobClient arRoot = mock(BlobClient.class);
        BlobClient pnpgRoot = mock(BlobClient.class);
        BlobContainerClient arContainer = mock(BlobContainerClient.class);
        BlobContainerClient pnpgContainer = mock(BlobContainerClient.class);
        when(provider.clientForCurrentTenant("mail-templates")).thenReturn(arStorage);
        when(provider.clientFor("PNPG", "mail-templates")).thenReturn(pnpgStorage);
        when(arStorage.blob("")).thenReturn(arRoot);
        when(pnpgStorage.blob("")).thenReturn(pnpgRoot);
        when(arRoot.getContainerClient()).thenReturn(arContainer);
        when(pnpgRoot.getContainerClient()).thenReturn(pnpgContainer);
        AzureBlobClient client = new AzureBlobClient(provider);

        client.probeContainer();
        client.probeContainer("PNPG");

        verify(arContainer).getProperties();
        verify(pnpgContainer).getProperties();

        RuntimeException failure = new IllegalStateException("Container unavailable");
        when(pnpgContainer.getProperties()).thenThrow(failure);
        assertSame(failure, assertThrows(RuntimeException.class, () -> client.probeContainer("PNPG")));
    }

    @Test
    void resolvesContainerFromEmptyOrPrefixedBlobReferenceWithoutNetworkAccess() {
        BlobContainerClient container = new BlobContainerClientBuilder()
                .endpoint("https://storage.blob.core.windows.net")
                .containerName("templates")
                .buildClient();

        assertEquals(container.getBlobContainerUrl(),
                container.getBlobClient("").getContainerClient().getBlobContainerUrl());
        assertEquals(container.getBlobContainerUrl(),
                container.getBlobClient("ar").getContainerClient().getBlobContainerUrl());
    }

    @Test
    void resolvesTemplateStorageForEveryTenantCallAndRejectsUnsafePaths() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("MONGO_AR", "mongodb://localhost/ar")
                .withProperty("MONGO_PNPG", "mongodb://localhost/pnpg")
                .withProperty("BLOB_AR", "connection-ar")
                .withProperty("BLOB_PNPG", "connection-pnpg");
        String registryJson = """
                {
                  "AR": {
                    "mongo": {
                      "account": "ar",
                      "database": "ar",
                      "connectionStringEnvVar": "MONGO_AR"
                    },
                    "storages": {
                      "mail-templates": {
                        "account": "storage-ar",
                        "container": "templates",
                        "pathPrefix": "ar",
                        "authentication": {
                          "type": "CONNECTION_STRING",
                          "connectionStringEnvVar": "BLOB_AR"
                        }
                      }
                    }
                  },
                  "PNPG": {
                    "mongo": {
                      "account": "pnpg",
                      "database": "pnpg",
                      "connectionStringEnvVar": "MONGO_PNPG"
                    },
                    "storages": {
                      "mail-templates": {
                        "account": "storage-pnpg",
                        "container": "templates",
                        "pathPrefix": "pnpg",
                        "authentication": {
                          "type": "CONNECTION_STRING",
                          "connectionStringEnvVar": "BLOB_PNPG"
                        }
                      }
                    }
                  }
                }
                """;
        TenantRegistry tenantRegistry =
                new TenantRegistry(new ObjectMapper(), environment, registryJson, "AR,PNPG", "mail-templates");
        tenantRegistry.initialize();

        BlobContainerClient arContainer = mock(BlobContainerClient.class);
        BlobContainerClient pnpgContainer = mock(BlobContainerClient.class);
        BlobClient arBlob = mock(BlobClient.class);
        BlobClient pnpgBlob = mock(BlobClient.class);
        when(arContainer.getBlobClient("ar/contracts/template.json")).thenReturn(arBlob);
        when(pnpgContainer.getBlobClient("pnpg/contracts/template.json")).thenReturn(pnpgBlob);
        when(arBlob.downloadContent()).thenReturn(BinaryData.fromString("AR template"));
        when(pnpgBlob.downloadContent()).thenReturn(BinaryData.fromString("PNPG template"));

        TenantContext tenantContext = new TenantContext();
        Map<String, BlobContainerClient> containers =
                Map.of("AR", arContainer, "PNPG", pnpgContainer);
        TenantBlobClientProvider provider = new TenantBlobClientProvider(
                tenantRegistry, tenantContext) {
            @Override
            protected BlobContainerClient createContainerClient(
                    String tenantId,
                    String logicalKey,
                    TenantDefinition.StorageDefinition storage) {
                return containers.get(tenantId);
            }
        };
        AzureBlobClient client = new AzureBlobClient(provider);

        tenantContext.setTenantId("AR");
        assertEquals("AR template", client.getTemplateFile("contracts/template.json"));
        tenantContext.setTenantId("PNPG");
        assertEquals("PNPG template", client.getTemplateFile("contracts/template.json"));
        verify(arContainer).getBlobClient("ar/contracts/template.json");
        verify(pnpgContainer).getBlobClient("pnpg/contracts/template.json");
        assertThrows(MsCoreException.class, () -> client.getTemplateFile("../secret.json"));
        tenantContext.clear();
        assertThrows(MsCoreException.class, () -> client.getTemplateFile("contracts/template.json"));
    }
}

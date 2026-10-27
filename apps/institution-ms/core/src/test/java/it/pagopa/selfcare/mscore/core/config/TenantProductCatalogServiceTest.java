package it.pagopa.selfcare.mscore.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.UnresolvedTenantException;
import it.pagopa.selfcare.commons.tenant.UnknownTenantException;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClientProvider;
import it.pagopa.selfcare.product.service.ProductService;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class TenantProductCatalogServiceTest {

    @Test
    void isolatesAndRefreshesProductCatalogsByTenant() throws Exception {
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
                      "products": {
                        "account": "catalog",
                        "container": "products",
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
                      "products": {
                        "account": "catalog",
                        "container": "products",
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
        TenantRegistry registry =
                new TenantRegistry(new ObjectMapper(), environment, registryJson, "AR,PNPG", "products");
        registry.initialize();

        BlobContainerClient container = mock(BlobContainerClient.class);
        BlobClient arBlob = mock(BlobClient.class);
        BlobClient pnpgBlob = mock(BlobClient.class);
        BlobProperties initialProperties = properties("2026-01-01T00:00:00Z");
        BlobProperties updatedProperties = properties("2026-01-02T00:00:00Z");
        when(container.getBlobClient("ar/products.json")).thenReturn(arBlob);
        when(container.getBlobClient("pnpg/products.json")).thenReturn(pnpgBlob);
        when(arBlob.getProperties()).thenReturn(
                initialProperties, initialProperties, updatedProperties, updatedProperties);
        when(pnpgBlob.getProperties()).thenReturn(initialProperties);
        when(arBlob.downloadContent()).thenReturn(
                catalog("AR initial"),
                catalog("AR updated"));
        when(pnpgBlob.downloadContent()).thenReturn(catalog("PNPG catalog"));

        TenantContext context = new TenantContext();
        TenantBlobClientProvider provider = new TenantBlobClientProvider(registry, context) {
            @Override
            protected BlobContainerClient createContainerClient(
                    String tenantId,
                    String logicalKey,
                    it.pagopa.selfcare.commons.tenant.TenantDefinition.StorageDefinition storage) {
                return container;
            }
        };
        ProductService service =
                new TenantProductCatalogService(provider, registry, context, "products.json");

        context.setTenantId("AR");
        assertEquals("AR initial", service.getProduct("shared-product").getTitle());
        context.setTenantId("PNPG");
        assertEquals("PNPG catalog", service.getProduct("shared-product").getTitle());
        context.setTenantId("AR");
        assertEquals("AR updated", service.getProduct("shared-product").getTitle());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> arResult = executor.submit(
                    () -> context.withTenant(
                            "AR",
                            () -> service.getProduct("shared-product").getTitle()));
            Future<String> pnpgResult = executor.submit(
                    () -> context.withTenant(
                            "PNPG",
                            () -> service.getProduct("shared-product").getTitle()));
            assertEquals("AR updated", arResult.get());
            assertEquals("PNPG catalog", pnpgResult.get());
        } finally {
            executor.shutdownNow();
        }

        verify(arBlob, times(2)).downloadContent();
        verify(pnpgBlob, times(1)).downloadContent();
        verify(container, atLeastOnce()).getBlobClient("ar/products.json");
        verify(container, atLeastOnce()).getBlobClient("pnpg/products.json");

        context.clear();
        assertThrows(UnresolvedTenantException.class, () -> service.getProduct("shared-product"));
        context.setTenantId("UNSUPPORTED");
        assertThrows(UnknownTenantException.class, () -> service.getProduct("shared-product"));
    }

    private static BlobProperties properties(String lastModified) {
        BlobProperties properties = mock(BlobProperties.class);
        when(properties.getLastModified()).thenReturn(OffsetDateTime.parse(lastModified));
        return properties;
    }

    private static BinaryData catalog(String title) {
        return BinaryData.fromString(
                """
                [{"id":"shared-product","title":"%s","status":"ACTIVE"}]
                """.formatted(title));
    }
}

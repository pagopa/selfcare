package it.pagopa.selfcare.external_api.client;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantDefinition;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.UnknownStorageException;
import it.pagopa.selfcare.commons.tenant.UnknownTenantException;
import it.pagopa.selfcare.commons.tenant.storage.TenantBlobClientProvider;
import it.pagopa.selfcare.external_api.exception.AzureRestClientException;
import it.pagopa.selfcare.external_api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static it.pagopa.selfcare.external_api.model.constant.GenericError.ERROR_DURING_DOWNLOAD_FILE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AzureBlobClientTest {

    private final TenantContext context = new TenantContext();
    private final BlobContainerClient arContainer = mock(BlobContainerClient.class);
    private final BlobContainerClient pnpgContainer = mock(BlobContainerClient.class);
    private final BlobClient arBlob = mock(BlobClient.class);
    private final BlobClient pnpgBlob = mock(BlobClient.class);
    private AzureBlobClient client;

    @BeforeEach
    void setUp() throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("tenant.mongodb.required", "false")
                .withProperty("BLOB_AR", "UseDevelopmentStorage=true;")
                .withProperty("BLOB_PNPG", "UseDevelopmentStorage=true;");
        TenantRegistry registry = new TenantRegistry(new ObjectMapper(), environment, """
                {"AR":{"storages":{"contracts":{"account":"arstorage","container":"ar-contracts","pathPrefix":"ar",
                  "authentication":{"type":"CONNECTION_STRING","connectionStringEnvVar":"BLOB_AR"}}}},
                 "PNPG":{"storages":{"contracts":{"account":"pnpgstorage","container":"pnpg-contracts","pathPrefix":"pnpg",
                  "authentication":{"type":"CONNECTION_STRING","connectionStringEnvVar":"BLOB_PNPG"}}}}}
                """, "AR,PNPG", "");
        registry.initialize();
        Map<String, BlobContainerClient> containers = Map.of("AR", arContainer, "PNPG", pnpgContainer);
        client = new AzureBlobClient(new TenantBlobClientProvider(registry, context) {
            @Override
            protected BlobContainerClient createContainerClient(
                    String tenant, String logicalKey, TenantDefinition.StorageDefinition storage) {
                assertEquals("contracts", logicalKey);
                assertEquals(tenant.equals("AR") ? "ar-contracts" : "pnpg-contracts", storage.container());
                return containers.get(tenant);
            }
        });
        when(arContainer.getBlobClient("ar/contract.pdf")).thenReturn(arBlob);
        when(pnpgContainer.getBlobClient("pnpg/contract.pdf")).thenReturn(pnpgBlob);
        for (BlobClient blob : List.of(arBlob, pnpgBlob)) {
            BlobProperties properties = mock(BlobProperties.class);
            when(properties.getContentType()).thenReturn("application/pdf");
            when(blob.getProperties()).thenReturn(properties);
            when(blob.getBlobName()).thenReturn("contract.pdf");
        }
    }

    @AfterEach
    void clearTenant() {
        context.clear();
    }

    @Test
    void sameFilenameIsResolvedAgainstEachTenantsBindingAndPrefix() {
        doAnswer(invocation -> {
            invocation.getArgument(0, OutputStream.class).write("AR".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(arBlob).downloadStream(any());
        doAnswer(invocation -> {
            invocation.getArgument(0, OutputStream.class).write("PNPG".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(pnpgBlob).downloadStream(any());
        for (String tenant : List.of("AR", "PNPG", "AR")) {
            context.setTenantId(tenant);
            var response = client.getFile("contract.pdf");
            assertArrayEquals(tenant.getBytes(StandardCharsets.UTF_8), response.getData());
            assertEquals("contract.pdf", response.getFileName());
            assertEquals("application/pdf", response.getMimetype());
        }
        verify(arContainer, times(2)).getBlobClient("ar/contract.pdf");
        verify(pnpgContainer).getBlobClient("pnpg/contract.pdf");
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 500})
    void preservesStorageErrorMapping(int status) {
        context.setTenantId("AR");
        BlobStorageException failure = mock(BlobStorageException.class);
        when(failure.getStatusCode()).thenReturn(status);
        doThrow(failure).when(arBlob).downloadStream(any());

        Class<? extends RuntimeException> expected =
                status == 404 ? ResourceNotFoundException.class : AzureRestClientException.class;
        var exception = assertThrows(expected, () -> client.getFile("contract.pdf"));
        assertEquals(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), "contract.pdf"),
                exception.getMessage());
        verifyNoInteractions(pnpgContainer);
    }

    @Test
    void missingOrUnknownTenantCannotFallBackToAnotherStorage() {
        assertThrows(IllegalStateException.class, () -> client.getFile("contract.pdf"));
        context.setTenantId("UNKNOWN");
        assertThrows(UnknownTenantException.class, () -> client.getFile("contract.pdf"));
        verifyNoInteractions(arContainer, pnpgContainer);
    }

    @Test
    void missingBindingCannotFallBackToLegacyStorage() {
        TenantRegistry registry = new TenantRegistry(new ObjectMapper(),
                new MockEnvironment().withProperty("tenant.mongodb.required", "false")
                        .withProperty("blobStorage.connectionString", "UseDevelopmentStorage=true;"),
                "{\"PNPG\":{}}", "PNPG", "");
        registry.initialize();
        AzureBlobClient unconfigured = new AzureBlobClient(new TenantBlobClientProvider(registry, context));
        context.setTenantId("PNPG");
        assertThrows(UnknownStorageException.class, () -> unconfigured.getFile("contract.pdf"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../contract.pdf", "/contract.pdf", "folder/../../contract.pdf"})
    void rejectsPathsOutsideTheTenantBinding(String path) {
        context.setTenantId("AR");
        assertThrows(IllegalArgumentException.class, () -> client.getFile(path));
        verifyNoInteractions(arContainer, pnpgContainer);
    }
}

package it.pagopa.selfcare.user.health;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class TemplatesBlobStorageReadinessCheckTest {

    private static final String ACCOUNT = "templates-account";
    private static final String CONTAINER = "$web";
    private static final String TEMPLATES_PATH = "resources/templates/email/";
    private static final String PROBE_TARGET = TEMPLATES_PATH + "user_activated.ftlh";

    private AzureBlobClient blobClient;
    private TemplatesBlobStorageReadinessCheck check;

    @BeforeEach
    void setUp() {
        blobClient = mock(AzureBlobClient.class);
        check = new TemplatesBlobStorageReadinessCheck(
                blobClient, CONTAINER, Optional.of(ACCOUNT), TEMPLATES_PATH);
    }

    private HealthCheckResponse await() {
        return check.call().await().atMost(Duration.ofSeconds(5));
    }

    @Test
    void up_whenProbeTemplateIsReadable() {
        when(blobClient.getProperties(PROBE_TARGET)).thenReturn(null);

        HealthCheckResponse response = await();

        assertEquals("blob-storage-templates", response.getName());
        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        Map<String, Object> data = response.getData().orElseThrow();
        assertEquals("blob-storage", data.get("component"));
        assertEquals(ACCOUNT, data.get("account"));
        assertEquals(CONTAINER, data.get("container"));
        assertEquals(PROBE_TARGET, data.get("probeTarget"));
        assertTrue(data.containsKey("latencyMs"));
        assertFalse(data.containsKey("error"));
    }

    @Test
    void down_whenBlobClientFailsWithAuthorizationError() {
        when(blobClient.getProperties(PROBE_TARGET))
                .thenThrow(new RuntimeException("Status code 403, AuthorizationPermissionMismatch"));

        HealthCheckResponse response = await();

        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        Map<String, Object> data = response.getData().orElseThrow();
        assertEquals(ACCOUNT, data.get("account"));
        assertEquals(CONTAINER, data.get("container"));
        assertEquals(PROBE_TARGET, data.get("probeTarget"));
        assertTrue(data.get("error").toString().contains("403"));
        assertTrue(data.get("error").toString().contains("AuthorizationPermissionMismatch"));
    }

    @Test
    void down_whenClientThrowsRuntimeException() {
        when(blobClient.getProperties(PROBE_TARGET))
                .thenThrow(new RuntimeException("connection refused"));

        HealthCheckResponse response = await();

        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        assertEquals(
                "RuntimeException: connection refused",
                response.getData().orElseThrow().get("error"));
    }

    @Test
    void up_whenAccountNameIsNotConfigured() {
        TemplatesBlobStorageReadinessCheck localCheck = new TemplatesBlobStorageReadinessCheck(
                blobClient, CONTAINER, Optional.empty(), TEMPLATES_PATH);
        when(blobClient.getProperties(PROBE_TARGET)).thenReturn(null);

        HealthCheckResponse response =
                localCheck.call().await().atMost(Duration.ofSeconds(5));

        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        Map<String, Object> data = response.getData().orElseThrow();
        assertEquals("n/a", data.get("account"));
        assertEquals(CONTAINER, data.get("container"));
        assertEquals(PROBE_TARGET, data.get("probeTarget"));
    }
}

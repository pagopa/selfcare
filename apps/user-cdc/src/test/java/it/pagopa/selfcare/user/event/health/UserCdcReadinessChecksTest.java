package it.pagopa.selfcare.user.event.health;

import com.azure.core.http.rest.PagedIterable;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import org.bson.Document;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserCdcReadinessChecksTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    void mongoIsUpWhenPingSucceeds() {
        ReactiveMongoClient mongoClient = mock(ReactiveMongoClient.class);
        ReactiveMongoDatabase database = mock(ReactiveMongoDatabase.class);
        when(mongoClient.getDatabase("selcUser")).thenReturn(database);
        when(database.runCommand(any(Document.class)))
                .thenReturn(Uni.createFrom().item(new Document("ok", 1)));
        UserCdcMongoReadinessCheck check = new UserCdcMongoReadinessCheck(
                mongoClient, "selcUser", "mongodb://mongo:27017/selcUser");

        HealthCheckResponse response = check.call().await().atMost(TIMEOUT);

        assertEquals("mongodb-user-cdc", response.getName());
        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        Map<String, Object> data = response.getData().orElseThrow();
        assertEquals("selcUser", data.get("database"));
        assertEquals("mongo:27017", data.get("host"));
    }

    @Test
    void mongoIsDownWhenPingFails() {
        ReactiveMongoClient mongoClient = mock(ReactiveMongoClient.class);
        ReactiveMongoDatabase database = mock(ReactiveMongoDatabase.class);
        when(mongoClient.getDatabase("selcUser")).thenReturn(database);
        when(database.runCommand(any(Document.class)))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("no primary")));
        UserCdcMongoReadinessCheck check = new UserCdcMongoReadinessCheck(
                mongoClient, "selcUser", "mongodb://mongo:27017/selcUser");

        HealthCheckResponse response = check.call().await().atMost(TIMEOUT);

        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        assertEquals("IllegalStateException: no primary", response.getData().orElseThrow().get("error"));
    }

    @Test
    void tableStorageIsUpWhenListingSucceeds() {
        TableClient tableClient = tableClientReturningEmptyPage();
        UserCdcTableStorageReadinessCheck check =
                new UserCdcTableStorageReadinessCheck(
                        tableClient, "CdCStartAt", Optional.of("table-account"));

        HealthCheckResponse response = check.call().await().atMost(TIMEOUT);

        assertEquals("table-storage-user-cdc", response.getName());
        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        Map<String, Object> data = response.getData().orElseThrow();
        assertEquals("table-storage", data.get("component"));
        assertEquals("table-account", data.get("account"));
        assertEquals("CdCStartAt", data.get("table"));
        assertTrue(data.containsKey("latencyMs"));
        assertFalse(data.containsKey("error"));
    }

    @Test
    void tableStorageIsDownWhenListingFails() {
        TableClient tableClient = mock(TableClient.class);
        when(tableClient.listEntities()).thenThrow(new RuntimeException("Status code 403"));
        UserCdcTableStorageReadinessCheck check =
                new UserCdcTableStorageReadinessCheck(
                        tableClient, "CdCStartAt", Optional.empty());

        HealthCheckResponse response = check.call().await().atMost(TIMEOUT);

        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        assertEquals("n/a", response.getData().orElseThrow().get("account"));
        assertEquals("RuntimeException: Status code 403", response.getData().orElseThrow().get("error"));
    }

    @Test
    void productBlobStorageReportsProbeOutcome() {
        AzureBlobClientDefault blobClient = mock(AzureBlobClientDefault.class);
        when(blobClient.getProperties("products.json")).thenReturn(null);
        ProductBlobStorageReadinessCheck check = new ProductBlobStorageReadinessCheck(
                blobClient, "products", Optional.of("blob-account"), "products.json");

        HealthCheckResponse response = check.call().await().atMost(TIMEOUT);

        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        Map<String, Object> data = response.getData().orElseThrow();
        assertEquals("blob-account", data.get("account"));
        assertEquals("products", data.get("container"));
        assertEquals("products.json", data.get("probeTarget"));
    }

    @Test
    void productBlobStorageIsDownWhenProbeFails() {
        AzureBlobClientDefault blobClient = mock(AzureBlobClientDefault.class);
        when(blobClient.getProperties("products.json"))
                .thenThrow(new RuntimeException("Status code 403"));
        ProductBlobStorageReadinessCheck check = new ProductBlobStorageReadinessCheck(
                blobClient, "products", Optional.empty(), "products.json");

        HealthCheckResponse response = check.call().await().atMost(TIMEOUT);

        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        assertEquals("n/a", response.getData().orElseThrow().get("account"));
        assertEquals("RuntimeException: Status code 403", response.getData().orElseThrow().get("error"));
    }

    @SuppressWarnings("unchecked")
    private static TableClient tableClientReturningEmptyPage() {
        TableClient tableClient = mock(TableClient.class);
        PagedIterable<TableEntity> entities = mock(PagedIterable.class);
        when(tableClient.listEntities()).thenReturn(entities);
        when(entities.iterator()).thenReturn(Collections.emptyIterator());
        return tableClient;
    }
}

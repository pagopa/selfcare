package it.pagopa.selfcare.delegation.event.health;

import com.azure.core.http.rest.PagedIterable;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Uni;
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

class DelegationCdcReadinessChecksTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    void mongoProbeReportsUpAndMetadata() {
        ReactiveMongoClient mongoClient = mock(ReactiveMongoClient.class);
        ReactiveMongoDatabase database = mock(ReactiveMongoDatabase.class);
        when(mongoClient.getDatabase("selcMsCore")).thenReturn(database);
        when(database.runCommand(any(Document.class)))
                .thenReturn(Uni.createFrom().item(new Document("ok", 1)));
        DelegationCdcMongoReadinessCheck check = new DelegationCdcMongoReadinessCheck(
                mongoClient, "selcMsCore", "mongodb://mongo:27017/selcMsCore");

        HealthCheckResponse response = check.call().await().atMost(TIMEOUT);

        assertEquals("mongodb-delegation-cdc", response.getName());
        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        assertEquals("selcMsCore", response.getData().orElseThrow().get("database"));
        assertEquals("mongo:27017", response.getData().orElseThrow().get("host"));
    }

    @Test
    void mongoProbeReportsDownOnFailure() {
        ReactiveMongoClient mongoClient = mock(ReactiveMongoClient.class);
        ReactiveMongoDatabase database = mock(ReactiveMongoDatabase.class);
        when(mongoClient.getDatabase("selcMsCore")).thenReturn(database);
        when(database.runCommand(any(Document.class)))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("no primary")));
        DelegationCdcMongoReadinessCheck check = new DelegationCdcMongoReadinessCheck(
                mongoClient, "selcMsCore", "mongodb://mongo:27017/selcMsCore");

        HealthCheckResponse response = check.call().await().atMost(TIMEOUT);

        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        assertEquals("IllegalStateException: no primary", response.getData().orElseThrow().get("error"));
    }

    @Test
    void tableStorageProbeReportsUpAndMetadata() {
        TableClient tableClient = tableClientReturningEmptyPage();
        DelegationCdcTableStorageReadinessCheck check =
                new DelegationCdcTableStorageReadinessCheck(
                        tableClient, "CdCStartAt", Optional.of("table-account"));

        HealthCheckResponse response = check.call().await().atMost(TIMEOUT);

        assertEquals("table-storage-delegation-cdc", response.getName());
        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        Map<String, Object> data = response.getData().orElseThrow();
        assertEquals("table-storage", data.get("component"));
        assertEquals("table-account", data.get("account"));
        assertEquals("CdCStartAt", data.get("table"));
        assertTrue(data.containsKey("latencyMs"));
        assertFalse(data.containsKey("error"));
    }

    @Test
    void tableStorageProbeReportsDownOnFailure() {
        TableClient tableClient = mock(TableClient.class);
        when(tableClient.listEntities()).thenThrow(new RuntimeException("Status code 403"));
        DelegationCdcTableStorageReadinessCheck check =
                new DelegationCdcTableStorageReadinessCheck(
                        tableClient, "CdCStartAt", Optional.empty());

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

package it.pagopa.selfcare.document.health;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class DocumentMongoReadinessCheckTest {

    private static final String DATABASE = "selcDocument";
    private static final String HOST = "localhost:27017";

    @Inject TenantRegistry tenantRegistry;

    private ReactiveMongoDatabase database;
    private DocumentMongoReadinessCheck check;

    @BeforeEach
    void setUp() {
        TenantMongoClientProducer producer = mock(TenantMongoClientProducer.class);
        ReactiveMongoClient mongoClient = mock(ReactiveMongoClient.class);
        database = mock(ReactiveMongoDatabase.class);
        when(producer.clientForTenant("AR")).thenReturn(mongoClient);
        when(mongoClient.getDatabase(DATABASE)).thenReturn(database);
        check = new DocumentMongoReadinessCheck(tenantRegistry, producer);
    }

    private HealthCheckResponse await() {
        return check.call().await().atMost(Duration.ofSeconds(5));
    }

    @Test
    void up_whenPingSucceeds() {
        when(database.runCommand(Mockito.any(Document.class)))
                .thenReturn(Uni.createFrom().item(new Document("ok", 1.0)));

        HealthCheckResponse response = await();

        assertThat(response.getName()).isEqualTo("mongodb-document");
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data)
                .containsEntry("component", "mongodb")
                .containsEntry("database", DATABASE)
                .containsEntry("host", HOST)
                .containsKey("latencyMs")
                .doesNotContainKey("error");
    }

    @Test
    void down_whenPingFails() {
        when(database.runCommand(Mockito.any(Document.class)))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("no primary")));

        HealthCheckResponse response = await();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData().orElseThrow())
                .containsEntry("database", DATABASE)
                .containsEntry("host", HOST)
                .containsEntry("error", "IllegalStateException: no primary");
    }
}

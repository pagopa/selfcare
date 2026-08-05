package it.pagopa.selfcare.document.health;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
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

    private ReactiveMongoDatabase database;
    private DocumentMongoReadinessCheck check;

    @BeforeEach
    void setUp() {
        ReactiveMongoClient mongoClient = mock(ReactiveMongoClient.class);
        database = mock(ReactiveMongoDatabase.class);
        when(mongoClient.getDatabase(DATABASE)).thenReturn(database);
        check = new DocumentMongoReadinessCheck(mongoClient, DATABASE);
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
                .containsKey("latencyMs")
                .doesNotContainKey("error");
    }

    @Test
    void down_whenPingFails() {
        when(database.runCommand(Mockito.any(Document.class)))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("no primary")));

        HealthCheckResponse response = await();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        Map<String, Object> data = response.getData().orElseThrow();
        assertThat(data)
                .containsEntry("database", DATABASE)
                .containsEntry("error", "IllegalStateException: no primary");
    }
}


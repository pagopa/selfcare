package it.pagopa.selfcare.user.health;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class UserMongoReadinessCheckTest {

    private static final String DATABASE = "selcUser";
    private static final String CONNECTION_STRING =
            "mongodb://mongo-primary.uat.local:27017,mongo-secondary.uat.local:27017/selcUser?replicaSet=rs0";

    private ReactiveMongoDatabase database;
    private UserMongoReadinessCheck check;

    @BeforeEach
    void setUp() {
        ReactiveMongoClient mongoClient = mock(ReactiveMongoClient.class);
        database = mock(ReactiveMongoDatabase.class);
        when(mongoClient.getDatabase(DATABASE)).thenReturn(database);
        check = new UserMongoReadinessCheck(mongoClient, DATABASE, CONNECTION_STRING);
    }

    private HealthCheckResponse await() {
        return check.call().await().atMost(Duration.ofSeconds(5));
    }

    @Test
    void up_whenPingSucceeds() {
        when(database.runCommand(Mockito.any(Document.class)))
                .thenReturn(Uni.createFrom().item(new Document("ok", 1.0)));

        HealthCheckResponse response = await();

        assertEquals("mongodb-user", response.getName());
        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        Map<String, Object> data = response.getData().orElseThrow();
        assertEquals("mongodb", data.get("component"));
        assertEquals(DATABASE, data.get("database"));
        assertEquals("mongo-primary.uat.local:27017,mongo-secondary.uat.local:27017", data.get("host"));
        assertTrue(data.containsKey("latencyMs"));
        assertFalse(data.containsKey("error"));
    }

    @Test
    void down_whenPingFails() {
        when(database.runCommand(Mockito.any(Document.class)))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("no primary")));

        HealthCheckResponse response = await();

        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        Map<String, Object> data = response.getData().orElseThrow();
        assertEquals(DATABASE, data.get("database"));
        assertEquals("mongo-primary.uat.local:27017,mongo-secondary.uat.local:27017", data.get("host"));
        assertEquals("IllegalStateException: no primary", data.get("error"));
    }

    @Test
    void host_showsPlaceholder_whenConnectionStringIsUnparseable() {
        ReactiveMongoClient mongoClient = mock(ReactiveMongoClient.class);
        when(mongoClient.getDatabase(DATABASE)).thenReturn(database);
        UserMongoReadinessCheck resilientCheck =
                new UserMongoReadinessCheck(mongoClient, DATABASE, "not-a-valid-connection-string");
        when(database.runCommand(Mockito.any(Document.class)))
                .thenReturn(Uni.createFrom().item(new Document("ok", 1.0)));

        HealthCheckResponse response = resilientCheck.call().await().atMost(Duration.ofSeconds(5));

        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        assertEquals(DATABASE, response.getData().orElseThrow().get("database"));
        assertEquals("n/a", response.getData().orElseThrow().get("host"));
    }
}

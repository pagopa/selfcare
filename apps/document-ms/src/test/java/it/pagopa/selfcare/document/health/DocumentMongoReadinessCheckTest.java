package it.pagopa.selfcare.document.health;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import org.bson.Document;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentMongoReadinessCheckTest {

    private TenantRegistry registry;
    private TenantMongoClientProducer producer;

    @BeforeEach
    void setUp() {
        registry = mock(TenantRegistry.class);
        producer = mock(TenantMongoClientProducer.class);
    }

    private ReactiveMongoDatabase tenant(String tenantId, String database, String connectionString) {
        when(registry.resolve(tenantId)).thenReturn(new TenantDefinition(
                new TenantDefinition.MongoDefinition("acc-" + tenantId, database, "MONGO_" + tenantId), null));
        when(registry.connectionString(tenantId)).thenReturn(Optional.ofNullable(connectionString));
        ReactiveMongoClient client = mock(ReactiveMongoClient.class);
        ReactiveMongoDatabase db = mock(ReactiveMongoDatabase.class);
        when(producer.clientForTenant(tenantId)).thenReturn(client);
        when(client.getDatabase(database)).thenReturn(db);
        return db;
    }

    private void supported(String... tenantIds) {
        when(registry.supportedTenantIds()).thenReturn(new LinkedHashSet<>(List.of(tenantIds)));
    }

    private static void pingOk(ReactiveMongoDatabase db) {
        when(db.runCommand(Mockito.any(Document.class)))
                .thenReturn(Uni.createFrom().item(new Document("ok", 1.0)));
    }

    private HealthCheckResponse call() {
        return new DocumentMongoReadinessCheck(registry, producer).call().await().atMost(Duration.ofSeconds(5));
    }

    @Test
    void up_whenSingleTenantPingSucceeds() {
        supported("AR");
        pingOk(tenant("AR", "selcDocument", "mongodb://localhost:27017"));

        HealthCheckResponse response = call();

        assertThat(response.getName()).isEqualTo("mongodb-document");
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData().orElseThrow())
                .containsEntry("component", "mongodb")
                .containsEntry("database", "AR=selcDocument")
                .containsEntry("host", "AR=localhost:27017")
                .containsKey("latencyMs")
                .doesNotContainKey("error");
    }

    @Test
    void up_whenEveryTenantPingSucceeds_listsTenantsInStableOrder() {
        supported("PNPG", "AR");
        ReactiveMongoDatabase ar = tenant("AR", "selcDocument", "mongodb://ar-host:27017");
        ReactiveMongoDatabase pnpg = tenant("PNPG", "selcDocumentPnpg", "mongodb://pnpg-host:27017");
        pingOk(ar);
        pingOk(pnpg);

        HealthCheckResponse response = call();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData().orElseThrow())
                .containsEntry("database", "AR=selcDocument,PNPG=selcDocumentPnpg")
                .containsEntry("host", "AR=ar-host:27017,PNPG=pnpg-host:27017");
        verify(ar).runCommand(Mockito.any(Document.class));
        verify(pnpg).runCommand(Mockito.any(Document.class));
    }

    @Test
    void down_whenOneTenantPingFails_reportsFailingTenant() {
        supported("AR", "PNPG");
        pingOk(tenant("AR", "selcDocument", "mongodb://ar-host:27017"));
        when(tenant("PNPG", "selcDocumentPnpg", "mongodb://pnpg-host:27017").runCommand(Mockito.any(Document.class)))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("no primary")));

        HealthCheckResponse response = call();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(String.valueOf(response.getData().orElseThrow().get("error")))
                .contains("Tenant PNPG ping failed")
                .contains("no primary");
    }

    @Test
    void down_whenClientLookupThrows() {
        supported("AR");
        tenant("AR", "selcDocument", "mongodb://localhost:27017");
        when(producer.clientForTenant("AR")).thenThrow(new IllegalStateException("Unsupported tenant"));

        HealthCheckResponse response = call();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(String.valueOf(response.getData().orElseThrow().get("error")))
                .contains("Tenant AR ping failed")
                .contains("Unsupported tenant");
    }

    @Test
    void host_neverExposesCredentials_andFallsBackWhenUnparseable() {
        supported("AR", "PNPG");
        pingOk(tenant("AR", "selcDocument", "mongodb://user:s3cr3t@ar-host:27017/selcDocument?replicaSet=rs0"));
        pingOk(tenant("PNPG", "selcDocumentPnpg", "not-a-connection-string"));

        Map<String, Object> data = call().getData().orElseThrow();

        assertThat(data).containsEntry("host", "AR=ar-host:27017,PNPG=n/a");
        assertThat(data.values()).allSatisfy(v -> assertThat(String.valueOf(v))
                .doesNotContain("s3cr3t")
                .doesNotContain("user:"));
    }

    @Test
    void down_whenNoTenantConfigured() {
        when(registry.supportedTenantIds()).thenReturn(Set.of());

        HealthCheckResponse response = call();

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData().orElseThrow())
                .containsEntry("database", "n/a")
                .containsEntry("error", "IllegalStateException: No tenant configured");
        verify(producer, never()).clientForTenant(Mockito.anyString());
    }
}

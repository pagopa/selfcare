package it.pagopa.selfcare.product.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bson.Document;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProductMongoReadinessCheckTest {

  private static final String DATABASE = "selcProduct";
  private static final String CONNECTION_STRING =
      "mongodb://mongo-primary.uat.local:27017,mongo-secondary.uat.local:27017/selcProduct?replicaSet=rs0";

  private ReactiveMongoDatabase database;
  private TenantRegistry tenantRegistry;
  private TenantMongoClientProducer tenantMongoClientProducer;
  private ProductMongoReadinessCheck check;

  @BeforeEach
  void setUp() {
    ReactiveMongoClient mongoClient = mock(ReactiveMongoClient.class);
    database = mock(ReactiveMongoDatabase.class);
    when(mongoClient.getDatabase(DATABASE)).thenReturn(database);
    tenantRegistry = mock(TenantRegistry.class);
    tenantMongoClientProducer = mock(TenantMongoClientProducer.class);
    when(tenantRegistry.supportedTenantIds()).thenReturn(Set.of("AR"));
    when(tenantRegistry.resolve("AR"))
        .thenReturn(
            new TenantDefinition(
                new TenantDefinition.MongoDefinition(
                    "test", DATABASE, "MONGODB_CONNECTION_STRING_AR"),
                null));
    when(tenantRegistry.connectionString("AR")).thenReturn(Optional.of(CONNECTION_STRING));
    when(tenantMongoClientProducer.clientForTenant("AR")).thenReturn(mongoClient);
    check = new ProductMongoReadinessCheck(tenantRegistry, tenantMongoClientProducer);
  }

  private HealthCheckResponse await() {
    return check.call().await().atMost(Duration.ofSeconds(5));
  }

  @Test
  void up_whenPingSucceeds() {
    when(database.runCommand(Mockito.any(Document.class)))
        .thenReturn(Uni.createFrom().item(new Document("ok", 1.0)));

    HealthCheckResponse response = await();

    assertThat(response.getName()).isEqualTo("mongodb-product");
    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    Map<String, Object> data = response.getData().orElseThrow();
    assertThat(data)
        .containsEntry("component", "mongodb")
        .containsEntry("database", DATABASE)
        .containsEntry("host", "mongo-primary.uat.local:27017,mongo-secondary.uat.local:27017")
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
        .containsEntry("host", "mongo-primary.uat.local:27017,mongo-secondary.uat.local:27017")
        .containsEntry("error", "IllegalStateException: no primary");
  }

  @Test
  void host_showsPlaceholder_whenConnectionStringIsUnparseable() {
    when(tenantRegistry.connectionString("AR"))
        .thenReturn(Optional.of("not-a-valid-connection-string"));
    ProductMongoReadinessCheck resilientCheck =
        new ProductMongoReadinessCheck(tenantRegistry, tenantMongoClientProducer);
    when(database.runCommand(Mockito.any(Document.class)))
        .thenReturn(Uni.createFrom().item(new Document("ok", 1.0)));

    HealthCheckResponse response = resilientCheck.call().await().atMost(Duration.ofSeconds(5));

    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    assertThat(response.getData().orElseThrow())
        .containsEntry("database", DATABASE)
        .containsEntry("host", "n/a");
  }
}

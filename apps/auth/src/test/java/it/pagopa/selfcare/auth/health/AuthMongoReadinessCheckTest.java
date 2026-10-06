package it.pagopa.selfcare.auth.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bson.Document;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

class AuthMongoReadinessCheckTest {

  @Test
  void probesTheMongoDatabaseForEverySupportedTenantWithoutRequestContext() {
    TenantRegistry registry = mock(TenantRegistry.class);
    TenantMongoClientProducer producer = mock(TenantMongoClientProducer.class);
    when(registry.supportedTenantIds()).thenReturn(Set.of("AR", "PNPG"));
    when(registry.resolve("AR")).thenReturn(tenantDefinition("selcAuth"));
    when(registry.resolve("PNPG")).thenReturn(tenantDefinition("selcAuthPnpg"));
    when(registry.connectionString(anyString())).thenReturn(Optional.empty());
    stubPing(producer, "AR", "selcAuth");
    stubPing(producer, "PNPG", "selcAuthPnpg");
    AuthMongoReadinessCheck check = new AuthMongoReadinessCheck(registry, producer);

    HealthCheckResponse response = check.call().await().atMost(Duration.ofSeconds(2));

    assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
    verify(producer).clientForTenant("AR");
    verify(producer).clientForTenant("PNPG");
  }

  @Test
  void reportsDownWhenNoTenantMongoRoutesAreConfigured() {
    TenantRegistry registry = mock(TenantRegistry.class);
    TenantMongoClientProducer producer = mock(TenantMongoClientProducer.class);
    when(registry.supportedTenantIds()).thenReturn(Set.of());
    AuthMongoReadinessCheck check = new AuthMongoReadinessCheck(registry, producer);

    HealthCheckResponse response = check.call().await().atMost(Duration.ofSeconds(2));

    assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
  }

  @Test
  void reportsMongoProbeFailureAsDown() {
    TenantRegistry registry = mock(TenantRegistry.class);
    TenantMongoClientProducer producer = mock(TenantMongoClientProducer.class);
    when(registry.supportedTenantIds()).thenReturn(Set.of("AR"));
    when(registry.resolve("AR")).thenReturn(tenantDefinition("selcAuth"));
    when(registry.connectionString("AR")).thenReturn(Optional.empty());
    ReactiveMongoClient client = mock(ReactiveMongoClient.class);
    ReactiveMongoDatabase database = mock(ReactiveMongoDatabase.class);
    when(producer.clientForTenant("AR")).thenReturn(client);
    when(client.getDatabase("selcAuth")).thenReturn(database);
    when(database.runCommand(any(Document.class)))
        .thenReturn(Uni.createFrom().failure(new IllegalStateException("unavailable")));
    AuthMongoReadinessCheck check = new AuthMongoReadinessCheck(registry, producer);

    HealthCheckResponse response = check.call().await().atMost(Duration.ofSeconds(2));

    assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
  }

  private static void stubPing(
      TenantMongoClientProducer producer, String tenantId, String databaseName) {
    ReactiveMongoClient client = mock(ReactiveMongoClient.class);
    ReactiveMongoDatabase database = mock(ReactiveMongoDatabase.class);
    when(producer.clientForTenant(tenantId)).thenReturn(client);
    when(client.getDatabase(databaseName)).thenReturn(database);
    when(database.runCommand(any(Document.class)))
        .thenReturn(Uni.createFrom().item(new Document("ok", 1)));
  }

  private static TenantDefinition tenantDefinition(String databaseName) {
    return new TenantDefinition(
        new TenantDefinition.MongoDefinition(null, databaseName, null), null);
  }
}

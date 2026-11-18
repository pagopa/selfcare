package it.pagopa.selfcare.product.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.tenant.TenantDefinition;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bson.Document;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductCdcMongoReadinessCheckTest {

  private TenantRegistry tenantRegistry;
  private TenantMongoClientProducer mongoClientProducer;
  private ReactiveMongoDatabase arDatabase;
  private ReactiveMongoDatabase pnpgDatabase;
  private ProductCdcMongoReadinessCheck check;

  @BeforeEach
  void setUp() {
    tenantRegistry = mock(TenantRegistry.class);
    mongoClientProducer = mock(TenantMongoClientProducer.class);
    arDatabase = mock(ReactiveMongoDatabase.class);
    pnpgDatabase = mock(ReactiveMongoDatabase.class);
    when(tenantRegistry.supportedTenantIds())
        .thenReturn(new LinkedHashSet<>(List.of("AR", "PNPG")));
    configureTenant("AR", "ar-products", arDatabase);
    configureTenant("PNPG", "pnpg-products", pnpgDatabase);
    check = new ProductCdcMongoReadinessCheck(tenantRegistry, mongoClientProducer);
  }

  private void configureTenant(
      String tenantId, String databaseName, ReactiveMongoDatabase database) {
    ReactiveMongoClient client = mock(ReactiveMongoClient.class);
    when(mongoClientProducer.clientForTenant(tenantId)).thenReturn(client);
    when(client.getDatabase(databaseName)).thenReturn(database);
    when(tenantRegistry.resolve(tenantId))
        .thenReturn(
            new TenantDefinition(
                new TenantDefinition.MongoDefinition(
                    tenantId, databaseName, "MONGODB_CONNECTION_STRING_" + tenantId),
                null));
    when(database.runCommand(any(Document.class)))
        .thenReturn(Uni.createFrom().item(new Document("ok", 1)));
  }

  private HealthCheckResponse await() {
    return check.call().await().atMost(Duration.ofSeconds(7));
  }

  @Test
  void upWhenEveryTenantRespondsWithoutRequestContext() {
    assertEquals(HealthCheckResponse.Status.UP, await().getStatus());

    verify(arDatabase).runCommand(new Document("ping", 1));
    verify(pnpgDatabase).runCommand(new Document("ping", 1));
    verify(mongoClientProducer, never()).tenantAwareClient();
    verify(mongoClientProducer, never()).databaseForCurrentTenant();
  }

  @Test
  void downWhenPnpgIsUnavailableEvenIfArResponds() {
    when(pnpgDatabase.runCommand(any(Document.class)))
        .thenReturn(Uni.createFrom().failure(new IllegalStateException("PNPG unavailable")));

    assertEquals(HealthCheckResponse.Status.DOWN, await().getStatus());
    verify(arDatabase).runCommand(new Document("ping", 1));
    verify(pnpgDatabase).runCommand(new Document("ping", 1));
  }

  @Test
  void downInsteadOfThrowingWhenClientResolutionFails() {
    when(mongoClientProducer.clientForTenant("AR"))
        .thenThrow(new IllegalStateException("Client unavailable"));

    assertEquals(HealthCheckResponse.Status.DOWN, await().getStatus());
  }

  @Test
  void downWhenMongoDoesNotRespondBeforeTimeout() {
    when(arDatabase.runCommand(any(Document.class))).thenReturn(Uni.createFrom().nothing());

    assertEquals(HealthCheckResponse.Status.DOWN, await().getStatus());
  }

  @Test
  void downWhenNoTenantIsConfigured() {
    when(tenantRegistry.supportedTenantIds()).thenReturn(Set.of());

    assertEquals(HealthCheckResponse.Status.DOWN, await().getStatus());
  }
}

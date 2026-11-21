package it.pagopa.selfcare.user.health;

import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import java.time.Duration;
import java.util.Set;
import org.bson.Document;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class UserMongoReadinessCheckTest {
  UserMongoReadinessCheck check;
  ReactiveMongoDatabase ar;
  ReactiveMongoDatabase pnpg;

  @BeforeEach void setup() {
    check = new UserMongoReadinessCheck();
    check.registry = mock(TenantRegistry.class, RETURNS_DEEP_STUBS);
    check.clients = mock(TenantMongoClientProducer.class, RETURNS_DEEP_STUBS);
    when(check.registry.supportedTenantIds()).thenReturn(Set.of("AR", "PNPG"));
    ar = database("AR");
    pnpg = database("PNPG");
  }

  private ReactiveMongoDatabase database(String tenant) {
    when(check.registry.resolve(tenant)).thenReturn(new it.pagopa.selfcare.tenant.TenantDefinition(
        new it.pagopa.selfcare.tenant.TenantDefinition.MongoDefinition("local", "users" + tenant, "TEST_MONGO"), null));
    ReactiveMongoDatabase database = mock(ReactiveMongoDatabase.class);
    when(check.clients.clientForTenant(tenant).getDatabase("users" + tenant)).thenReturn(database);
    when(database.runCommand(any(Document.class))).thenReturn(Uni.createFrom().item(new Document("ok", 1)));
    return database;
  }

  @Test void probesEveryTenantWithoutRequestContext() {
    var response = check.call().await().atMost(Duration.ofSeconds(5));
    assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
    assertEquals("usersPNPG", response.getData().orElseThrow().get("PNPG.database"));
    verify(ar).runCommand(new Document("ping", 1));
    verify(pnpg).runCommand(new Document("ping", 1));
  }

  @Test void reportsFailingTenantButStillProbesTheOther() {
    when(ar.runCommand(any(Document.class))).thenReturn(Uni.createFrom().failure(new IllegalStateException("unavailable")));
    var response = check.call().await().atMost(Duration.ofSeconds(5));
    assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
    assertTrue(response.getData().orElseThrow().get("error").toString().contains("[AR]"));
    verify(pnpg).runCommand(new Document("ping", 1));
  }

  @Test void boundsHangingTenantProbe() {
    when(pnpg.runCommand(any(Document.class))).thenReturn(Uni.createFrom().nothing());
    var response = check.call().await().atMost(Duration.ofSeconds(5));
    assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
    assertTrue(response.getData().orElseThrow().get("error").toString().contains("[PNPG]"));
  }
}

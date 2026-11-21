package it.pagopa.selfcare.user.internalevents;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.internalevents.model.InstitutionUpdatedEvent;
import it.pagopa.selfcare.internalevents.model.InternalEvent;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.user.service.UserInstitutionService;
import jakarta.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class InstitutionUpdatedEventHandlerTest {
  @Inject InstitutionUpdatedEventHandler handler;
  @Inject TenantContext context;
  @InjectMock UserInstitutionService service;

  private InstitutionUpdatedEvent event(String tenant) {
    var event = new InstitutionUpdatedEvent();
    event.setEventTenant(tenant);
    event.setInstitutionId("institution");
    event.setDescription("description");
    return event;
  }

  @Test void sourceTenantControlsBackgroundDatabaseContext() throws Exception {
    when(service.updateInstitutionDescription(anyString(), any())).thenAnswer(invocation -> {
      assertEquals("PNPG", context.requiredTenantId());
      return Uni.createFrom().item(1L);
    });
    var event = event(null);
    CompletableFuture.runAsync(() -> handler.handle("PNPG", event)).get(5, TimeUnit.SECONDS);
    assertEquals(InternalEvent.Status.COMPLETE, event.getEventTargetStatus());
  }

  @Test void mismatchedEventIsDeadLetteredWithoutCallingService() {
    var event = event("PNPG");
    handler.handle("AR", event);
    assertEquals(InternalEvent.Status.DEAD_LETTER, event.getEventTargetStatus());
    assertEquals("TenantMismatch", event.getEventDeadLetterReason());
    verifyNoInteractions(service);
    assertFalse(context.isInitialized());
  }

  @Test void clearsTenantEvenWhenProcessingFails() {
    when(service.updateInstitutionDescription(anyString(), any()))
        .thenReturn(Uni.createFrom().failure(new IllegalStateException("failed")));
    assertThrows(IllegalStateException.class, () -> handler.handle("AR", event("AR")));
    assertFalse(context.isInitialized());
  }

  @Test void concurrentReceiversKeepTheirOwnTenant() throws Exception {
    var started = new CountDownLatch(2);
    var observed = new ConcurrentHashMap<String, String>();
    when(service.updateInstitutionDescription(anyString(), any())).thenAnswer(invocation -> {
      observed.put(invocation.getArgument(0), context.requiredTenantId());
      started.countDown();
      if (!started.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Concurrent consumers did not start");
      }
      return Uni.createFrom().item(1L);
    });
    var ar = event("AR");
    ar.setInstitutionId("AR");
    var pnpg = event("PNPG");
    pnpg.setInstitutionId("PNPG");
    CompletableFuture.allOf(
        CompletableFuture.runAsync(() -> handler.handle("AR", ar)),
        CompletableFuture.runAsync(() -> handler.handle("PNPG", pnpg))).get(10, TimeUnit.SECONDS);
    assertEquals(Map.of("AR", "AR", "PNPG", "PNPG"), observed);
    assertEquals(InternalEvent.Status.COMPLETE, ar.getEventTargetStatus());
    assertEquals(InternalEvent.Status.COMPLETE, pnpg.getEventTargetStatus());
  }
}

package it.pagopa.selfcare.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.context.api.ManagedExecutorConfig;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TenantMailJobTest {
  @Inject TenantMailJob job;
  @Inject TenantContext context;
  @InjectMock MailService mailService;

  @Inject
  @ManagedExecutorConfig(propagated = {}, cleared = ThreadContext.ALL_REMAINING)
  ManagedExecutor executor;

  @Test
  void concurrentJobsHaveIndependentContextsAndDoNotChangeTheCaller() throws Exception {
    context.setTenantId("AR");
    var started = new CountDownLatch(2);
    var observed = new ConcurrentHashMap<String, String>();
    when(mailService.sendOneMail(anyString(), anyString(), anyString(), anyMap()))
        .thenAnswer(invocation -> {
          String userId = invocation.getArgument(0);
          observed.put(userId, context.requiredTenantId());
          started.countDown();
          if (!started.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent mail jobs did not start");
          }
          return Uni.createFrom().voidItem();
        });

    var ar = executor.runAsync(() -> runJobAndCheckCleanup("AR"));
    var pnpg = executor.runAsync(() -> runJobAndCheckCleanup("PNPG"));
    CompletableFuture.allOf(ar, pnpg).get(10, TimeUnit.SECONDS);

    assertEquals(Map.of("AR", "AR", "PNPG", "PNPG"), observed);
    assertEquals("AR", context.requiredTenantId());
  }

  @Test
  void failedMailAlsoTerminatesItsContext() {
    when(mailService.sendOneMail(anyString(), anyString(), anyString(), anyMap()))
        .thenReturn(Uni.createFrom().failure(new IllegalStateException("Mail unavailable")));

    Uni.createFrom().completionStage(executor.runAsync(() -> runJobAndCheckCleanup("PNPG")))
        .await().atMost(Duration.ofSeconds(5));
  }

  private void runJobAndCheckCleanup(String tenant) {
    assertFalse(context.isInitialized());
    job.send(tenant, tenant, "test@example.test", "template", Map.of());
    assertFalse(context.isInitialized());
  }
}

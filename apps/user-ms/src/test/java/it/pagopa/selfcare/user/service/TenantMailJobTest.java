package it.pagopa.selfcare.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TenantMailJobTest {
  @Inject TenantMailJob job;
  @Inject TenantContext context;
  @InjectMock MailService mailService;

  @Test
  void forwardsTheCapturedTenantWithoutChangingTheCallerContext() {
    context.setTenantId("AR");
    when(mailService.sendOneMail("PNPG", "user", "test@example.test", "template", Map.of()))
        .thenReturn(Uni.createFrom().voidItem());

    job.send("PNPG", "user", "test@example.test", "template", Map.of());
    verify(mailService).sendOneMail("PNPG", "user", "test@example.test", "template", Map.of());
    assertEquals("AR", context.requiredTenantId());
  }

  @Test
  void containsMailFailureWithoutChangingTheCallerContext() {
    context.setTenantId("AR");
    when(mailService.sendOneMail("PNPG", "user", "test@example.test", "template", Map.of()))
        .thenReturn(Uni.createFrom().failure(new IllegalStateException("Mail unavailable")));

    job.send("PNPG", "user", "test@example.test", "template", Map.of());
    verify(mailService).sendOneMail("PNPG", "user", "test@example.test", "template", Map.of());
    assertEquals("AR", context.requiredTenantId());
  }
}

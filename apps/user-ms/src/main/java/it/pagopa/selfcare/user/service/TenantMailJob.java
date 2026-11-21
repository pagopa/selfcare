package it.pagopa.selfcare.user.service;

import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class TenantMailJob {
  @Inject TenantContext context;
  @Inject MailService mailService;

  @ActivateRequestContext
  public void send(
      String tenant, String userId, String email, String templateId, Map<String, String> dataModel) {
    try {
      context.setTenantId(tenant);
      // Keep this job's request context alive through the client filters and retries.
      mailService.sendOneMail(userId, email, templateId, dataModel).await().indefinitely();
      log.debug("Mail job completed for tenant {}", tenant);
    } catch (RuntimeException failure) {
      log.error("Mail job failed for tenant {}", tenant, failure);
    } finally {
      context.clear();
    }
  }
}

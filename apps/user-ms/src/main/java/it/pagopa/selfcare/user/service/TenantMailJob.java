package it.pagopa.selfcare.user.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class TenantMailJob {
  @Inject MailService mailService;

  public void send(
      String tenant, String userId, String email, String templateId, Map<String, String> dataModel) {
    try {
      mailService.sendOneMail(tenant, userId, email, templateId, dataModel).await().indefinitely();
    } catch (RuntimeException failure) {
      log.error("Mail job failed for tenant {}: {}", tenant, failure.getClass().getSimpleName());
    }
  }
}

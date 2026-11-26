package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.client.OneMailClient;
import it.pagopa.selfcare.user.conf.TenantNotificationConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.one_mail_json.model.EmailHighPriorityBodyDTO;
import org.openapi.quarkus.one_mail_json.model.EmailHighPriorityBodyDTOAllOfFrom;
import org.openapi.quarkus.one_mail_json.model.EmailHighPriorityBodyDTOAllOfTo;

import java.time.Duration;
import java.util.Map;

@Slf4j
@ApplicationScoped
public class OneMailServiceImpl implements MailService {

    @RestClient
    @Inject
    OneMailClient oneMailClient;

    @Inject TenantNotificationConfig tenantConfig;

    @ConfigProperty(name = "user-ms.retry.min-backoff")
    Integer retryMinBackOff;

    @ConfigProperty(name = "user-ms.retry.max-backoff")
    Integer retryMaxBackOff;

    @ConfigProperty(name = "user-ms.retry")
    Integer maxRetry;

    @Override
    public Uni<Void> sendOneMail(String tenant, String userId, String email, String templateId, Map<String, String> templateAttributes) {

      var settings = tenantConfig.forTenant(tenant);
      if (!settings.mailEnabled()) {
        log.debug("Email sending is disabled for tenant {}", tenant);
        return Uni.createFrom().voidItem();
      }

      log.info("Sending email. tenant={}, userId={}, templateId={}", tenant, userId, templateId);

      EmailHighPriorityBodyDTO emailRequest = EmailHighPriorityBodyDTO.builder()
        .from(new EmailHighPriorityBodyDTOAllOfFrom().email(settings.sender()))
        .to(new EmailHighPriorityBodyDTOAllOfTo().email(email))
        .templateContent(Map.of(
          "templateId", templateId,
          "templateAttributes", templateAttributes
        ))
        .build();

      return Uni.createFrom().deferred(() -> oneMailClient.sendHighPriority(settings.apiKey(), false, emailRequest))
        .invoke(() -> log.info("OneMail call completed successfully for tenant {}, userId={}", tenant, userId))
        .onFailure().invoke(failure -> log.error("OneMail call failed for tenant {}, userId={}: {}",
            tenant, userId, failure instanceof WebApplicationException webFailure
                ? "HTTP " + webFailure.getResponse().getStatus() : failure.getClass().getSimpleName()))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff))
        .atMost(maxRetry)
        .replaceWithVoid();

    }

}

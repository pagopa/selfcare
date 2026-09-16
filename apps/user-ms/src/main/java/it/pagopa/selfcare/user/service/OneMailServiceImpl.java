package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.one_mail_json.api.EmailsApi;
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
    EmailsApi emailsApi;

    @ConfigProperty(name = "user-ms.mail.no-reply")
    String senderMail;

    @ConfigProperty(name = "user-ms.retry.min-backoff")
    Integer retryMinBackOff;

    @ConfigProperty(name = "user-ms.retry.max-backoff")
    Integer retryMaxBackOff;

    @ConfigProperty(name = "user-ms.retry")
    Integer maxRetry;

    @Override
    public Uni<Void> sendOneMail(String userId, String email, String templateId, Map<String, String> templateAttributes) {

      log.info("Sending email. userId={}, email={}, templateId={}", userId, email, templateId);

      EmailHighPriorityBodyDTO emailRequest = EmailHighPriorityBodyDTO.builder()
        .from(new EmailHighPriorityBodyDTOAllOfFrom().email(senderMail))
        .to(new EmailHighPriorityBodyDTOAllOfTo().email(email))
        .templateContent(Map.of(
          "templateId", templateId,
          "templateAttributes", templateAttributes
        ))
        .build();

      return emailsApi
        .v1EmailsSendHighPost(false, emailRequest)
        .invoke(() -> log.info("OneMail call completed successfully for {}", email))
        .onFailure()
        .invoke(t -> log.error("OneMail call failed for {}: {}", email, t.getMessage(), t))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff))
        .atMost(maxRetry)
        .onFailure()
        .recoverWithNull()
        .replaceWithVoid();

    }

}


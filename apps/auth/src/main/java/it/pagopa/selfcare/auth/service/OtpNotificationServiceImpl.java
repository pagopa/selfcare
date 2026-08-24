package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.client.OneMailEmailsApi;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import it.pagopa.selfcare.auth.util.GeneralUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.time.Duration;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.one_mail_json.model.EmailAddress;
import org.openapi.quarkus.one_mail_json.model.EmailHighPriorityBodyDTO;
import org.openapi.quarkus.one_mail_json.model.EmailStatusItemResponseDTO;
import org.openapi.quarkus.one_mail_json.model.EmailSuccessResponseDTO;

import java.time.Duration;
import java.util.Map;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class OtpNotificationServiceImpl implements OtpNotificationService {

  private static final String SELFCARE_USER_OTP_TEMPLATE = "selfcare_user_otp";
  @ConfigProperty(name = "auth-ms.retry.min-backoff")
  Integer retryMinBackOff;

  @ConfigProperty(name = "auth-ms.retry.max-backoff")
  Integer retryMaxBackOff;

  @ConfigProperty(name = "auth-ms.retry")
  Integer maxRetry;

  @ConfigProperty(name = "auth-ms.mail-sender")
  String senderMail;

  @RestClient @Inject
  OneMailEmailsApi oneMailEmailsApi;

  @Override
  public Uni<String> sendOtpEmail(String userId, String email, String otp, String name) {

    log.info("Sending OTP email. userId={}, email={}", userId, email);

    EmailHighPriorityBodyDTO emailRequest = EmailHighPriorityBodyDTO.builder()
      .from(new EmailAddress().email(senderMail))
      .to(new EmailAddress().email(email))
      .templateContent(Map.of(
        "templateId", SELFCARE_USER_OTP_TEMPLATE,
        "templateAttributes", Map.of(
          "name", name,
          "otp", otp
        )
      ))
      .build();

    return oneMailEmailsApi
      .v1EmailsSendHighPost(false, emailRequest)
      .map(EmailSuccessResponseDTO::getRequestId)
      .invoke(() -> log.info("OneMail call completed successfully for {}", email))
      .onFailure()
      .invoke(t -> log.error("OneMail call failed for {}: {}", email, t.getMessage(), t))
      .onFailure(GeneralUtils::checkIfIsRetryableException)
      .retry()
      .withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff))
      .atMost(maxRetry)
      .onFailure(WebApplicationException.class)
      .transform(GeneralUtils::extractExceptionFromWebAppException)
      .onFailure()
      .recoverWithNull();
  }

  @Override
  public Uni<EmailStatusItemResponseDTO> getOtpMailInfo(String mailRequestId) {
    return oneMailEmailsApi
      .v1EmailsStatusesGet(mailRequestId)
      .onFailure()
      .invoke(t -> log.error("OneMail call failed for requestId {}: {}", mailRequestId, t.getMessage(), t))
      .onFailure(GeneralUtils::checkIfIsRetryableException)
      .retry()
      .withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff))
      .atMost(maxRetry)
      .onFailure(WebApplicationException.class)
      .transform(GeneralUtils::extractExceptionFromWebAppException)
      .map(
        responses ->
          responses.stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Mail status not found for requestId " + mailRequestId)));
  }

}

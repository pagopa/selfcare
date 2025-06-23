package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.util.GeneralUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.internal_ms_user_json.api.UserApi;
import org.openapi.quarkus.internal_ms_user_json.model.SendEmailOtpDto;

import java.time.Duration;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class OtpNotificationServiceImpl implements OtpNotificationService {

  @ConfigProperty(name = "auth-ms.retry.min-backoff")
  Integer retryMinBackOff;

  @ConfigProperty(name = "auth-ms.retry.max-backoff")
  Integer retryMaxBackOff;

  @ConfigProperty(name = "auth-ms.retry")
  Integer maxRetry;

  @RestClient @Inject UserApi internalUserApi;

  @Override
  public Uni<Void> sendOtpEmail(String userId, String email, String otp) {
    SendEmailOtpDto sendEmailOtpDto =
        SendEmailOtpDto.builder().institutionalEmail(email).otp(otp).build();
    return internalUserApi
        .sendEmailOtp(userId, sendEmailOtpDto)
        .onFailure(GeneralUtils::checkIfIsRetryableException)
        .retry()
        .withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff))
        .atMost(maxRetry)
        .onFailure(WebApplicationException.class)
        .transform(GeneralUtils::extractExceptionFromWebAppException)
        .replaceWithVoid();
  }
}

package it.pagopa.selfcare.auth.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import it.pagopa.selfcare.auth.client.InternalUserMsApi;
import it.pagopa.selfcare.auth.client.OneMailEmailsApi;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.util.OtpUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.one_mail_json.model.EmailStatusItemResponseDTO;
import org.openapi.quarkus.one_mail_json.model.EmailSuccessResponseDTO;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@QuarkusTest
public class OtpNotificationServiceTest {

  @Inject OtpNotificationService otpNotificationService;

  @RestClient @InjectMock
  OneMailEmailsApi oneMailEmailsApi;

  private UserClaims getUserClaims() {
    return UserClaims.builder()
        .uid(UUID.randomUUID().toString())
        .name("name")
        .familyName("family")
        .fiscalCode("fiscalCode")
        .build();
  }

  @Test
  void sendOtpEmail() {
    UserClaims input = getUserClaims();
    String otp = OtpUtils.generateOTP();
    String email = "test@test.com";
    when(oneMailEmailsApi.v1EmailsSendHighPost(anyBoolean(), any()))
        .thenReturn(Uni.createFrom().item(new EmailSuccessResponseDTO()));
    otpNotificationService
        .sendOtpEmail(input.getUid(), email, otp, input.getName())
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted();
  }

  @Test
  void fireAndForgetWhileSendingOtpEmail() {
    UserClaims input = getUserClaims();
    String otp = OtpUtils.generateOTP();
    String email = "test@test.com";
    when(oneMailEmailsApi.v1EmailsSendHighPost(anyBoolean(), any()))
        .thenReturn(
            Uni.createFrom().failure(new WebApplicationException(Response.status(500).build())));
    otpNotificationService
        .sendOtpEmail(input.getUid(), email, otp, input.getName())
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted();
  }

  @Test
  void getOtpMailInfo() {
    String requestId = "requestId";

    EmailStatusItemResponseDTO response = EmailStatusItemResponseDTO.builder()
      .emailId(requestId)
      .build();

    when(oneMailEmailsApi.v1EmailsStatusesGet(requestId))
      .thenReturn(Uni.createFrom().item(List.of(response)));

    otpNotificationService
      .getOtpMailInfo(requestId)
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertCompleted()
      .assertItem(response);
  }

  @Test
  void getOtpMailInfo_NotFoundWhenEmptyList() {
    String requestId = "requestId";

    when(oneMailEmailsApi.v1EmailsStatusesGet(requestId))
      .thenReturn(Uni.createFrom().item(List.of()));

    otpNotificationService
      .getOtpMailInfo(requestId)
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertFailedWith(
        ResourceNotFoundException.class,
        "Mail status not found for requestId " + requestId);
  }

  @Test
  void getOtpMailInfo_OneMailRetraiable() {
    String requestId = "requestId";

    when(oneMailEmailsApi.v1EmailsStatusesGet(requestId))
      .thenReturn(
        Uni.createFrom()
          .failure(
            new WebApplicationException(
              Response.status(Response.Status.CONFLICT).build())));

    otpNotificationService
      .getOtpMailInfo(requestId)
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertFailedWith(InternalException.class);

  }
}

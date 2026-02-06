package it.pagopa.selfcare.auth.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.auth.client.InternalUserMsApi;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.util.OtpUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class OtpNotificationServiceTest {

  @Inject OtpNotificationService otpNotificationService;

  @RestClient @InjectMock InternalUserMsApi internalUserApi;

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
    when(internalUserApi.sendEmailOtp(anyString(), any()))
        .thenReturn(Uni.createFrom().item(Response.accepted().build()));
    otpNotificationService
        .sendOtpEmail(input.getUid(), email, otp)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted();
  }

  @Test
  void fireAndForgetWhileSendingOtpEmail() {
    UserClaims input = getUserClaims();
    String otp = OtpUtils.generateOTP();
    String email = "test@test.com";
    when(internalUserApi.sendEmailOtp(anyString(), any()))
        .thenReturn(
            Uni.createFrom().failure(new WebApplicationException(Response.status(500).build())));
    otpNotificationService
        .sendOtpEmail(input.getUid(), email, otp)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted();
  }
}

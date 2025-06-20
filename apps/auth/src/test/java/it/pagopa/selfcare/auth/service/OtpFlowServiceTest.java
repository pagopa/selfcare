package it.pagopa.selfcare.auth.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.model.OtpStatus;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.util.OtpUtils;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
public class OtpFlowServiceTest {

  @Inject OtpFlowService otpFlowService;

  private UserClaims getUserClaims() {
    return UserClaims.builder()
        .uid(UUID.randomUUID().toString())
        .name("name")
        .familyName("family")
        .fiscalCode("fiscalCode")
        .build();
  }

  @Test
  void persistNewOtpFlow() {
    UserClaims input = getUserClaims();
    String otp = OtpUtils.generateOTP();
    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    OtpFlow created =
        otpFlowService
            .createNewOtpFlow(input.getUid(), otp)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertEquals(DigestUtils.md5Hex(otp), created.getOtp());
    Assertions.assertEquals(0, created.getAttempts());
    Assertions.assertEquals(OtpStatus.PENDING, created.getStatus());
    Assertions.assertEquals(input.getUid(), created.getUserId());
  }

  @Test
  void failureWhilePersistNewOtpFlow() {
    String exceptionDesc = "Cannot persist Otp Flow";
    UserClaims input = getUserClaims();
    String otp = OtpUtils.generateOTP();
    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    when(OtpFlow.persist(any(OtpFlow.class), any()))
        .thenReturn(Uni.createFrom().failure(new Exception(exceptionDesc)));
    otpFlowService
        .createNewOtpFlow(input.getUid(), otp)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed()
        .assertFailedWith(Exception.class, exceptionDesc);
  }

  @Test
  void findLastOtpFlowByUserId() {
    UserClaims input = getUserClaims();
    OtpFlow otpFlow =
        OtpFlow.builder().userId(input.getUid()).uuid("uuid").status(OtpStatus.PENDING).build();
    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResult()).thenReturn(Uni.createFrom().item(otpFlow));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);
    otpFlowService
        .findLastOtpFlowByUserId(input.getUid())
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted();
  }

  @Test
  void failureWhileFindinglastOtpFlow() {
    String exceptionDesc = "Cannot find Otp Flow";
    UserClaims input = getUserClaims();
    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResult()).thenReturn(Uni.createFrom().failure(new Exception(exceptionDesc)));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);

    otpFlowService
        .findLastOtpFlowByUserId(input.getUid())
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed()
        .assertFailedWith(Exception.class, exceptionDesc);
  }
}

package it.pagopa.selfcare.auth.service;

import io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.auth.controller.response.TokenResponse;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.exception.ConflictException;
import it.pagopa.selfcare.auth.exception.OtpForbiddenException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import it.pagopa.selfcare.auth.model.OtpStatus;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.model.otp.OtpInfo;
import it.pagopa.selfcare.auth.util.OtpUtils;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.util.*;

import org.apache.commons.codec.digest.DigestUtils;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
public class OtpFlowServiceTest {

  @InjectMock SessionService sessionService;
  @InjectMock UserService userService;

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
  void returnEmptyOtpFlow_whenHandlingNoneFFOtpFlow() {
    UserClaims input = getUserClaims();
    Optional<OtpInfo> created =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertEquals(created, Optional.empty());
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

  @Test
  public void testVerifyOtp_NotFound() {
    String otpUid = "test-uuid";
    String otp = "test-otp";
    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
            Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.empty()));
    when(OtpFlow.find(any())).thenReturn(query);

    otpFlowService.verifyOtp(otpUid, otp)
            .subscribe().withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(ResourceNotFoundException.class, "Cannot find OtpFlow");
  }

  @Test
  public void testVerifyOtp_Success() {
    String otpUid = "test-uuid";
    String otp = "test-otp";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setUuid(otpUid);
    otpFlow.setOtp(DigestUtils.md5Hex(otp));
    otpFlow.setExpiresAt(OffsetDateTime.now().plusMinutes(5));
    otpFlow.setStatus(OtpStatus.PENDING);
    otpFlow.setAttempts(0);
    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
            Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(otpFlow)));
    when(OtpFlow.find(any())).thenReturn(query);
    ReactivePanacheUpdate update =
            Mockito.mock(ReactivePanacheUpdate.class);
    when(update.where(anyString(), any(String.class))).thenReturn(Uni.createFrom().item(1L));
    when(OtpFlow.update(anyString(), (Object) any())).thenReturn(update);
    when(userService.getUserClaimsFromPdv(anyString())).thenReturn(Uni.createFrom().item(UserClaims.builder().build()));
    when(sessionService.generateSessionToken(any())).thenReturn(Uni.createFrom().item("session-token"));
    TokenResponse response = otpFlowService.verifyOtp(otpUid, otp)
            .subscribe().withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();

    Assertions.assertEquals("session-token", response.getSessionToken());
  }

  @Test
  public void testVerifyOtp_InvalidOtp() {
    String otpUid = "test-uuid";
    String otp = "wrong-otp";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setUuid(otpUid);
    otpFlow.setOtp("hashed-otp");
    otpFlow.setExpiresAt(OffsetDateTime.now().plusMinutes(5));
    otpFlow.setAttempts(1);
    otpFlow.setStatus(OtpStatus.PENDING);

    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
            Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(otpFlow)));
    when(OtpFlow.find(any())).thenReturn(query);
    ReactivePanacheUpdate update =
            Mockito.mock(ReactivePanacheUpdate.class);
    when(update.where(anyString(), any(String.class))).thenReturn(Uni.createFrom().item(1L));
    when(OtpFlow.update(anyString(), (Object) any())).thenReturn(update);
    when(userService.getUserClaimsFromPdv(anyString())).thenReturn(Uni.createFrom().item(UserClaims.builder().build()));
    when(sessionService.generateSessionToken(any())).thenReturn(Uni.createFrom().item("session-token"));

    otpFlowService.verifyOtp(otpUid, otp)
            .subscribe().withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(OtpForbiddenException.class, "Wrong Otp Code");
  }

  @Test
  public void testVerifyOtp_MaxAttemptsReached() {
    String otpUid = "test-uuid";
    String otp = "otp";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setUuid(otpUid);
    otpFlow.setOtp(DigestUtils.md5Hex(otp));
    otpFlow.setExpiresAt(OffsetDateTime.now().plusMinutes(5));
    otpFlow.setAttempts(5);
    otpFlow.setStatus(OtpStatus.PENDING);

    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
            Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(otpFlow)));
    when(OtpFlow.find(any())).thenReturn(query);
    ReactivePanacheUpdate update =
            Mockito.mock(ReactivePanacheUpdate.class);
    when(update.where(anyString(), any(String.class))).thenReturn(Uni.createFrom().item(1L));
    when(OtpFlow.update(anyString(), (Object) any())).thenReturn(update);
    when(userService.getUserClaimsFromPdv(anyString())).thenReturn(Uni.createFrom().item(UserClaims.builder().build()));
    when(sessionService.generateSessionToken(any())).thenReturn(Uni.createFrom().item("session-token"));

    otpFlowService.verifyOtp(otpUid, otp)
            .subscribe().withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(OtpForbiddenException.class, "Max attempts reached");
  }

  @Test
  public void testVerifyOtp_ExpiredOtp() {
    String otpUid = "test-uuid";
    String otp = "test-otp";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setUuid(otpUid);
    otpFlow.setOtp("test-otp");
    otpFlow.setAttempts(1);
    otpFlow.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
    otpFlow.setStatus(OtpStatus.PENDING);

    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
            Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(otpFlow)));
    when(OtpFlow.find(any())).thenReturn(query);
    ReactivePanacheUpdate update =
            Mockito.mock(ReactivePanacheUpdate.class);
    when(update.where(anyString(), any(String.class))).thenReturn(Uni.createFrom().item(1L));
    when(OtpFlow.update(anyString(), (Object) any())).thenReturn(update);
    when(userService.getUserClaimsFromPdv(anyString())).thenReturn(Uni.createFrom().item(UserClaims.builder().build()));
    when(sessionService.generateSessionToken(any())).thenReturn(Uni.createFrom().item("session-token"));

    otpFlowService.verifyOtp(otpUid, otp)
            .subscribe().withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(ConflictException.class, "Otp is expired");
  }
}

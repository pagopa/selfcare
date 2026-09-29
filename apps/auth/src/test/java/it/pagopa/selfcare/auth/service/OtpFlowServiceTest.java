package it.pagopa.selfcare.auth.service;

import io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.auth.context.AuthTenantContext;
import it.pagopa.selfcare.auth.controller.response.OtpMailInfoResponse;
import it.pagopa.selfcare.auth.controller.response.TokenResponse;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.exception.ConflictException;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.exception.OtpForbiddenException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import it.pagopa.selfcare.auth.model.OtpStatus;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.model.otp.OtpInfo;
import it.pagopa.selfcare.auth.util.OtpUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapi.quarkus.one_mail_json.model.EmailAddress;
import org.openapi.quarkus.one_mail_json.model.EmailStatus;
import org.openapi.quarkus.one_mail_json.model.EmailStatusItemResponseDTO;
import org.openapi.quarkus.one_mail_json.model.EmailStatusItemResponseDTOHistoryInner;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class OtpFlowServiceTest {

  @InjectMock SessionService sessionService;
  @InjectMock UserService userService;
  @InjectMock OtpNotificationService otpNotificationService;
  @InjectMock AuthTenantContext tenantContext;

  @Inject OtpFlowService otpFlowService;

  @BeforeEach
  void setUpTenant() {
    when(tenantContext.getTenantId()).thenReturn("AR");
  }

  private UserClaims getUserClaims() {
    return UserClaims.builder()
        .uid(UUID.randomUUID().toString())
        .name("name")
        .familyName("family")
        .fiscalCode("fiscalCode")
        .tenantId("AR")
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
    Assertions.assertEquals("AR", created.getTenantId());
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
        OtpFlow.builder().userId(input.getUid()).uuid("uuid").tenantId("AR")
            .status(OtpStatus.PENDING).build();
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
  void interleavedTenantsUseSeparateReadPredicates() {
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
    List<Document> filters = new ArrayList<>();
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenAnswer(invocation -> {
      filters.add(invocation.getArgument(0));
      return query;
    });
    when(query.firstResult()).thenReturn(Uni.createFrom().nullItem());
    when(tenantContext.getTenantId()).thenReturn("AR", "PNPG");

    otpFlowService.findLastOtpFlowByUserId("shared-user").await().indefinitely();
    otpFlowService.findLastOtpFlowByUserId("shared-user").await().indefinitely();

    Assertions.assertEquals("AR",
        ((Document) ((List<?>) ((Document) ((List<?>) filters.get(0).get("$and"))
            .get(1)).get("$or")).get(0)).getString("tenantId"));
    Assertions.assertEquals("PNPG", filters.get(1).getString("tenantId"));
    Assertions.assertFalse(filters.get(1).containsKey("$or"));
  }

  @Test
  void missingTenantContextCannotAccessOtpFlows() {
    when(tenantContext.getTenantId()).thenReturn(null);
    Assertions.assertThrows(IllegalStateException.class,
        () -> otpFlowService.findLastOtpFlowByUserId("shared-user"));
    Assertions.assertThrows(IllegalStateException.class,
        () -> otpFlowService.createNewOtpFlow("shared-user", "123456"));
    Assertions.assertThrows(IllegalStateException.class,
        () -> otpFlowService.getOtpInfo("shared-user", null));
    Assertions.assertThrows(IllegalStateException.class,
        () -> otpFlowService.getOtpMailInfo("request-id"));
  }

  @Test
  void verifyOtpRejectsUnmatchedTenantUpdate() {
    String otp = "123456";
    OtpFlow flow = OtpFlow.builder().uuid("shared-uuid").userId("shared-user").tenantId("AR")
        .otp(DigestUtils.md5Hex(otp)).status(OtpStatus.PENDING).attempts(0)
        .expiresAt(OffsetDateTime.now().plusMinutes(5)).build();
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(flow)));
    when(OtpFlow.find(any(Document.class))).thenReturn(query);
    ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
    when(OtpFlow.update(anyString(), (Object) any())).thenReturn(update);
    when(update.where(any(Document.class))).thenReturn(Uni.createFrom().item(0L));
    when(userService.getUserClaimsFromPdv(anyString()))
        .thenReturn(Uni.createFrom().item(UserClaims.builder().build()));
    when(sessionService.generateSessionToken(any()))
        .thenReturn(Uni.createFrom().item("token"));

    otpFlowService.verifyOtp("shared-uuid", otp).subscribe()
        .withSubscriber(UniAssertSubscriber.create()).assertFailedWith(InternalException.class);

    ArgumentCaptor<Document> filter = ArgumentCaptor.forClass(Document.class);
    Mockito.verify(update).where(filter.capture());
    Assertions.assertEquals(new Document("uuid", "shared-uuid").append("tenantId", "AR"),
        filter.getValue());
  }

  @Test
  void verifyOtpRejectsLegacyFlowBeforeTokenOrUpdate() {
    OtpFlow legacyFlow = OtpFlow.builder().uuid("legacy-uuid").status(OtpStatus.PENDING).build();
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.find(any(Document.class))).thenReturn(query);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(legacyFlow)));

    otpFlowService.verifyOtp("legacy-uuid", "123456")
        .subscribe().withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(IllegalStateException.class, "OTP flow requires tenant backfill before verification");

    Mockito.verifyNoInteractions(userService, sessionService);
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

    otpFlowService
        .verifyOtp(otpUid, otp)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(ResourceNotFoundException.class, "Cannot find OtpFlow");
  }

  @Test
  public void testVerifyOtp_Success() {
    String otpUid = "test-uuid";
    String otp = "test-otp";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setTenantId("AR");
    otpFlow.setUuid(otpUid);
    otpFlow.setUserId("user-id");
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
    ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
    when(update.where(any(Document.class))).thenReturn(Uni.createFrom().item(1L));
    when(OtpFlow.update(anyString(), (Object) any())).thenReturn(update);
    when(userService.getUserClaimsFromPdv(anyString()))
        .thenReturn(Uni.createFrom().item(UserClaims.builder().build()));
    when(sessionService.generateSessionToken(any()))
        .thenReturn(Uni.createFrom().item("session-token"));
    TokenResponse response =
        otpFlowService
            .verifyOtp(otpUid, otp)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();

    Assertions.assertEquals("session-token", response.getSessionToken());
    ArgumentCaptor<UserClaims> userClaimsCaptor = ArgumentCaptor.forClass(UserClaims.class);
    verify(sessionService).generateSessionToken(userClaimsCaptor.capture());
    Assertions.assertEquals("AR", userClaimsCaptor.getValue().getTenantId());
    ArgumentCaptor<Document> filter = ArgumentCaptor.forClass(Document.class);
    verify(update).where(filter.capture());
    Assertions.assertEquals(new Document("uuid", otpUid).append("tenantId", "AR"), filter.getValue());
  }

  @Test
  public void testVerifyOtp_InvalidOtp() {
    String otpUid = "test-uuid";
    String otp = "wrong-otp";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setTenantId("AR");
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
    ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
    when(update.where(any(Document.class))).thenReturn(Uni.createFrom().item(1L));
    when(OtpFlow.update(anyString(), (Object) any())).thenReturn(update);
    when(userService.getUserClaimsFromPdv(anyString()))
        .thenReturn(Uni.createFrom().item(UserClaims.builder().build()));
    when(sessionService.generateSessionToken(any()))
        .thenReturn(Uni.createFrom().item("session-token"));

    otpFlowService
        .verifyOtp(otpUid, otp)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(OtpForbiddenException.class, "Wrong Otp Code");
  }

  @Test
  public void testVerifyOtp_MaxAttemptsReached() {
    String otpUid = "test-uuid";
    String otp = "otp";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setTenantId("AR");
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
    ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
    when(update.where(any(Document.class))).thenReturn(Uni.createFrom().item(1L));
    when(OtpFlow.update(anyString(), (Object) any())).thenReturn(update);
    when(userService.getUserClaimsFromPdv(anyString()))
        .thenReturn(Uni.createFrom().item(UserClaims.builder().build()));
    when(sessionService.generateSessionToken(any()))
        .thenReturn(Uni.createFrom().item("session-token"));

    otpFlowService
        .verifyOtp(otpUid, otp)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(OtpForbiddenException.class, "Max attempts reached");
  }

  @Test
  public void testVerifyOtp_ExpiredOtp() {
    String otpUid = "test-uuid";
    String otp = "test-otp";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setTenantId("AR");
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
    ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
    when(update.where(any(Document.class))).thenReturn(Uni.createFrom().item(1L));
    when(OtpFlow.update(anyString(), (Object) any())).thenReturn(update);
    when(userService.getUserClaimsFromPdv(anyString()))
        .thenReturn(Uni.createFrom().item(UserClaims.builder().build()));
    when(sessionService.generateSessionToken(any()))
        .thenReturn(Uni.createFrom().item("session-token"));

    otpFlowService
        .verifyOtp(otpUid, otp)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(ConflictException.class, "Otp is expired");
  }

  @Test
  public void testResendOtp_OtpFlowNotFound() {
    String otpUid = "test-uuid";

    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.empty()));
    when(OtpFlow.find(any())).thenReturn(query);

    otpFlowService
        .resendOtp(otpUid)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(ResourceNotFoundException.class, "Cannot find OtpFlow");
  }

  @Test
  void resendOtpRejectsLegacyFlowBeforeMailOrUpdate() {
    OtpFlow legacyFlow = OtpFlow.builder().uuid("legacy-uuid").status(OtpStatus.PENDING).build();
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.find(any(Document.class))).thenReturn(query);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(legacyFlow)));

    otpFlowService.resendOtp("legacy-uuid")
        .subscribe().withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(IllegalStateException.class, "OTP flow requires tenant backfill before resend");

    Mockito.verifyNoInteractions(userService, otpNotificationService);
  }

  @Test
  public void testResendOtp_InternalError() {
    String otpUid = "test-uuid";

    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional())
        .thenReturn(Uni.createFrom().failure(new InternalException("Internal Error")));
    when(OtpFlow.find(any())).thenReturn(query);

    otpFlowService
        .resendOtp(otpUid)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(InternalException.class, "Internal Error");
  }

  @Test
  public void testResendOtp_SuccessWhenOtpIsExpired() {
    String otpUid = "test-uuid";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setTenantId("AR");
    otpFlow.setUuid(otpUid);
    otpFlow.setOtp("test-otp");
    otpFlow.setUserId("userId");
    otpFlow.setAttempts(1);
    otpFlow.setExpiresAt(OffsetDateTime.now().minusMinutes(10));
    otpFlow.setStatus(OtpStatus.PENDING);

    String newOtpUid = "test-newuuid";
    OtpFlow newOtpFlow = new OtpFlow();
    newOtpFlow.setTenantId("AR");
    newOtpFlow.setUuid(newOtpUid);
    newOtpFlow.setOtp("test-otp");
    newOtpFlow.setUserId("userId");
    newOtpFlow.setAttempts(0);
    otpFlow.setExpiresAt(OffsetDateTime.now().plusMinutes(15));
    otpFlow.setStatus(OtpStatus.PENDING);

    UserClaims userClaims = getUserClaims();

    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(otpFlow)));
    when(OtpFlow.find(any())).thenReturn(query);
    when(OtpFlow.persist(any(OtpFlow.class), any())).thenReturn(Uni.createFrom().voidItem());
    ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
    when(update.where(any(Document.class))).thenReturn(Uni.createFrom().item(1L));
    when(OtpFlow.update(anyString(), (Object) any())).thenReturn(update);

    when(userService.getUserClaimsFromPdv(anyString()))
        .thenReturn(Uni.createFrom().item(userClaims));
    when(userService.getUserInfoEmail(any())).thenReturn(Uni.createFrom().item("test@test.it"));
    when(otpNotificationService.sendOtpEmail(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Uni.createFrom().item("requestId"));

    otpFlowService
        .resendOtp(otpUid)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted();
  }

  @Test
  public void testResendOtp_ConflictWhenOtpIsInAFinalState() {
    String otpUid = "test-uuid";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setTenantId("AR");
    otpFlow.setUuid(otpUid);
    otpFlow.setOtp("test-otp");
    otpFlow.setAttempts(1);
    otpFlow.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
    otpFlow.setStatus(OtpStatus.REJECTED);

    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(otpFlow)));
    when(OtpFlow.find(any())).thenReturn(query);

    otpFlowService
        .resendOtp(otpUid)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(ConflictException.class, "Otp is expired or in a final state");
  }

  @Test
  public void testResendOtp_InternalErrorWhenPdvIsUnreacheable() {
    String otpUid = "test-uuid";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setTenantId("AR");
    otpFlow.setUuid(otpUid);
    otpFlow.setOtp("test-otp");
    otpFlow.setUserId("userId");
    otpFlow.setAttempts(1);
    otpFlow.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
    otpFlow.setStatus(OtpStatus.PENDING);

    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(otpFlow)));
    when(OtpFlow.find(any())).thenReturn(query);

    when(userService.getUserClaimsFromPdv(anyString()))
        .thenReturn(Uni.createFrom().failure(new InternalException("PDV unreachable")));

    otpFlowService
        .resendOtp(otpUid)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(InternalException.class, "Cannot get User from PDV");
  }

  @Test
  public void testResendOtp_InternalErrorWhenInternalUserApiNotReachable() {
    String otpUid = "test-uuid";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setTenantId("AR");
    otpFlow.setUuid(otpUid);
    otpFlow.setOtp("test-otp");
    otpFlow.setUserId("userId");
    otpFlow.setAttempts(1);
    otpFlow.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
    otpFlow.setStatus(OtpStatus.PENDING);

    UserClaims userClaims = getUserClaims();

    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(otpFlow)));
    when(OtpFlow.find(any())).thenReturn(query);

    when(userService.getUserClaimsFromPdv(anyString()))
        .thenReturn(Uni.createFrom().item(userClaims));
    when(userService.getUserInfoEmail(any()))
        .thenReturn(
            Uni.createFrom().failure(new InternalException("Internal User MS not reachable")));

    otpFlowService
        .resendOtp(otpUid)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(InternalException.class, "Cannot get User Info Email");
  }

  @Test
  public void testResendOtp_ConflictErrorWhenUserNotFound() {
    String otpUid = "test-uuid";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setTenantId("AR");
    otpFlow.setUuid(otpUid);
    otpFlow.setOtp("test-otp");
    otpFlow.setUserId("userId");
    otpFlow.setAttempts(1);
    otpFlow.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
    otpFlow.setStatus(OtpStatus.PENDING);

    UserClaims userClaims = getUserClaims();

    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(otpFlow)));
    when(OtpFlow.find(any())).thenReturn(query);

    when(userService.getUserClaimsFromPdv(anyString()))
        .thenReturn(Uni.createFrom().item(userClaims));
    when(userService.getUserInfoEmail(any()))
        .thenReturn(Uni.createFrom().failure(new NotFoundException("User not found")));

    otpFlowService
        .resendOtp(otpUid)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(ConflictException.class, "User not found");
  }

  @Test
  public void testResendOtp_ExceptionWhenOtpFlowCreationFails() {
    String otpUid = "test-uuid";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setTenantId("AR");
    otpFlow.setUuid(otpUid);
    otpFlow.setOtp("test-otp");
    otpFlow.setUserId("userId");
    otpFlow.setAttempts(1);
    otpFlow.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
    otpFlow.setStatus(OtpStatus.PENDING);

    UserClaims userClaims = getUserClaims();

    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(otpFlow)));
    when(OtpFlow.find(any())).thenReturn(query);
    when(OtpFlow.persist(any(OtpFlow.class), any()))
        .thenReturn(Uni.createFrom().failure(new Exception("Cannot create Otp Flow")));

    when(userService.getUserClaimsFromPdv(anyString()))
        .thenReturn(Uni.createFrom().item(userClaims));
    when(userService.getUserInfoEmail(any())).thenReturn(Uni.createFrom().item("test@test.it"));

    otpFlowService
        .resendOtp(otpUid)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(Exception.class, "Cannot create Otp Flow");
  }

  @Test
  public void testResendOtp_failsIfUpdateOldOtpFlowFails() {
    String otpUid = "test-uuid";
    OtpFlow otpFlow = new OtpFlow();
    otpFlow.setTenantId("AR");
    otpFlow.setUuid(otpUid);
    otpFlow.setOtp("test-otp");
    otpFlow.setUserId("userId");
    otpFlow.setAttempts(1);
    otpFlow.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
    otpFlow.setStatus(OtpStatus.PENDING);

    String newOtpUid = "test-newuuid";
    OtpFlow newOtpFlow = new OtpFlow();
    newOtpFlow.setTenantId("AR");
    newOtpFlow.setUuid(newOtpUid);
    newOtpFlow.setOtp("test-otp");
    newOtpFlow.setUserId("userId");
    newOtpFlow.setAttempts(0);
    otpFlow.setExpiresAt(OffsetDateTime.now().plusMinutes(15));
    otpFlow.setStatus(OtpStatus.PENDING);

    UserClaims userClaims = getUserClaims();

    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(otpFlow)));
    when(OtpFlow.find(any())).thenReturn(query);
    when(OtpFlow.persist(any(OtpFlow.class), any())).thenReturn(Uni.createFrom().voidItem());
    ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
    when(update.where(any(Document.class)))
        .thenReturn(Uni.createFrom().item(1L), Uni.createFrom().failure(new Exception("Cannot update old OTP")));
    when(OtpFlow.update(anyString(), (Object) any())).thenReturn(update);

    when(userService.getUserClaimsFromPdv(anyString()))
        .thenReturn(Uni.createFrom().item(userClaims));
    when(userService.getUserInfoEmail(any())).thenReturn(Uni.createFrom().item("test@test.it"));
    when(otpNotificationService.sendOtpEmail(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Uni.createFrom().item("requestId"));

    otpFlowService
        .resendOtp(otpUid)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(Exception.class, "Cannot update old OTP");
    ArgumentCaptor<Document> filter = ArgumentCaptor.forClass(Document.class);
    verify(update, Mockito.times(2)).where(filter.capture());
    Assertions.assertEquals("AR", filter.getAllValues().get(0).getString("tenantId"));
    Assertions.assertEquals(new Document("uuid", otpUid).append("tenantId", "AR"),
        filter.getAllValues().get(1));
  }

  @Test
  void resendOtpFailsIfMailRequestIdUpdateMatchesNoFlow() {
    mockResendUntilNotification(Uni.createFrom().item("request-id"));
    ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
    when(OtpFlow.update(anyString(), (Object) any())).thenReturn(update);
    when(update.where(any(Document.class))).thenReturn(Uni.createFrom().item(0L));

    otpFlowService.resendOtp("test-uuid")
        .subscribe().withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(ResourceNotFoundException.class, "Cannot update OtpFlow for tenant");

    ArgumentCaptor<Document> filter = ArgumentCaptor.forClass(Document.class);
    verify(update).where(filter.capture());
    Assertions.assertEquals("AR", filter.getValue().getString("tenantId"));
  }

  @Test
  void resendOtpPropagatesNotificationFailure() {
    mockResendUntilNotification(Uni.createFrom().failure(new IllegalStateException("OneMail unavailable")));

    otpFlowService.resendOtp("test-uuid")
        .subscribe().withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(IllegalStateException.class, "OneMail unavailable");
  }

  @Test
  void resendOtpRejectsMissingMailRequestId() {
    mockResendUntilNotification(Uni.createFrom().nullItem());

    otpFlowService.resendOtp("test-uuid")
        .subscribe().withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(InternalException.class, "OTP mail request ID is missing");
  }

  private void mockResendUntilNotification(Uni<String> notificationResult) {
    OtpFlow oldFlow = OtpFlow.builder().uuid("test-uuid").userId("user-id")
        .tenantId("AR").status(OtpStatus.PENDING).build();
    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.find(any(Document.class))).thenReturn(query);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(oldFlow)));
    when(OtpFlow.persist(any(OtpFlow.class), any())).thenReturn(Uni.createFrom().voidItem());
    when(userService.getUserClaimsFromPdv("user-id"))
        .thenReturn(Uni.createFrom().item(getUserClaims()));
    when(userService.getUserInfoEmail("user-id"))
        .thenReturn(Uni.createFrom().item("test@test.it"));
    when(otpNotificationService.sendOtpEmail(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(notificationResult);
  }

  @Test
  void testGetOtpMailInfo_Success() {
    String mailRequestId = "request-id";
    mockMailRequestIdForCurrentTenant();

    EmailStatusItemResponseDTOHistoryInner history1 =
      new EmailStatusItemResponseDTOHistoryInner();
    history1.setStatus(EmailStatus.DELIVERED);
    history1.setChangedAt(OffsetDateTime.now());

    EmailStatusItemResponseDTOHistoryInner history2 =
      new EmailStatusItemResponseDTOHistoryInner();
    history2.setStatus(EmailStatus.QUEUED);
    history2.setChangedAt(OffsetDateTime.now().minusMinutes(1));

    EmailAddress emailAddress = new EmailAddress();
    emailAddress.setEmail("test@test.com");

    EmailStatusItemResponseDTO response =
      EmailStatusItemResponseDTO.builder()
        .emailId(mailRequestId)
        .status(EmailStatus.DELIVERED)
        .to(emailAddress)
        .attempts(1)
        .history(List.of(history1, history2))
        .build();

    when(otpNotificationService.getOtpMailInfo(mailRequestId))
      .thenReturn(Uni.createFrom().item(response));

    OtpMailInfoResponse result =
      otpFlowService
        .getOtpMailInfo(mailRequestId)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted()
        .getItem();

    Assertions.assertEquals(mailRequestId, result.getMailRequestId());
    Assertions.assertEquals("Delivered", result.getStatus());
    Assertions.assertEquals("test@test.com", result.getRecipient());
    Assertions.assertEquals(1, result.getAttempts());

    Assertions.assertEquals(2, result.getHistory().size());
    Assertions.assertEquals(
      "Delivered",
      result.getHistory().get(0).getStatus());
    Assertions.assertEquals(
      history1.getChangedAt(),
      result.getHistory().get(0).getChangedAt());

    Assertions.assertEquals(
      "Queued",
      result.getHistory().get(1).getStatus());
  }

  @Test
  void testGetOtpMailInfo_Failure() {
    String mailRequestId = "request-id";
    mockMailRequestIdForCurrentTenant();

    when(otpNotificationService.getOtpMailInfo(mailRequestId))
      .thenReturn(Uni.createFrom().failure(new InternalException("OneMail error")));

    otpFlowService
      .getOtpMailInfo(mailRequestId)
      .subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .assertFailedWith(InternalException.class, "OneMail error");
  }

  private void mockMailRequestIdForCurrentTenant() {
    OtpFlow flow = OtpFlow.builder().tenantId("AR").build();
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.find(any(Document.class))).thenReturn(query);
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(flow)));
  }

  @Test
  void mailRequestIdFromAnotherTenantCannotReadNotification() {
    when(tenantContext.getTenantId()).thenReturn("PNPG");
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
    List<Document> filters = new ArrayList<>();
    when(OtpFlow.find(any(Document.class))).thenAnswer(invocation -> {
      filters.add(invocation.getArgument(0));
      return query;
    });
    when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.empty()));

    otpFlowService.getOtpMailInfo("shared-request-id").subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailedWith(ResourceNotFoundException.class);

    Assertions.assertEquals(new Document("mailRequestId", "shared-request-id")
        .append("tenantId", "PNPG"), filters.get(0));
    Mockito.verifyNoInteractions(otpNotificationService);
  }

  @Test
  void testGetOtpInfo_WithStatus() {
    String userId = "userId";

    List<OtpFlow> otpFlows = List.of(
      OtpFlow.builder()
        .uuid("uuid1")
        .userId(userId)
        .tenantId("AR")
        .status(OtpStatus.PENDING)
        .build(),
      OtpFlow.builder()
        .uuid("uuid2")
        .userId(userId)
        .tenantId("AR")
        .status(OtpStatus.PENDING)
        .build());

    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();

    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
      Mockito.mock(ReactivePanacheQuery.class);

    when(query.list()).thenReturn((Uni) Uni.createFrom().item(otpFlows));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);

    List<OtpFlow> result =
      otpFlowService
        .getOtpInfo(userId, OtpStatus.PENDING)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted()
        .getItem();

    Assertions.assertEquals(2, result.size());
    Assertions.assertEquals("uuid1", result.get(0).getUuid());
    Assertions.assertEquals(OtpStatus.PENDING, result.get(0).getStatus());
    Assertions.assertEquals("uuid2", result.get(1).getUuid());
    Assertions.assertEquals(OtpStatus.PENDING, result.get(1).getStatus());
  }

  @Test
  void testGetOtpInfo_WithoutStatus() {
    String userId = "userId";

    List<OtpFlow> otpFlows = List.of(
      OtpFlow.builder()
        .uuid("uuid1")
        .userId(userId)
        .tenantId("AR")
        .status(OtpStatus.COMPLETED)
        .build());

    PanacheMock.mock(OtpFlow.class);
    when(OtpFlow.builder()).thenCallRealMethod();

    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
      Mockito.mock(ReactivePanacheQuery.class);

    when(query.list()).thenReturn((Uni) Uni.createFrom().item(otpFlows));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);

    List<OtpFlow> result =
      otpFlowService
        .getOtpInfo(userId, null)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted()
        .getItem();

    Assertions.assertEquals(1, result.size());
    Assertions.assertEquals("uuid1", result.get(0).getUuid());
    Assertions.assertEquals(userId, result.get(0).getUserId());
    Assertions.assertEquals(OtpStatus.COMPLETED, result.get(0).getStatus());
  }

  @Test
  void otpInfoStatusReadIsTenantScoped() {
    when(tenantContext.getTenantId()).thenReturn("PNPG");
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);
    when(query.list()).thenReturn(Uni.createFrom().item(List.of()));
    List<Document> filters = new ArrayList<>();
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenAnswer(invocation -> {
      filters.add(invocation.getArgument(0));
      return query;
    });

    otpFlowService.getOtpInfo("shared-user", OtpStatus.PENDING).await().indefinitely();

    Assertions.assertEquals(new Document("userId", "shared-user")
        .append("status", OtpStatus.PENDING).append("tenantId", "PNPG"), filters.get(0));
  }
}

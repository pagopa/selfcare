package it.pagopa.selfcare.auth.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.model.OtpStatus;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.model.otp.OtpInfo;
import it.pagopa.selfcare.auth.profile.BetaFFTestProfile;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.WebApplicationException;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
@TestProfile(BetaFFTestProfile.class)
public class OtpFlowBetaServiceTest {

  @InjectMock UserService userService;
  @InjectMock OtpNotificationService otpNotificationService;
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
  void returnNewOtpFlow_whenNoneOtpFlowExistsForNotForcedBetaUser() {
    UserClaims input = getUserClaims();
    getUserClaims().setFiscalCode("fiscalCode2");
    when(userService.getUserInfoEmail(any(UserClaims.class)))
            .thenReturn(Uni.createFrom().item("test@test.com"));
    when(otpNotificationService.sendOtpEmail(anyString(), anyString(), anyString()))
            .thenReturn(Uni.createFrom().voidItem());
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
            Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    when(query.firstResult())
            .thenReturn(Uni.createFrom().failure(new WebApplicationException(404)));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);
    Optional<OtpInfo> maybeOtpInfo =
            otpFlowService
                    .handleOtpFlow(input)
                    .subscribe()
                    .withSubscriber(UniAssertSubscriber.create())
                    .assertCompleted()
                    .getItem();
    Assertions.assertTrue(maybeOtpInfo.isPresent());
    OtpInfo otpInfo = maybeOtpInfo.get();
    Assertions.assertEquals("test@test.com", otpInfo.getInstitutionalEmail());
  }

  @Test
  void returnNewOtpFlow_whenNoneOtpFlowExists() {
    UserClaims input = getUserClaims();
    when(userService.getUserInfoEmail(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item("test@test.com"));
    when(otpNotificationService.sendOtpEmail(anyString(), anyString(), anyString()))
        .thenReturn(Uni.createFrom().voidItem());
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    when(query.firstResult())
        .thenReturn(Uni.createFrom().failure(new WebApplicationException(404)));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);
    Optional<OtpInfo> maybeOtpInfo =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpInfo.isPresent());
    OtpInfo otpInfo = maybeOtpInfo.get();
    Assertions.assertEquals("test@test.com", otpInfo.getInstitutionalEmail());
  }

  @Test
  void returnNewOtpFlow_whenAnotherCompletedExists() {
    UserClaims input = getUserClaims();

    when(userService.getUserInfoEmail(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item("test@test.com"));
    when(otpNotificationService.sendOtpEmail(anyString(), anyString(), anyString()))
        .thenReturn(Uni.createFrom().voidItem());
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    OtpFlow foundOtpFlow =
        OtpFlow.builder().uuid("uuid").otp("123456").status(OtpStatus.COMPLETED).build();
    when(query.firstResult()).thenReturn(Uni.createFrom().item(foundOtpFlow));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);
    Optional<OtpInfo> maybeOtpInfo =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpInfo.isPresent());
    OtpInfo otpInfo = maybeOtpInfo.get();
    Assertions.assertEquals("test@test.com", otpInfo.getInstitutionalEmail());
    Assertions.assertNotEquals(foundOtpFlow.getUuid(), otpInfo.getUuid());
  }

  @Test
  void returnNewOtpFlow_whenAnotherRejectedExists() {
    UserClaims input = getUserClaims();

    when(userService.getUserInfoEmail(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item("test@test.com"));
    when(otpNotificationService.sendOtpEmail(anyString(), anyString(), anyString()))
        .thenReturn(Uni.createFrom().voidItem());
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    OtpFlow foundOtpFlow =
        OtpFlow.builder()
            .uuid("uuid")
            .otp("123456")
            .status(OtpStatus.REJECTED)
            .expiresAt(OffsetDateTime.now())
            .build();
    when(query.firstResult()).thenReturn(Uni.createFrom().item(foundOtpFlow));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);
    Optional<OtpInfo> maybeOtpInfo =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpInfo.isPresent());
    OtpInfo otpInfo = maybeOtpInfo.get();
    Assertions.assertEquals("test@test.com", otpInfo.getInstitutionalEmail());
    Assertions.assertNotEquals(foundOtpFlow.getUuid(), otpInfo.getUuid());
  }

  @Test
  void returnNewOtpFlow_whenAnotherExpiredExists() {
    UserClaims input = getUserClaims();

    when(userService.getUserInfoEmail(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item("test@test.com"));
    when(otpNotificationService.sendOtpEmail(anyString(), anyString(), anyString()))
        .thenReturn(Uni.createFrom().voidItem());
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    OtpFlow foundOtpFlow =
        OtpFlow.builder()
            .uuid("uuid")
            .otp("123456")
            .status(OtpStatus.PENDING)
            .expiresAt(OffsetDateTime.now().minusHours(1))
            .build();
    when(query.firstResult()).thenReturn(Uni.createFrom().item(foundOtpFlow));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);
    Optional<OtpInfo> maybeOtpInfo =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpInfo.isPresent());
    OtpInfo otpInfo = maybeOtpInfo.get();
    Assertions.assertEquals("test@test.com", otpInfo.getInstitutionalEmail());
    Assertions.assertNotEquals(foundOtpFlow.getUuid(), otpInfo.getUuid());
  }

  @Test
  void returnExistingOtpFlow_whenUntilValid() {
    UserClaims input = getUserClaims();

    when(userService.getUserInfoEmail(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item("test@test.com"));
    when(otpNotificationService.sendOtpEmail(anyString(), anyString(), anyString()))
        .thenReturn(Uni.createFrom().voidItem());
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    OtpFlow foundOtpFlow =
        OtpFlow.builder()
            .uuid("uuid")
            .otp("123456")
            .status(OtpStatus.PENDING)
            .expiresAt(OffsetDateTime.now().plusMinutes(10))
            .build();
    when(query.firstResult()).thenReturn(Uni.createFrom().item(foundOtpFlow));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);
    Optional<OtpInfo> maybeOtpInfo =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpInfo.isPresent());
    OtpInfo otpInfo = maybeOtpInfo.get();
    Assertions.assertEquals(foundOtpFlow.getUuid(), otpInfo.getUuid());
  }

  @Test
  void returnEmpty_whenUserNotExists() {
    UserClaims input = getUserClaims();
    when(userService.getUserInfoEmail(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().failure(new WebApplicationException(404)));
    Optional<OtpInfo> maybeOtpInfo =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpInfo.isEmpty());
  }

  @Test
  void returnEmpty_whenUserNotRequiresOtpFlowUsingSameIdp() {
    UserClaims input = getUserClaims();
    input.setFiscalCode("noOtpFiscalCode");
    input.setSameIdp(true);
    when(userService.getUserInfoEmail(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item("test@test.com"));
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    when(query.firstResult())
        .thenReturn(Uni.createFrom().failure(new WebApplicationException(404)));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);
    Optional<OtpInfo> maybeOtpInfo =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpInfo.isEmpty());
  }

  @Test
  void returnEmpty_whenAPreviousOtpFlowIsCompletedUsingSameIdp() {
    UserClaims input = getUserClaims();
    input.setFiscalCode("noOtpFiscalCode");
    input.setSameIdp(true);
    when(userService.getUserInfoEmail(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item("test@test.com"));
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    OtpFlow foundOtpFlow =
        OtpFlow.builder()
            .uuid("uuid")
            .otp("123456")
            .status(OtpStatus.COMPLETED)
            .expiresAt(OffsetDateTime.now())
            .build();
    when(query.firstResult()).thenReturn(Uni.createFrom().item(foundOtpFlow));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);
    Optional<OtpInfo> maybeOtpInfo =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpInfo.isEmpty());
  }

  @Test
  void failure_whenAnErrorOccursCallingGetUserInfo() {
    String exceptionDesc = "Cannot get User Info Email on External Internal APIs";
    UserClaims input = getUserClaims();
    when(userService.getUserInfoEmail(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().failure(new WebApplicationException(500)));
    OtpFlow.builder()
        .uuid("uuid")
        .otp("123456")
        .status(OtpStatus.COMPLETED)
        .expiresAt(OffsetDateTime.now())
        .build();

    otpFlowService
        .handleOtpFlow(input)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed()
        .assertFailedWith(InternalException.class, exceptionDesc);
  }

  @Test
  void failure_whenAnErrorOccursWhileFindingLastOtpFlow() {
    String exceptionDesc = "Cannot get Last OtpFlow";
    UserClaims input = getUserClaims();
    when(userService.getUserInfoEmail(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item("test@test.com"));
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    when(query.firstResult()).thenReturn(Uni.createFrom().failure(new Exception(exceptionDesc)));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);

    otpFlowService
        .handleOtpFlow(input)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed()
        .assertFailedWith(InternalException.class, exceptionDesc);
  }
}

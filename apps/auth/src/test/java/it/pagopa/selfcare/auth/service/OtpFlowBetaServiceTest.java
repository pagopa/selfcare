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
import org.openapi.quarkus.internal_json.model.UserResource;

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
  void returnNewOtpFlow_whenNoneOtpFlowExists() {
    UserClaims input = getUserClaims();
    when(userService.getUserInfo(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item(UserResource.builder().email("test@test.com").build()));
    when(otpNotificationService.sendOtpEmail(anyString(), anyString(), anyString()))
        .thenReturn(Uni.createFrom().voidItem());
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    when(query.firstResult())
        .thenReturn(Uni.createFrom().failure(new WebApplicationException(404)));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);
    Optional<OtpFlow> maybeOtpFlow =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpFlow.isPresent());
    OtpFlow otpFlow = maybeOtpFlow.get();
    Assertions.assertEquals(otpFlow.getUserId(), input.getUid());
    Assertions.assertEquals(OtpStatus.PENDING, otpFlow.getStatus());
    Assertions.assertEquals("test@test.com", otpFlow.getNotificationEmail());
    Assertions.assertEquals(0, otpFlow.getAttempts());
  }

  @Test
  void returnNewOtpFlow_whenAnotherCompletedExists() {
    UserClaims input = getUserClaims();

    when(userService.getUserInfo(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item(UserResource.builder().email("test@test.com").build()));
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
    Optional<OtpFlow> maybeOtpFlow =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpFlow.isPresent());
    OtpFlow otpFlow = maybeOtpFlow.get();
    Assertions.assertEquals(otpFlow.getUserId(), input.getUid());
    Assertions.assertEquals(OtpStatus.PENDING, otpFlow.getStatus());
    Assertions.assertEquals("test@test.com", otpFlow.getNotificationEmail());
    Assertions.assertEquals(0, otpFlow.getAttempts());
    Assertions.assertNotEquals(foundOtpFlow.getUuid(), otpFlow.getUuid());
  }

  @Test
  void returnNewOtpFlow_whenAnotherRejectedExists() {
    UserClaims input = getUserClaims();

    when(userService.getUserInfo(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item(UserResource.builder().email("test@test.com").build()));
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
    Optional<OtpFlow> maybeOtpFlow =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpFlow.isPresent());
    OtpFlow otpFlow = maybeOtpFlow.get();
    Assertions.assertEquals(otpFlow.getUserId(), input.getUid());
    Assertions.assertEquals(OtpStatus.PENDING, otpFlow.getStatus());
    Assertions.assertEquals("test@test.com", otpFlow.getNotificationEmail());
    Assertions.assertEquals(0, otpFlow.getAttempts());
    Assertions.assertNotEquals(foundOtpFlow.getUuid(), otpFlow.getUuid());
  }

  @Test
  void returnNewOtpFlow_whenAnotherExpiredExists() {
    UserClaims input = getUserClaims();

    when(userService.getUserInfo(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item(UserResource.builder().email("test@test.com").build()));
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
    Optional<OtpFlow> maybeOtpFlow =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpFlow.isPresent());
    OtpFlow otpFlow = maybeOtpFlow.get();
    Assertions.assertEquals(otpFlow.getUserId(), input.getUid());
    Assertions.assertEquals(OtpStatus.PENDING, otpFlow.getStatus());
    Assertions.assertEquals("test@test.com", otpFlow.getNotificationEmail());
    Assertions.assertEquals(0, otpFlow.getAttempts());
    Assertions.assertNotEquals(foundOtpFlow.getUuid(), otpFlow.getUuid());
  }

  @Test
  void returnExistingOtpFlow_whenUntilValid() {
    UserClaims input = getUserClaims();

    when(userService.getUserInfo(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item(UserResource.builder().email("test@test.com").build()));
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
    Optional<OtpFlow> maybeOtpFlow =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpFlow.isPresent());
    OtpFlow otpFlow = maybeOtpFlow.get();
    Assertions.assertEquals(OtpStatus.PENDING, otpFlow.getStatus());
    Assertions.assertEquals(foundOtpFlow.getUuid(), otpFlow.getUuid());
  }

  @Test
  void returnEmpty_whenUserNotExists() {
    UserClaims input = getUserClaims();
    when(userService.getUserInfo(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().failure(new WebApplicationException(404)));
    Optional<OtpFlow> maybeOtpFlow =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpFlow.isEmpty());
  }

  @Test
  void returnEmpty_whenUserNotRequiresOtpFlowUsingSameIdp() {
    UserClaims input = getUserClaims();
    input.setFiscalCode("noOtpFiscalCode");
    input.setSameIdp(true);
    when(userService.getUserInfo(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item(UserResource.builder().email("test@test.com").build()));
    PanacheMock.mock(OtpFlow.class);
    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query =
        Mockito.mock(ReactivePanacheQuery.class);
    when(OtpFlow.builder()).thenCallRealMethod();
    when(query.firstResult())
        .thenReturn(Uni.createFrom().failure(new WebApplicationException(404)));
    when(OtpFlow.find(any(Document.class), any(Document.class))).thenReturn(query);
    Optional<OtpFlow> maybeOtpFlow =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpFlow.isEmpty());
  }

  @Test
  void returnEmpty_whenAPreviousOtpFlowIsCompletedUsingSameIdp() {
    UserClaims input = getUserClaims();
    input.setFiscalCode("noOtpFiscalCode");
    input.setSameIdp(true);
    when(userService.getUserInfo(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().item(UserResource.builder().email("test@test.com").build()));
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
    Optional<OtpFlow> maybeOtpFlow =
        otpFlowService
            .handleOtpFlow(input)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();
    Assertions.assertTrue(maybeOtpFlow.isEmpty());
  }

  @Test
  void failure_whenAnErrorOccursCallingGetUserInfo() {
    String exceptionDesc = "Cannot get User Info on External Internal APIs";
    UserClaims input = getUserClaims();
    input.setFiscalCode("noOtpFiscalCode");
    input.setSameIdp(true);
    when(userService.getUserInfo(any(UserClaims.class)))
        .thenReturn(Uni.createFrom().failure(new WebApplicationException(500)));
    OtpFlow foundOtpFlow =
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
    input.setFiscalCode("noOtpFiscalCode");
    input.setSameIdp(true);
    when(userService.getUserInfo(any(UserClaims.class)))
            .thenReturn(Uni.createFrom().item(UserResource.builder().email("test@test.com").build()));
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

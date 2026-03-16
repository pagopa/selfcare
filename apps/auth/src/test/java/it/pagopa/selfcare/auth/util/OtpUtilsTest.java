package it.pagopa.selfcare.auth.util;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.model.OtpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.when;

@QuarkusTest
public class OtpUtilsTest {

  @Test
  void maskEmailCorrectly() {
    String email = "pippo.pluto@test.com";
    Assertions.assertEquals("p***o.p***o@test.com", OtpUtils.maskEmail(email));
  }

  @Test
  void maskEmailWithPartLenghtUnder2() {
    String email = "p.pluto@test.com";
    Assertions.assertEquals("p*.p***o@test.com", OtpUtils.maskEmail(email));
  }

  @Test
  void maskEmailWhenNull() {
    Assertions.assertNull(OtpUtils.maskEmail(null));
  }

  @Test
  void maskEmailNotContainingAtSign() {
    String email = "pippo";
    Assertions.assertEquals(email, OtpUtils.maskEmail(email));
  }

  @Test
  void generateRandomNumericOtpEveryTime() {
    String firstOtp = OtpUtils.generateOTP();
    String secondOtp = OtpUtils.generateOTP();
    Assertions.assertNotEquals(firstOtp, secondOtp);
    Assertions.assertTrue(firstOtp.matches("\\d{6}"));
    Assertions.assertTrue(secondOtp.matches("\\d{6}"));
  }

  @Test
  void completedAndSameIdp_ShouldNotRequireNewOtp() {
    OtpFlow flow = createOtpFlow(OtpStatus.COMPLETED, OffsetDateTime.now().plusMinutes(5), OffsetDateTime.now());
    Assertions.assertFalse(OtpUtils.isNewOtpFlowRequired(flow, true, 0).await().indefinitely());
  }

  @Test
  void completedAndDifferentIdp_ShouldRequireNewOtp() {
    OtpFlow flow = createOtpFlow(OtpStatus.COMPLETED, OffsetDateTime.now().plusMinutes(5), OffsetDateTime.now());
    Assertions.assertTrue(OtpUtils.isNewOtpFlowRequired(flow, false, 0).await().indefinitely());
  }

  @Test
  void expiredStatus_ShouldRequireNewOtp() {
    OtpFlow flow = createOtpFlow(OtpStatus.EXPIRED, OffsetDateTime.now().plusMinutes(5), OffsetDateTime.now());
    Assertions.assertTrue(OtpUtils.isNewOtpFlowRequired(flow, true, 0).await().indefinitely());
  }

  @Test
  void rejectedStatus_ShouldRequireNewOtp() {
    OtpFlow flow = createOtpFlow(OtpStatus.REJECTED, OffsetDateTime.now().plusMinutes(5), OffsetDateTime.now());
    Assertions.assertTrue(OtpUtils.isNewOtpFlowRequired(flow, true, 0).await().indefinitely());
  }

  @Test
  void pendingAndNotExpired_ShouldNotRequireNewOtp() {
    OtpFlow flow = createOtpFlow(OtpStatus.PENDING, OffsetDateTime.now().plusMinutes(5), OffsetDateTime.now());
    Assertions.assertFalse(OtpUtils.isNewOtpFlowRequired(flow, true, 0).await().indefinitely());
  }

  @Test
  void pendingAndExpired_ShouldRequireNewOtp() {
    OtpFlow flow = createOtpFlow(OtpStatus.PENDING, OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now());
    Assertions.assertTrue(OtpUtils.isNewOtpFlowRequired(flow, true, 0).await().indefinitely());
  }

  @Test
  void completedAndExpired_ShouldNotRequireNewOtp() {
    OtpFlow flow = createOtpFlow(OtpStatus.COMPLETED, OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now());
    Assertions.assertFalse(OtpUtils.isNewOtpFlowRequired(flow, true, 0).await().indefinitely());
  }

  @Test
  void completed3monthsAgo_ShouldNotRequireNewOtp() {
    OtpFlow flow = createOtpFlow(OtpStatus.COMPLETED, OffsetDateTime.now().plusMinutes(5), OffsetDateTime.now().minusMonths(3));
    Assertions.assertFalse(OtpUtils.isNewOtpFlowRequired(flow, true, 0).await().indefinitely());
  }

  @Test
  void completed7monthsAgo_ShouldNotRequireNewOtp_PeriodicOTPNotActive() {
    OtpFlow flow = createOtpFlow(OtpStatus.COMPLETED, OffsetDateTime.now().plusMinutes(5), OffsetDateTime.now().minusMonths(7));
    Assertions.assertFalse(OtpUtils.isNewOtpFlowRequired(flow, true, 0).await().indefinitely());
  }

  @Test
  void completed7monthsAgo_ShouldRequireNewOtp_PeriodicOTPActive() {
    OtpFlow flow = createOtpFlow(OtpStatus.COMPLETED, OffsetDateTime.now().plusMinutes(5), OffsetDateTime.now().minusMonths(7));
    Assertions.assertTrue(OtpUtils.isNewOtpFlowRequired(flow, true, -1).await().indefinitely());
  }

  @Test
  void completed7MonthsAgo_ShouldRequireNewOtp_PeriodicOTPActiveUnderLimit() {

    OtpFlow flow = createOtpFlow(
            OtpStatus.COMPLETED,
            OffsetDateTime.now().plusMinutes(5),
            OffsetDateTime.now().minusMonths(7)
    );

    PanacheMock.mock(OtpFlow.class);

    OtpFlow f1 = Mockito.mock(OtpFlow.class);
    when(f1.getUserId()).thenReturn("user1");
    OtpFlow f2 = Mockito.mock(OtpFlow.class);
    when(f2.getUserId()).thenReturn("user2");
    OtpFlow f3 = Mockito.mock(OtpFlow.class);
    when(f3.getUserId()).thenReturn("user2");

    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);

    when(OtpFlow.find(Mockito.anyString(), Mockito.<Object[]>any()))
            .thenReturn(query);

    when(query.list())
            .thenReturn(Uni.createFrom().item(List.of(f1, f2, f3)));

    Boolean result = OtpUtils.isNewOtpFlowRequired(flow, true, 3)
            .await()
            .indefinitely();

    Assertions.assertTrue(result);
  }

  @Test
  void completed7monthsAgo_ShouldNotRequireNewOtp_PeriodicOTPActiveAboveLimit() {
    OtpFlow flow = createOtpFlow(OtpStatus.COMPLETED, OffsetDateTime.now().plusMinutes(5), OffsetDateTime.now().minusMonths(7));

    PanacheMock.mock(OtpFlow.class);

    OtpFlow f1 = Mockito.mock(OtpFlow.class);
    when(f1.getUserId()).thenReturn("user1");
    OtpFlow f2 = Mockito.mock(OtpFlow.class);
    when(f2.getUserId()).thenReturn("user2");
    OtpFlow f3 = Mockito.mock(OtpFlow.class);
    when(f3.getUserId()).thenReturn("user2");

    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);

    when(OtpFlow.find(Mockito.anyString(), Mockito.<Object[]>any()))
            .thenReturn(query);

    when(query.list())
            .thenReturn(Uni.createFrom().item(List.of(f1, f2, f3)));

    Boolean result = OtpUtils.isNewOtpFlowRequired(flow, true, 2)
            .await()
            .indefinitely();

    Assertions.assertFalse(result);
  }

  @Test
  void isOtpRequiredWithMissingOtpFlow_DifferentIdp_ShouldRequireOtp() {
    Assertions.assertTrue(OtpUtils.isOtpRequiredWithMissingOtpFlow(false, 0).await().indefinitely());
  }

  @Test
  void isOtpRequiredWithMissingOtpFlow_SameIdp_PeriodicOtpNotActive_ShouldNotRequireOtp() {
    Assertions.assertFalse(OtpUtils.isOtpRequiredWithMissingOtpFlow(true, 0).await().indefinitely());
  }

  @Test
  void isOtpRequiredWithMissingOtpFlow_SameIdp_PeriodicOtpActiveUnderLimit_ShouldRequireOtp() {

    PanacheMock.mock(OtpFlow.class);

    OtpFlow f1 = Mockito.mock(OtpFlow.class);
    when(f1.getUserId()).thenReturn("user1");
    OtpFlow f2 = Mockito.mock(OtpFlow.class);
    when(f2.getUserId()).thenReturn("user2");
    OtpFlow f3 = Mockito.mock(OtpFlow.class);
    when(f3.getUserId()).thenReturn("user2");

    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);

    when(OtpFlow.find(Mockito.anyString(), Mockito.<Object[]>any()))
            .thenReturn(query);

    when(query.list())
            .thenReturn(Uni.createFrom().item(List.of(f1, f2, f3)));

    Boolean result = OtpUtils.isOtpRequiredWithMissingOtpFlow(true, 3)
            .await()
            .indefinitely();

    Assertions.assertTrue(result);
  }

  @Test
  void isOtpRequiredWithMissingOtpFlow_SameIdp_PeriodicOtpActiveAboveLimit_ShouldNotRequireOtp() {

    PanacheMock.mock(OtpFlow.class);

    OtpFlow f1 = Mockito.mock(OtpFlow.class);
    when(f1.getUserId()).thenReturn("user1");
    OtpFlow f2 = Mockito.mock(OtpFlow.class);
    when(f2.getUserId()).thenReturn("user2");
    OtpFlow f3 = Mockito.mock(OtpFlow.class);
    when(f3.getUserId()).thenReturn("user2");

    ReactivePanacheQuery<ReactivePanacheMongoEntityBase> query = Mockito.mock(ReactivePanacheQuery.class);

    when(OtpFlow.find(Mockito.anyString(), Mockito.<Object[]>any()))
            .thenReturn(query);

    when(query.list())
            .thenReturn(Uni.createFrom().item(List.of(f1, f2, f3)));

    Boolean result = OtpUtils.isOtpRequiredWithMissingOtpFlow(true, 2)
            .await()
            .indefinitely();

    Assertions.assertFalse(result);
  }


  @Test
  void isOtpRequiredWithMissingOtpFlow_SameIdp_PeriodicOtpActive_ShouldRequireOtp() {
    Assertions.assertTrue(OtpUtils.isOtpRequiredWithMissingOtpFlow(true, -1).await().indefinitely());
  }


    private OtpFlow createOtpFlow(OtpStatus status, OffsetDateTime expiresAt, OffsetDateTime createdAt) {
      return OtpFlow.builder().status(status).expiresAt(expiresAt).createdAt(createdAt).build();
  }
}

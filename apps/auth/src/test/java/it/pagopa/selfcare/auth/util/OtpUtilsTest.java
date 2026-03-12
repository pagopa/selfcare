package it.pagopa.selfcare.auth.util;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.model.OtpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

@QuarkusTest
public class OtpUtilsTest {

  @Test
  public void maskEmailCorrectly() {
    String email = "pippo.pluto@test.com";
    Assertions.assertEquals("p***o.p***o@test.com", OtpUtils.maskEmail(email));
  }

  @Test
  public void generateRandomNumericOtpEveryTime() {
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

  private OtpFlow createOtpFlow(OtpStatus status, OffsetDateTime expiresAt, OffsetDateTime createdAt) {
    return OtpFlow.builder().status(status).expiresAt(expiresAt).createdAt(createdAt).build();
  }
}

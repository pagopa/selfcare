package it.pagopa.selfcare.auth.util;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.model.OtpStatus;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
    OtpFlow flow = createOtpFlow(OtpStatus.COMPLETED, OffsetDateTime.now().plusMinutes(5));
    Assertions.assertFalse(OtpUtils.isNewOtpFlowRequired(flow, true));
  }

  @Test
  void completedAndDifferentIdp_ShouldRequireNewOtp() {
    OtpFlow flow = createOtpFlow(OtpStatus.COMPLETED, OffsetDateTime.now().plusMinutes(5));
    Assertions.assertTrue(OtpUtils.isNewOtpFlowRequired(flow, false));
  }

  @Test
  void expiredStatus_ShouldRequireNewOtp() {
    OtpFlow flow = createOtpFlow(OtpStatus.EXPIRED, OffsetDateTime.now().plusMinutes(5));
    Assertions.assertTrue(OtpUtils.isNewOtpFlowRequired(flow, true));
  }

  @Test
  void rejectedStatus_ShouldRequireNewOtp() {
    OtpFlow flow = createOtpFlow(OtpStatus.REJECTED, OffsetDateTime.now().plusMinutes(5));
    Assertions.assertTrue(OtpUtils.isNewOtpFlowRequired(flow, true));
  }

  @Test
  void pendingAndNotExpired_ShouldNotRequireNewOtp() {
    OtpFlow flow = createOtpFlow(OtpStatus.PENDING, OffsetDateTime.now().plusMinutes(5));
    Assertions.assertFalse(OtpUtils.isNewOtpFlowRequired(flow, true));
  }

  @Test
  void pendingAndExpired_ShouldRequireNewOtp() {
    OtpFlow flow = createOtpFlow(OtpStatus.PENDING, OffsetDateTime.now().minusMinutes(1));
    Assertions.assertTrue(OtpUtils.isNewOtpFlowRequired(flow, true));
  }

  @Test
  void completedAndExpired_ShouldNotRequireNewOtp() {
    OtpFlow flow = createOtpFlow(OtpStatus.COMPLETED, OffsetDateTime.now().minusMinutes(1));
    Assertions.assertFalse(OtpUtils.isNewOtpFlowRequired(flow, true));
  }

  private OtpFlow createOtpFlow(OtpStatus status, OffsetDateTime expiresAt) {
    return OtpFlow.builder().status(status).expiresAt(expiresAt).build();
  }
}

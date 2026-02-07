package it.pagopa.selfcare.auth.integration_test.steps;

import static org.junit.jupiter.api.Assertions.fail;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.model.FeatureFlagEnum;
import it.pagopa.selfcare.auth.model.OtpStatus;
import it.pagopa.selfcare.auth.model.otp.OtpBetaUser;
import it.pagopa.selfcare.auth.model.otp.OtpFeatureFlag;
import it.pagopa.selfcare.auth.service.JwtService;
import it.pagopa.selfcare.cucumber.utils.SharedStepData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;

@Slf4j
@ApplicationScoped
public class AuthSteps {
  @Inject SharedStepData sharedStepData;

  @Inject JwtService jwtService;

  @Inject OtpFeatureFlag otpFeatureFlag;

  @Before
  public void setUp() {
    resetTestState();
  }

  @After
  public void tearDown() {
    resetTestState();
  }

  @After("@RemoveOtpFlow")
  public void removeOtpFlowAfterScenario(Scenario scenario) {
    String otpSessionUid =
        sharedStepData.getResponse().body().jsonPath().getString("otpSessionUid");
    final String uiidField = OtpFlow.Fields.uuid.name();

    Long l = OtpFlow.delete(new Document(uiidField, otpSessionUid)).await().indefinitely();

    if (l == 0) {
      log.info("No OTP flow found for session UID: {}", otpSessionUid);
    } else {
      log.info("Deleted OTP flow with session UID: {}", otpSessionUid);
    }
  }

  @And("OTP feature flag is set to {string}")
  public void otpFeatureFlagIsSetTo(String featureFlag) {
    FeatureFlagEnum flag = FeatureFlagEnum.valueOf(featureFlag.toUpperCase());
    otpFeatureFlag.setFeatureFlag(flag);
  }

  @And("User in the beta user list with the following details:")
  public void userIsInTheBetaUserList(Map<String, String> userDetails) {
    String fiscalCode = userDetails.get("fiscalCode");
    boolean forceOtp = Boolean.parseBoolean(userDetails.getOrDefault("forceOtp", "false"));
    String forcedEmail = userDetails.get("forcedEmail");

    OtpBetaUser betaUser =
        OtpBetaUser.builder()
            .fiscalCode(fiscalCode)
            .forceOtp(forceOtp)
            .forcedEmail(forcedEmail)
            .build();
    otpFeatureFlag.setOtpBetaUsers(List.of(betaUser));
  }

  @And("The session token claims contains:")
  public void checkResponseBody(Map<String, String> expectedKeyValues) {
    String sessionToken = sharedStepData.getResponse().body().jsonPath().getString("sessionToken");

    Map<String, String> claims =
        jwtService.extractClaimsFromJwtToken(sessionToken).await().indefinitely();

    expectedKeyValues.forEach(
        (expectedKey, expectedValue) -> {
          String actualValue = claims.get(expectedKey);
          Assertions.assertNotNull(actualValue, "Claim '" + expectedKey + "' not found in token");
          Assertions.assertEquals(
              expectedValue,
              actualValue,
              "Claim '" + expectedKey + "' does not match expected value");
        });
  }

  @And("An OTP flow should be created with status {string}")
  public void anOtpFlowShouldBeCreatedWithStatus(String status) {
    String otpSessionUid =
        sharedStepData.getResponse().body().jsonPath().getString("otpSessionUid");
    final String uiidField = OtpFlow.Fields.uuid.name();

    OtpFlow otpFlow =
        OtpFlow.<OtpFlow>find(new Document(uiidField, otpSessionUid))
            .firstResult()
            .await()
            .indefinitely();

    Assertions.assertNotNull(otpFlow, "Failed to find OtpFlow with UUID:" + otpSessionUid);
    Assertions.assertEquals(
        OtpStatus.valueOf(status), otpFlow.getStatus(), "OtpFlow status is not : " + status);
  }

  @And("An OTP flow with uuid {string} already exists with status {string} and attempts {int}")
  public void anOTPFlowWithUuidAlreadyExistsWithStatus(String uuid, String status, int attempts) {
    String updateBuilder =
        "{'$set': { 'status': ?1, 'attempts' : ?2, 'updatedAt': ?3, 'expiresAt' : ?4 } }";
    OtpFlow.update(
            updateBuilder,
            status,
            attempts,
            Date.from(OffsetDateTime.now().toInstant()),
            Date.from(OffsetDateTime.now().plusMinutes(5).toInstant()))
        .where(OtpFlow.Fields.uuid.name(), uuid)
        .await()
        .indefinitely();
  }

  @And("The OTP flow with uuid {string} has been updated to status {string}")
  public void theOTPFlowStatusHasBeenUpdatedTo(String uuid, String status) {
    OtpFlow otpFlow =
        OtpFlow.<OtpFlow>find(new Document(OtpFlow.Fields.uuid.name(), uuid))
            .firstResult()
            .await()
            .indefinitely();

    if (otpFlow == null) {
      fail("Failed to find OtpFlow with UUID: " + uuid);
    }

    Assertions.assertEquals(
        OtpStatus.valueOf(status), otpFlow.getStatus(), "OtpFlow status is not: " + status);
  }

  private void resetTestState() {
    otpFeatureFlag.setFeatureFlag(FeatureFlagEnum.NONE);
    otpFeatureFlag.setOtpBetaUsers(List.of());
  }
}

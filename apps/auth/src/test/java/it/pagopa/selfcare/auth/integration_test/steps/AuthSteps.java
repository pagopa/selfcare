package it.pagopa.selfcare.auth.integration_test.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.model.FeatureFlagEnum;
import it.pagopa.selfcare.auth.model.OtpStatus;
import it.pagopa.selfcare.auth.model.otp.OtpBetaUser;
import it.pagopa.selfcare.auth.model.otp.OtpDailyLimit;
import it.pagopa.selfcare.auth.model.otp.OtpFeatureFlag;
import it.pagopa.selfcare.auth.service.JwtService;
import it.pagopa.selfcare.cucumber.utils.SharedStepData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static it.pagopa.selfcare.auth.model.OtpStatus.COMPLETED;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@ApplicationScoped
public class AuthSteps {
  @Inject SharedStepData sharedStepData;

  @Inject JwtService jwtService;

  @Inject OtpFeatureFlag otpFeatureFlag;

  @Inject OtpDailyLimit otpDailyLimit;

  @Before(order = 0)
  public void setUp() {
    resetTestState();
  }

  @Before(value = "@OidcBelowLimit", order = 10)
  public void setHighLimit() {
    otpDailyLimit.setDailyLimit(20);
    writeOptFlowToDatabase();
  }

  @Before(value = "@OidcAboveLimit", order = 10)
  public void setLowLimit() {
    otpDailyLimit.setDailyLimit(2);
    writeOptFlowToDatabase();
  }

  @Before(value = "@OidcOpenLimit", order = 10)
  public void setLimitOpen() {
    otpDailyLimit.setDailyLimit(-1);
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
    boolean sameIdp = Boolean.parseBoolean(userDetails.getOrDefault("sameIdp", "false"));

    OtpBetaUser betaUser =
        OtpBetaUser.builder()
            .fiscalCode(fiscalCode)
            .forceOtp(forceOtp)
            .forcedEmail(forcedEmail)
            .sameIdp(sameIdp)
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


  @And("An OTP flow with uuid {string} was COMPLETED {int} months ago")
  public void anOTPFlowWithUuidWasCompletedMonthsAgo(String uuid, int months) {
    String updateBuilder =
            "{'$set': { 'status': ?1, 'attempts' : ?2, 'createdAt': ?3, 'updatedAt': ?4, 'expiresAt' : ?5 } }";
    OtpFlow.update(
                    updateBuilder,
                    COMPLETED,
                    0,
                    Date.from(OffsetDateTime.now().minusMonths(months).toInstant()),
                    Date.from(OffsetDateTime.now().minusMonths(months).toInstant()),
                    Date.from(OffsetDateTime.now().minusMonths(months).plusMinutes(5).toInstant()))
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
    otpDailyLimit.setDailyLimit(0);
    deleteOtpFlowFromDatabase();
  }

  private void writeOptFlowToDatabase() {
    OffsetDateTime now = OffsetDateTime.now();

    OtpFlow otpFlow =
            OtpFlow.builder()
                    .userId("35a78332-d038-4bfa-8e85-2cba7f6b7323")
                    .status(OtpStatus.PENDING)
                    .attempts(0)
                    .createdAt(now)
                    .updatedAt(now)
                    .expiresAt(now)
                    .build();

    OtpFlow otpFlow2 =
            OtpFlow.builder()
                    .userId("35a78332-d038-4bfa-8e85-2cba7f6b7322")
                    .status(OtpStatus.PENDING)
                    .attempts(0)
                    .createdAt(now)
                    .updatedAt(now)
                    .expiresAt(now)
                    .build();

    OtpFlow otpFlowSameUser =
            OtpFlow.builder()
                    .userId("35a78332-d038-4bfa-8e85-2cba7f6b7322")
                    .status(OtpStatus.PENDING)
                    .attempts(0)
                    .createdAt(now)
                    .updatedAt(now)
                    .expiresAt(now)
                    .build();

    otpFlow.persist().await().indefinitely();
    otpFlow2.persist().await().indefinitely();
    otpFlowSameUser.persist().await().indefinitely();
  }

  private void deleteOtpFlowFromDatabase() {
    OtpFlow.delete("userId", "35a78332-d038-4bfa-8e85-2cba7f6b7323")
            .await()
            .indefinitely();

    OtpFlow.delete("userId", "35a78332-d038-4bfa-8e85-2cba7f6b7322")
            .await()
            .indefinitely();
  }


}

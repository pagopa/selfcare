package it.pagopa.selfcare.auth.profile;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class BetaFFTestProfile implements QuarkusTestProfile {
  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of(
        "otp.ff.enabled", "BETA",
        "otp.ff.beta-users",
            "[{\"fiscalCode\": \"fiscalCode\", \"forceOtp\": true}, " +
                    "{\"fiscalCode\": \"fiscalCode2\", \"forceOtp\": false}, " +
                    "{\"fiscalCode\": \"fiscalCode3\", \"forceOtp\": true, \"forcedEmail\": \"forced.mail@test.com\"}]");
  }
}

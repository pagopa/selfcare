package it.pagopa.selfcare.auth.conf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.auth.model.FeatureFlagEnum;
import it.pagopa.selfcare.auth.model.otp.OtpBetaUser;
import it.pagopa.selfcare.auth.model.otp.OtpFeatureFlag;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
public class FeatureFlagConfig {

  /**
   * {@code @Startup} must sit on the producer method, not on the declaring class: annotating the
   * class only forces {@code FeatureFlagConfig} itself to be instantiated, which never invokes the
   * producer. Without it the flag is resolved lazily on the first OTP flow, so a misconfiguration
   * surfaces at runtime instead of at deploy time.
   */
  @Produces
  @ApplicationScoped
  @Startup
  public OtpFeatureFlag otpFeatureFlag(
      @ConfigProperty(name = "otp.ff.enabled") String otpFf,
      @ConfigProperty(name = "otp.ff.beta-users") String otpBetaUsers) {

    FeatureFlagEnum featureFlag = parseFeatureFlag(otpFf);
    List<OtpBetaUser> betaUsers = parseBetaUsers(otpBetaUsers);

    // Beta users carry fiscal codes and e-mail addresses, so only the size is logged.
    log.info(
        "Initialized OtpFeatureFlag with otp.ff.enabled={} and {} beta user(s)",
        featureFlag,
        betaUsers.size());

    return OtpFeatureFlag.builder().featureFlag(featureFlag).otpBetaUsers(betaUsers).build();
  }

  private FeatureFlagEnum parseFeatureFlag(String otpFf) {
    String normalized = otpFf == null ? "" : otpFf.trim().toUpperCase(Locale.ROOT);
    try {
      return FeatureFlagEnum.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          String.format(
              "Invalid otp.ff.enabled value '%s'. Allowed values are %s.",
              otpFf, Arrays.toString(FeatureFlagEnum.values())),
          e);
    }
  }

  /**
   * A malformed value must not be swallowed: silently falling back to an empty list would keep the
   * BETA flag on while disabling OTP for every user, weakening the second factor with no visible
   * failure.
   */
  private List<OtpBetaUser> parseBetaUsers(String otpBetaUsers) {
    if (otpBetaUsers == null || otpBetaUsers.isBlank()) {
      return List.of();
    }
    try {
      List<OtpBetaUser> parsed =
          new ObjectMapper().readValue(otpBetaUsers, new TypeReference<List<OtpBetaUser>>() {});
      return parsed == null ? List.of() : parsed;
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "Invalid otp.ff.beta-users value: expected a JSON array of beta users. "
              + "Check the 'feature-flag-otp-beta-users' Key Vault secret.",
          e);
    }
  }
}

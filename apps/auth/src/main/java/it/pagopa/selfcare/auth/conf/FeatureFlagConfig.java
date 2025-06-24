package it.pagopa.selfcare.auth.conf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.auth.model.FeatureFlagEnum;
import it.pagopa.selfcare.auth.model.otp.OtpBetaUser;
import it.pagopa.selfcare.auth.model.otp.OtpFeatureFlag;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@ApplicationScoped
public class FeatureFlagConfig {

  @ApplicationScoped
  public OtpFeatureFlag otpFeatureFlag(
      @ConfigProperty(name = "otp.ff.enabled") String otpFf,
      @ConfigProperty(name = "otp.ff.beta-users") String otpBetaUsers) {

    log.info("Initializing OtpFeatureFlag with otp.ff.enabled={}, and otp.ff.beta-users={}", otpFf, otpBetaUsers);
    ObjectMapper objectMapper = new ObjectMapper();
    TypeReference<List<OtpBetaUser>> jacksonTypeReference = new TypeReference<>() {};
    List<OtpBetaUser> betaUsers = new ArrayList<>();
    try {
      betaUsers = objectMapper.readValue(otpBetaUsers, jacksonTypeReference);
    } catch (JsonProcessingException e) {
      log.error(e.toString());
    }
    OtpFeatureFlag otpFeatureFlag =  OtpFeatureFlag.builder()
        .featureFlag(FeatureFlagEnum.valueOf(otpFf))
        .otpBetaUsers(betaUsers)
        .build();

    log.info("Using OtpFeatureFlag:{}", otpFeatureFlag);
    return otpFeatureFlag;
  }
}

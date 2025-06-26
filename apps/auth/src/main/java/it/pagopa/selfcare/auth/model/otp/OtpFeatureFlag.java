package it.pagopa.selfcare.auth.model.otp;

import it.pagopa.selfcare.auth.model.FeatureFlagEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class OtpFeatureFlag {
  private FeatureFlagEnum featureFlag;
  private List<OtpBetaUser> otpBetaUsers;

  public Boolean isOtpForced(String fiscalCode) {
    return otpBetaUsers.stream()
        .filter(betaUser -> betaUser.getFiscalCode().equals(fiscalCode))
        .map(OtpBetaUser::getForceOtp)
        .findFirst()
        .orElse(Boolean.FALSE);
  }

  public Boolean isBetaUser(String fiscalCode) {
    return otpBetaUsers.stream().anyMatch(betaUser -> betaUser.getFiscalCode().equals(fiscalCode));
  }
}

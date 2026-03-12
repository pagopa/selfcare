package it.pagopa.selfcare.auth.model.otp;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@EqualsAndHashCode
public class OtpBetaUser {
  private String fiscalCode;
  private Boolean forceOtp = Boolean.FALSE;
  private String forcedEmail;
}

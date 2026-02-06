package it.pagopa.selfcare.auth.model.otp;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpInfo {
  private String uuid;
  private String institutionalEmail;
}

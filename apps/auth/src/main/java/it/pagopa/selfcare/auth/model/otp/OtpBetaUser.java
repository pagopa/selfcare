package it.pagopa.selfcare.auth.model.otp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class OtpBetaUser {
    private String fiscalCode;
    private Boolean forceOtp = Boolean.FALSE;
}

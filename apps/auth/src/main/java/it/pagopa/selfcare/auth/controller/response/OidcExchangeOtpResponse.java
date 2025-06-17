package it.pagopa.selfcare.auth.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OidcExchangeOtpResponse extends OidcExchangeResponse {

    private Boolean requiresOtpFlow = Boolean.TRUE;
    private String otpSessionUid;
    private String maskedEmail;

}

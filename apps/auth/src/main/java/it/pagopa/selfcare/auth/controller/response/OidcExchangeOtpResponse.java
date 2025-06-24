package it.pagopa.selfcare.auth.controller.response;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class OidcExchangeOtpResponse extends OidcExchangeResponse {

    private Boolean requiresOtpFlow = Boolean.TRUE;
    private String otpSessionUid;
    private String maskedEmail;

    public OidcExchangeOtpResponse(String otpSessionUid, String maskedEmail) {
        super();
        this.otpSessionUid = otpSessionUid;
        this.maskedEmail = maskedEmail;
    }
}

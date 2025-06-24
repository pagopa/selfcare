package it.pagopa.selfcare.auth.controller.response;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class OidcExchangeTokenResponse extends TokenResponse {
    private Boolean requiresOtpFlow = Boolean.FALSE;

    public OidcExchangeTokenResponse(String sessionToken){
        this.setSessionToken(sessionToken);
    }
}

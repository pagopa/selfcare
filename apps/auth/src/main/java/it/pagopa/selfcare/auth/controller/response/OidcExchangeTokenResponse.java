package it.pagopa.selfcare.auth.controller.response;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OidcExchangeTokenResponse extends TokenResponse {
    private Boolean requiresOtpFlow = Boolean.FALSE;

    public OidcExchangeTokenResponse(String sessionToken){
        super();
        this.setSessionToken(sessionToken);
    }
}

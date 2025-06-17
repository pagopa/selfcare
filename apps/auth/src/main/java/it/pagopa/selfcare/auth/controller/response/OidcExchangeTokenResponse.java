package it.pagopa.selfcare.auth.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OidcExchangeTokenResponse extends OidcExchangeResponse {

    private Boolean requiresOtpFlow = Boolean.FALSE;
    private String sessionToken;

}

package it.pagopa.selfcare.auth.controller.response;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OidcExchangeOtpResponse extends OidcExchangeResponse {

    private Boolean requiresOtpFlow = Boolean.TRUE;
    private String otpSessionUid;
    private String maskedEmail;

}

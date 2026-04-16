package it.pagopa.selfcare.auth.controller.response;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse extends OidcExchangeResponse {
  private String sessionToken;
}

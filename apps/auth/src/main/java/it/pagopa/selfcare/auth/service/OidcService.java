package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.controller.response.OidcExchangeResponse;

public interface OidcService {
  Uni<OidcExchangeResponse> exchange(String authCode, String redirectUri);
}

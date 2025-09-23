package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;

public interface SAMLService {
  Uni<String> generateSessionToken(String samlResponse) throws Exception;
  String getLoginSuccessUrl(String token);
}

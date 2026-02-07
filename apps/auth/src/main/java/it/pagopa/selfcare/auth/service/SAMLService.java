package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.model.UserClaims;

public interface SAMLService {
  Uni<String> generateSessionToken(String samlResponse) throws Exception;

  String getLoginSuccessUrl(String token);

  Uni<UserClaims> saveUser(String email);
}

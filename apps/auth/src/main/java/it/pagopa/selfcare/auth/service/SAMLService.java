package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.model.UserClaims;

public interface SAMLService {
  Uni<Boolean> validate(String samlResponse) throws Exception;
}

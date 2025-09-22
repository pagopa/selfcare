package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.model.UserClaims;

public interface SessionService {
    Uni<String> generateSessionToken(UserClaims userClaims);
    Uni<String> generateSessionTokenInternal(UserClaims userClaims);
}

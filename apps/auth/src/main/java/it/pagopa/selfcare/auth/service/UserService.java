package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.model.UserClaims;

public interface UserService {
  Uni<UserClaims> patchUser(String fiscalNumber, String name, String familyName, Boolean sameIdp);

  Uni<UserClaims> getUserClaimsFromPdv(String userId);

  Uni<String> getUserInfoEmail(UserClaims userClaims);
}

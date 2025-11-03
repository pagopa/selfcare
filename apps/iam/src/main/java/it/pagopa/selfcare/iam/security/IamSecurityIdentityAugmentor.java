package it.pagopa.selfcare.product.security;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.security.JWTSecurityIdentityAugmentor;

public class IamSecurityIdentityAugmentor extends JWTSecurityIdentityAugmentor {
  @Override
  public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
    return super.augment(identity,context);
  }
}

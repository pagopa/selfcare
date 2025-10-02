package it.pagopa.selfcare.commons.web.security;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;


@ApplicationScoped
public class JWTSecurityIdentityAugmentor implements SecurityIdentityAugmentor {


  @Override
  public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
    if (!(identity.getPrincipal() instanceof JsonWebToken jwt)) {
      return Uni.createFrom().item(identity);
    }

    String issuer = jwt.getIssuer();
    if (issuer == null || issuer.isEmpty()) {
      return Uni.createFrom().item(identity);
    }

    QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
    builder.setPrincipal(identity.getPrincipal());
    identity.getRoles().forEach(builder::addRole);
    identity.getAttributes().forEach(builder::addAttribute);
    builder.addAttribute("jwt.issuer", issuer);
    builder.addRoles(determineRolesForIssuer(issuer));
    identity.getCredentials().forEach(builder::addCredential);
    return Uni.createFrom().item(builder.build());
  }

  private static Set<String> determineRolesForIssuer(String issuer) {
    Set<String> roles = Set.of();

    if (issuer.equals("PAGOPA")) {
      roles = Set.of("SUPPORT");
    }

    return roles;
  }
}

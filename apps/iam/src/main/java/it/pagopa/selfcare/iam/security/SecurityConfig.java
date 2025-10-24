package it.pagopa.selfcare.iam.security;

import it.pagopa.selfcare.commons.web.security.JWTCallerPrincipalFactory;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces; 
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SecurityConfig {

  @Produces
  @Singleton
  @Alternative
  @Priority(1)
  public JWTCallerPrincipalFactory createCallerPrincipalFactory(
      @ConfigProperty(name = "mp.jwt.verify.publickey") String pubKey) throws Exception {
    return new JWTCallerPrincipalFactory(pubKey);
  }
}
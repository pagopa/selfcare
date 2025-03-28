package it.pagopa.selfcare.auth.service;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

  @ConfigProperty(name = "jwt.session.duration")
  Integer sessionDuration;

  @Override
  public Uni<String> generateSessionToken(String fiscalNumber, String name, String familyName) {
    JwtClaimsBuilder claims = Jwt.claims();

    claims.claim("fiscal_number", fiscalNumber.replace("TINIT-", ""));
    claims.claim("name", name);
    claims.claim("family_name", familyName);
    claims.issuedAt(Instant.now());
    claims.expiresAt(Instant.now().plus(Duration.ofHours(sessionDuration)));

    return Uni.createFrom().item(claims.sign());
  }
}

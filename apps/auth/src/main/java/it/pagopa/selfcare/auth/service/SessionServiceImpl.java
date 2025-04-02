package it.pagopa.selfcare.auth.service;

import io.smallrye.jwt.build.Jwt;
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

  private static final String SPID_FISCAL_NUMBER_PREFIX = "TINIT-";

  @ConfigProperty(name = "jwt.session.duration")
  Integer sessionDuration;

  @Override
  public Uni<String> generateSessionToken(String fiscalNumber, String name, String familyName) {
    String sessionToken =
        Jwt.claims()
            .claim("fiscal_number", fiscalNumber.replace(SPID_FISCAL_NUMBER_PREFIX, ""))
            .claim("name", name)
            .claim("family_name", familyName)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plus(Duration.ofHours(sessionDuration)))
            .sign();
    return Uni.createFrom().item(sessionToken);
  }
}

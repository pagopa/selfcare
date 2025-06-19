package it.pagopa.selfcare.auth.service;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.util.Pkcs8Utils;
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

  private static final String SPID_LEVEL_L2 = "https://www.spid.gov.it/SpidL2";
  private static final String ISSUER = "SPID";

  @ConfigProperty(name = "jwt.session.duration")
  Integer sessionDuration;

  @ConfigProperty(name = "jwt.session.audience")
  String audience;

  @ConfigProperty(name = "jwt.session.private.key")
  String privateKeyPem;

  @Override
  public Uni<String> generateSessionToken(UserClaims userClaims) {
    return Pkcs8Utils.extractRSAPrivateKeyFromPem(privateKeyPem)
        .onFailure()
        .transform(ex -> new InternalException(ex.getMessage()))
        .map(
            rsaPrivateKey ->
                Jwt.claims()
                    .claim("fiscal_number", userClaims.getFiscalCode())
                    .claim("name", userClaims.getName())
                    .claim("family_name", userClaims.getFamilyName())
                    .claim("uid", userClaims.getUid())
                    .claim("spid_level", SPID_LEVEL_L2)
                    .issuer(ISSUER)
                    .audience(audience)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plus(Duration.ofHours(sessionDuration)))
                    .sign(rsaPrivateKey));
  }
}

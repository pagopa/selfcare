package it.pagopa.selfcare.auth.service;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.conf.TenantSessionKeyProvider;
import it.pagopa.selfcare.auth.conf.TenantSessionKeyProvider.SigningKey;
import it.pagopa.selfcare.auth.model.UserClaims;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
  private static final String TENANT_CLAIM = "tenant_id";

  @ConfigProperty(name = "jwt.session.duration")
  Integer sessionDuration;

  @ConfigProperty(name = "jwt.session.audience")
  String audience;

  @Inject TenantSessionKeyProvider tenantSessionKeyProvider;

  @Override
  public Uni<String> generateSessionToken(UserClaims userClaims) {
    SigningKey signingKey = tenantSessionKeyProvider.getSigningKey(userClaims.getTenantId());
    Instant issuedAt = Instant.now();
    return Uni.createFrom()
        .item(
            Jwt.claims()
                .claim("fiscal_number", userClaims.getFiscalCode())
                .claim("name", userClaims.getName())
                .claim("family_name", userClaims.getFamilyName())
                .claim("uid", userClaims.getUid())
                .claim(TENANT_CLAIM, userClaims.getTenantId())
                .claim("spid_level", SPID_LEVEL_L2)
                .issuer(ISSUER)
                .audience(audience)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofHours(sessionDuration)))
                .jws()
                .keyId(signingKey.keyId())
                .sign(signingKey.privateKey()));
  }

  @Override
  public Uni<String> generateSessionTokenInternal(UserClaims userClaims) {
    SigningKey signingKey = tenantSessionKeyProvider.getSigningKey(userClaims.getTenantId());
    Instant issuedAt = Instant.now();
    return Uni.createFrom()
        .item(
            Jwt.claims()
                .claim("uid", userClaims.getUid())
                .claim("email", userClaims.getEmail())
                .claim(TENANT_CLAIM, userClaims.getTenantId())
                .issuer("PAGOPA")
                .audience(audience)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofHours(sessionDuration)))
                .jws()
                .keyId(signingKey.keyId())
                .sign(signingKey.privateKey()));
  }
}

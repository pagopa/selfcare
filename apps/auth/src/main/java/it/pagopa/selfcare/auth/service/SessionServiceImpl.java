package it.pagopa.selfcare.auth.service;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.conf.TenantDefinition;
import it.pagopa.selfcare.auth.conf.TenantRegistry;
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
  // RFC 9068 client_id: lets backends tell which channel issued the session token.
  static final String CLIENT_ID_CLAIM = "client_id";
  static final String SPID_CLIENT_ID = "selfcare-fe";
  static final String SAML_CLIENT_ID = "selfcare-saml";

  @ConfigProperty(name = "jwt.session.duration")
  Integer sessionDuration;

  @ConfigProperty(name = "jwt.session.audience")
  String audience;

  @ConfigProperty(name = "jwt.session.tenant-audience-enabled", defaultValue = "false")
  boolean tenantAudienceEnabled;

  @Inject TenantSessionKeyProvider tenantSessionKeyProvider;
  @Inject TenantRegistry tenantRegistry;

  /**
   * Returns the tenant-specific audience from the tenant registry (jwt.session.audience) when the
   * feature is enabled, falling back to the global jwt.session.audience.
   */
  String resolveAudience(String tenantId) {
    if (!tenantAudienceEnabled) {
      return audience;
    }
    return resolveAudience(tenantRegistry.resolveEnabledTenant(tenantId).definition());
  }

  String resolveAudience(TenantDefinition tenant) {
    if (!tenantAudienceEnabled) {
      return audience;
    }
    TenantDefinition.JwtDefinition jwt = tenant.jwt();
    String tenantAudience = jwt == null || jwt.session() == null ? null : jwt.session().audience();
    return tenantAudience == null || tenantAudience.isBlank() ? audience : tenantAudience;
  }

  @Override
  public Uni<String> generateSessionToken(UserClaims userClaims) {
    tenantRegistry.resolveEnabledTenant(userClaims.getTenantId());
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
                .claim(CLIENT_ID_CLAIM, SPID_CLIENT_ID)
                .issuer(ISSUER)
                .audience(resolveAudience(userClaims.getTenantId()))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofHours(sessionDuration)))
                .jws()
                .keyId(signingKey.keyId())
                .sign(signingKey.privateKey()));
  }

  @Override
  public Uni<String> generateSessionTokenInternal(UserClaims userClaims) {
    tenantRegistry.resolveEnabledTenant(userClaims.getTenantId());
    SigningKey signingKey = tenantSessionKeyProvider.getSigningKey(userClaims.getTenantId());
    Instant issuedAt = Instant.now();
    return Uni.createFrom()
        .item(
            Jwt.claims()
                .claim("uid", userClaims.getUid())
                .claim("email", userClaims.getEmail())
                .claim(TENANT_CLAIM, userClaims.getTenantId())
                .claim(CLIENT_ID_CLAIM, SAML_CLIENT_ID)
                .issuer("PAGOPA")
                .audience(resolveAudience(userClaims.getTenantId()))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofHours(sessionDuration)))
                .jws()
                .keyId(signingKey.keyId())
                .sign(signingKey.privateKey()));
  }
}

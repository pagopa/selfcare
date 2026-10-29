package it.pagopa.selfcare.contractscrm.client.auth;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtSignatureBuilder;
import io.smallrye.jwt.util.KeyUtils;
import it.pagopa.selfcare.contractscrm.config.ContractsCrmConfig;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Builds the machine JWT used to call onboarding-ms. The token is accepted by
 * selfcare-sdk-security: SPID/PAGOPA issuer, {@code uid} and {@code tenant_id} claims.
 */
@ApplicationScoped
public class MachineTokenProvider {

  static final String TENANT_CLAIM = "tenant_id";
  private static final Duration RENEW_BEFORE_EXPIRY = Duration.ofMinutes(1);

  private final ContractsCrmConfig config;
  private PrivateKey privateKey;
  private String cachedToken;
  private Instant cachedTokenExpiry = Instant.MIN;

  public MachineTokenProvider(ContractsCrmConfig config) {
    this.config = config;
  }

  public synchronized String getToken() {
    if (cachedToken == null || Instant.now().isAfter(cachedTokenExpiry.minus(RENEW_BEFORE_EXPIRY))) {
      ContractsCrmConfig.MachineToken tokenConfig = config.machineToken();
      Instant expiry = Instant.now().plus(tokenConfig.duration());
      JwtSignatureBuilder builder =
          Jwt.issuer(tokenConfig.issuer())
              .subject(tokenConfig.uid())
              .claim("uid", tokenConfig.uid())
              .claim(TENANT_CLAIM, config.tenantId())
              .claim("jti", UUID.randomUUID().toString())
              .expiresAt(expiry)
              .jws();
      tokenConfig.kid().ifPresent(builder::keyId);
      cachedToken = builder.sign(getPrivateKey());
      cachedTokenExpiry = expiry;
    }
    return cachedToken;
  }

  private PrivateKey getPrivateKey() {
    if (privateKey == null) {
      String pem =
          config
              .machineToken()
              .privateKey()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "contracts-crm.machine-token.private-key is not configured"));
      try {
        privateKey = KeyUtils.decodePrivateKey(pem);
      } catch (GeneralSecurityException e) {
        throw new IllegalStateException("Invalid machine token private key", e);
      }
    }
    return privateKey;
  }
}

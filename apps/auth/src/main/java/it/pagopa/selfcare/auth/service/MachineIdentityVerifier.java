package it.pagopa.selfcare.auth.service;

import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.auth.conf.MachineTokenConfig;
import it.pagopa.selfcare.auth.exception.MachineAuthenticationException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import org.jose4j.http.Get;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;

@ApplicationScoped
@Startup
public class MachineIdentityVerifier {
  @Inject MachineTokenConfig config;

  private JwtConsumer consumer;
  private String tenantId;

  @PostConstruct
  void initialize() {
    if (!config.enabled()) {
      return;
    }
    tenantId =
        UUID.fromString(
                config
                    .entraTenantId()
                    .orElseThrow(
                        () -> new IllegalStateException("M2M Entra tenant id is required")))
            .toString();
    String audience =
        config
            .entraAudience()
            .filter(value -> !value.isBlank())
            .orElseThrow(() -> new IllegalStateException("M2M Entra audience is required"));
    HttpsJwks keys =
        new HttpsJwks("https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys");
    Get http = new Get();
    http.setConnectTimeout(2000);
    http.setReadTimeout(3000);
    keys.setSimpleHttpGet(http);
    consumer = consumer(tenantId, audience, new HttpsJwksVerificationKeyResolver(keys));
  }

  static JwtConsumer consumer(String tenantId, String audience, VerificationKeyResolver keys) {
    return new JwtConsumerBuilder()
        .setRequireExpirationTime()
        .setRequireIssuedAt()
        .setExpectedIssuer("https://login.microsoftonline.com/" + tenantId + "/v2.0")
        .setExpectedAudience(audience)
        .setAllowedClockSkewInSeconds(30)
        .setJwsAlgorithmConstraints(
            new AlgorithmConstraints(
                AlgorithmConstraints.ConstraintType.PERMIT, AlgorithmIdentifiers.RSA_USING_SHA256))
        .setVerificationKeyResolver(keys)
        .build();
  }

  public String verify(String authorization) {
    if (consumer == null
        || authorization == null
        || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
      throw new MachineAuthenticationException();
    }
    try {
      return objectId(consumer.processToClaims(authorization.substring(7).trim()), tenantId);
    } catch (InvalidJwtException | MalformedClaimException | IllegalArgumentException exception) {
      // Do not log the JOSE exception: it can contain the credential itself.
      throw new MachineAuthenticationException();
    }
  }

  static String objectId(JwtClaims claims, String tenantId) throws MalformedClaimException {
    if (!tenantId.equals(claims.getStringClaimValue("tid"))
        || !"app".equals(claims.getStringClaimValue("idtyp"))
        || !"2.0".equals(claims.getStringClaimValue("ver"))) {
      throw new MachineAuthenticationException();
    }
    String oid = claims.getStringClaimValue("oid");
    if (oid == null) {
      throw new MachineAuthenticationException();
    }
    return UUID.fromString(oid).toString();
  }
}

package it.pagopa.selfcare.auth.conf;

import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.util.Pkcs8Utils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class TenantSessionKeyProvider {

  @Inject TenantRegistry tenantRegistry;
  @Inject Config config;

  private Map<String, SigningKey> signingKeys;

  @PostConstruct
  void initialize() {
    signingKeys =
        tenantRegistry.enabledAuthenticationTenants().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    TenantRegistry.Tenant::id, tenant -> loadSigningKey(tenant.id())));
  }

  public SigningKey getSigningKey(String tenantId) {
    SigningKey signingKey = signingKeys.get(tenantId);
    if (signingKey == null) {
      throw new InternalException("JWT signing key is not configured for tenant");
    }
    return signingKey;
  }

  private SigningKey loadSigningKey(String tenantId) {
    String propertyPrefix = "tenant." + tenantId.toLowerCase() + ".jwt.session.";
    String privateKeyPem = requiredConfig(propertyPrefix + "private-key", tenantId);
    String keyId = requiredConfig(propertyPrefix + "key-id", tenantId);

    try {
      return new SigningKey(Pkcs8Utils.parseRSAPrivateKeyFromPem(privateKeyPem), keyId);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new IllegalStateException("Invalid JWT signing key for tenant " + tenantId, e);
    }
  }

  private String requiredConfig(String propertyName, String tenantId) {
    return config
        .getOptionalValue(propertyName, String.class)
        .filter(value -> !value.isBlank())
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Missing JWT signing configuration for tenant " + tenantId));
  }

  public record SigningKey(PrivateKey privateKey, String keyId) {}
}

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
                    TenantRegistry.Tenant::id, this::loadSigningKey));
  }

  public SigningKey getSigningKey(String tenantId) {
    SigningKey signingKey = signingKeys.get(tenantId);
    if (signingKey == null) {
      throw new InternalException("JWT signing key is not configured for tenant");
    }
    return signingKey;
  }

  private SigningKey loadSigningKey(TenantRegistry.Tenant tenant) {
    String tenantId = tenant.id();
    TenantDefinition.JwtDefinition jwt = tenant.definition().jwt();
    TenantDefinition.SessionDefinition session = jwt == null ? null : jwt.session();
    String privateKeyPem =
        requiredConfig(session == null ? null : session.privateKeyEnvVar(), tenantId);
    String keyId = requiredConfig(session == null ? null : session.keyIdEnvVar(), tenantId);

    try {
      return new SigningKey(Pkcs8Utils.parseRSAPrivateKeyFromPem(privateKeyPem), keyId);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new IllegalStateException("Invalid JWT signing key for tenant " + tenantId, e);
    }
  }

  private String requiredConfig(String envVarName, String tenantId) {
    if (envVarName == null || envVarName.isBlank()) {
      throw missingConfiguration(tenantId);
    }
    return config
        .getOptionalValue(envVarName, String.class)
        .filter(value -> !value.isBlank())
        .orElseThrow(() -> missingConfiguration(tenantId));
  }

  private IllegalStateException missingConfiguration(String tenantId) {
    return new IllegalStateException("Missing JWT signing configuration for tenant " + tenantId);
  }

  public record SigningKey(PrivateKey privateKey, String keyId) {}
}

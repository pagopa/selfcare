package it.pagopa.selfcare.iam.util;

import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.eclipse.microprofile.config.Config;

/**
 * Encrypts and decrypts personal data with the key of the current tenant. Every supported tenant
 * must define {@code SELFCARE_DATA_ENCRIPTION_KEY_<TENANT>} and {@code
 * SELFCARE_DATA_ENCRIPTION_IV_<TENANT>}; startup fails otherwise.
 */
@Singleton
@Startup
public class DataEncryptionConfig {

  static final String KEY_PREFIX = "SELFCARE_DATA_ENCRIPTION_KEY_";
  static final String IV_PREFIX = "SELFCARE_DATA_ENCRIPTION_IV_";

  private final TenantContext tenantContext;
  private final Map<String, TenantKey> tenantKeys;

  @Inject
  public DataEncryptionConfig(
      TenantRegistry tenantRegistry, TenantContext tenantContext, Config config) {
    this.tenantContext = tenantContext;
    Map<String, TenantKey> keys = new HashMap<>();
    tenantRegistry
        .supportedTenantIds()
        .forEach(
            tenantId ->
                keys.put(
                    tenantId,
                    new TenantKey(
                        requiredValue(config, KEY_PREFIX + tenantId),
                        requiredValue(config, IV_PREFIX + tenantId))));
    this.tenantKeys = Map.copyOf(keys);
  }

  public String encrypt(String value) {
    return apply(value, v -> DataEncryptionUtils.encrypt(v, key().key(), key().iv()));
  }

  public String decrypt(String value) {
    return apply(value, v -> DataEncryptionUtils.decrypt(v, key().key(), key().iv()));
  }

  private String apply(String value, UnaryOperator<String> operation) {
    return Optional.ofNullable(value).filter(v -> !v.isEmpty()).map(operation).orElse("");
  }

  private TenantKey key() {
    String tenantId = tenantContext.requiredTenantId();
    TenantKey key = tenantKeys.get(tenantId);
    if (key == null) {
      throw new IllegalStateException("Missing encryption key for tenant " + tenantId);
    }
    return key;
  }

  private static String requiredValue(Config config, String name) {
    return config
        .getOptionalValue(name, String.class)
        .filter(value -> !value.isBlank())
        .orElseThrow(() -> new IllegalStateException("Missing configuration " + name));
  }

  private record TenantKey(String key, String iv) {
    @Override
    public String toString() {
      return "TenantKey[redacted]";
    }
  }
}

package it.pagopa.selfcare.iam.util;

import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.Config;

/**
 * Encrypts and decrypts personal data with per-tenant keys. Every supported tenant must define
 * {@code SELFCARE_DATA_ENCRIPTION_KEY_<TENANT>} and {@code SELFCARE_DATA_ENCRIPTION_IV_<TENANT>};
 * startup fails otherwise.
 *
 * <p>The user identity is shared by every tenant and is encrypted with the key of the tenant that
 * created it. Lookups therefore encrypt the email with every key ({@link #encryptForEveryTenant}),
 * and {@link #decrypt} tries the key of the current tenant first, then the others (AES-GCM rejects
 * a wrong key).
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

  /** Encrypts with the key of the current tenant. */
  public String encrypt(String value) {
    return encrypt(value, tenantContext.requiredTenantId());
  }

  public String encrypt(String value, String tenantId) {
    TenantKey key = key(tenantId);
    return apply(value, v -> DataEncryptionUtils.encrypt(v, key.key(), key.iv()));
  }

  /**
   * Encrypts {@code value} with the key of every tenant.
   *
   * @return ciphertext by tenant, the current tenant first
   */
  public Map<String, String> encryptForEveryTenant(String value) {
    Map<String, String> ciphertexts = new LinkedHashMap<>();
    tenantsCurrentFirst().forEach(tenantId -> ciphertexts.put(tenantId, encrypt(value, tenantId)));
    return ciphertexts;
  }

  /** Decrypts with the key of the current tenant, falling back to the keys of the other tenants. */
  public String decrypt(String value) {
    return apply(
        value,
        v ->
            tenantsCurrentFirst()
                .map(tenantId -> tryDecrypt(v, key(tenantId)))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No tenant key decrypts the value")));
  }

  private Stream<String> tenantsCurrentFirst() {
    String current = tenantContext.requiredTenantId();
    return Stream.concat(
        Stream.of(current),
        tenantKeys.keySet().stream().filter(tenantId -> !tenantId.equals(current)).sorted());
  }

  private static Optional<String> tryDecrypt(String value, TenantKey key) {
    try {
      return Optional.of(DataEncryptionUtils.decrypt(value, key.key(), key.iv()));
    } catch (RuntimeException e) {
      return Optional.empty();
    }
  }

  private String apply(String value, UnaryOperator<String> operation) {
    return Optional.ofNullable(value).filter(v -> !v.isEmpty()).map(operation).orElse("");
  }

  private TenantKey key(String tenantId) {
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

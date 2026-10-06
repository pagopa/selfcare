package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;

import java.util.Map;
import java.util.Optional;

/**
 * Per-tenant binding of the contract/documents blob storage (the {@code contracts} logical storage
 * of {@code document-ms}), keyed by canonical tenant id.
 */
@ConfigMapping(prefix = "onboarding-functions.contract-storage")
public interface ContractStorageConfig {

  Map<String, TenantBinding> tenants();

  interface TenantBinding {

    Optional<String> accountName();

    Optional<String> container();

    /** Trusted prefix prepended to every blob path; never accepted from callers. */
    Optional<String> pathPrefix();

    Optional<String> managedIdentityClientId();

    /** Local/Azurite only: when set, it takes precedence over Managed Identity. */
    Optional<String> connectionString();
  }
}

package it.pagopa.selfcare.security.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TenantDataIsolationRegistryTest {

  private static final String REGISTRY_JSON =
      """
      {
        "AR": {
          "cosmos_account_name": "selc-d-cosmosdb-mongodb-account",
          "cosmos_resource_group_name": "selc-d-cosmosdb-mongodb-rg",
          "cosmos_connection_string_secret_name": "mongodb-connection-string",
          "storage_account_infix": "ar",
          "storage_container_suffix": "",
          "personal_data_vault_tenant": null,
          "email_sender_domain": null
        },
        "PNPG": {
          "cosmos_account_name": "selc-d-weu-pnpg-cosmosdb-mongodb-account",
          "cosmos_resource_group_name": "selc-d-weu-pnpg-cosmosdb-mongodb-rg",
          "cosmos_connection_string_secret_name": "mongodb-connection-string-pnpg",
          "storage_account_infix": "pnpg",
          "storage_container_suffix": "-pnpg",
          "personal_data_vault_tenant": null,
          "email_sender_domain": null
        }
      }
      """;

  private final TenantDataIsolationRegistry registry =
      TenantDataIsolationRegistry.fromJson(REGISTRY_JSON);

  @Test
  void shouldResolveEachTenantToItsOwnResources() {
    assertEquals("selc-d-cosmosdb-mongodb-account", registry.cosmosAccountName(TenantId.AR));
    assertEquals(
        "selc-d-weu-pnpg-cosmosdb-mongodb-account", registry.cosmosAccountName(TenantId.PNPG));
    assertEquals("selc-d-cosmosdb-mongodb-rg", registry.cosmosResourceGroupName(TenantId.AR));
    assertEquals("ar", registry.storageAccountInfix(TenantId.AR));
    assertEquals("pnpg", registry.storageAccountInfix(TenantId.PNPG));
  }

  @Test
  void shouldExposeKeyVaultSecretNamesNotSecretValues() {
    assertEquals(
        "mongodb-connection-string", registry.cosmosConnectionStringSecretName(TenantId.AR));
    assertEquals(
        "mongodb-connection-string-pnpg", registry.cosmosConnectionStringSecretName(TenantId.PNPG));
  }

  @Test
  void shouldDeriveContainerNameFromValidatedTenantOnly() {
    assertEquals("sc-d-documents-blob", registry.storageContainer(TenantId.AR, "sc-d-documents-blob"));
    assertEquals(
        "sc-d-documents-blob-pnpg", registry.storageContainer(TenantId.PNPG, "sc-d-documents-blob"));
  }

  @Test
  void shouldRejectContainerDerivationWithoutABaseName() {
    assertThrows(
        UnresolvedTenantMappingException.class, () -> registry.storageContainer(TenantId.AR, " "));
  }

  @Test
  void shouldRejectDimensionsLeftUndecided() {
    assertThrows(
        UnresolvedTenantMappingException.class, () -> registry.personalDataVaultTenant(TenantId.AR));
    assertThrows(
        UnresolvedTenantMappingException.class, () -> registry.emailSenderDomain(TenantId.PNPG));
  }

  @Test
  void shouldRejectTenantMissingFromTheRegistry() {
    TenantDataIsolationRegistry partial =
        TenantDataIsolationRegistry.fromJson(
            "{\"AR\": {\"cosmos_account_name\": \"selc-d-cosmosdb-mongodb-account\"}}");

    assertThrows(
        UnresolvedTenantMappingException.class, () -> partial.cosmosAccountName(TenantId.PNPG));
  }

  @Test
  void shouldRejectLookupWithoutATenant() {
    assertThrows(UnresolvedTenantMappingException.class, () -> registry.cosmosAccountName(null));
  }

  @Test
  void shouldStartEmptyWhenRegistryIsNotConfiguredAndRejectEveryLookup() {
    TenantDataIsolationRegistry unconfigured = TenantDataIsolationRegistry.fromJson("  ");

    assertThrows(
        UnresolvedTenantMappingException.class, () -> unconfigured.cosmosAccountName(TenantId.AR));
  }

  @Test
  void shouldFailFastOnAnUnknownTenantOrMalformedPayload() {
    assertThrows(
        IllegalStateException.class,
        () -> TenantDataIsolationRegistry.fromJson("{\"UNKNOWN\": {\"storage_account_infix\": \"x\"}}"));
    assertThrows(IllegalStateException.class, () -> TenantDataIsolationRegistry.fromJson("[]"));
  }

  @Test
  void shouldIgnoreDimensionsAddedToTheRegistryButNotYetKnownToThisService() {
    TenantDataIsolationRegistry newer =
        TenantDataIsolationRegistry.fromJson(
            "{\"AR\": {\"storage_account_infix\": \"ar\", \"future_dimension\": \"value\"}}");

    assertEquals("ar", newer.storageAccountInfix(TenantId.AR));
  }
}

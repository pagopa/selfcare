package it.pagopa.selfcare.security.tenant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The data-layer resources belonging to one tenant (Step_1 SELC-8..SELC-11).
 *
 * <p>Mirrors one entry of {@code local.tenant_data_isolation} in
 * {@code infra/resources/_modules/local-env/locals.tf}, which is the single source of truth
 * (Step_1/EPIC.md sub-task 9); this record is only its in-process representation and must never
 * grow values that are not declared there.
 *
 * <p>A {@code null} field means the mapping is not decided for that tenant (the personal data
 * vault, SELC-10.3, and the email sender domain, SELC-11.3, are still open), not that a default
 * applies: reading it through {@link TenantDataIsolationRegistry} throws. An empty string, by
 * contrast, is a deliberate value — {@code storageContainerSuffix} is empty for the tenant whose
 * containers keep their current unsuffixed names.
 *
 * <p>Unknown JSON properties are ignored so that a new dimension can be added to the Terraform
 * registry and rolled out to services one at a time, instead of breaking every service that has not
 * been redeployed yet.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TenantDataMapping(
    @JsonProperty("cosmos_account_name") String cosmosAccountName,
    @JsonProperty("cosmos_resource_group_name") String cosmosResourceGroupName,
    @JsonProperty("cosmos_connection_string_secret_name") String cosmosConnectionStringSecretName,
    @JsonProperty("storage_account_infix") String storageAccountInfix,
    @JsonProperty("storage_container_suffix") String storageContainerSuffix,
    @JsonProperty("personal_data_vault_tenant") String personalDataVaultTenant,
    @JsonProperty("email_sender_domain") String emailSenderDomain) {
}

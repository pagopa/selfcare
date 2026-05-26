###############################################################################
# IAM: registry-proxy-runner managed identity → Blob Storage
# Grants the CAE managed identity Storage Blob Data Contributor on the
# web storage account so registry-proxy-runner can write daily CSV snapshots.
###############################################################################
data "azurerm_user_assigned_identity" "cae_managed_identity" {
  name                = "selc-${local.env_short}-cae-002-managed_identity"
  resource_group_name = "selc-${local.env_short}-container-app-002-rg"
}

data "azurerm_storage_account" "checkout_fe_storage" {
  name                = "selc${local.env_short}weuarcheckoutst01"
  resource_group_name = "selc-${local.env_short}-checkout-fe-rg"
}

resource "azurerm_role_assignment" "registry_proxy_runner_blob_contributor" {
  scope                = data.azurerm_storage_account.checkout_fe_storage.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = data.azurerm_user_assigned_identity.cae_managed_identity.principal_id
}

module "tfstate_access" {
  source = "../../_modules/tfstate_access"

  storage_account_name        = local.storage_state.storage_account_name
  storage_resource_group_name = local.storage_state.resource_group_name
  storage_container_name      = local.storage_state.container_name
  prefix                      = local.prefix
  env_short                   = local.env_short
  storage_role_name           = local.storage_role.name
}

module "keyvault_permissions" {
  source = "../../_modules/keyvault_permissions"

  prefix       = local.prefix
  env_short    = local.env_short
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

module "keyvault_pnpg_permissions" {
  source = "../../_modules/keyvault_permissions"

  prefix       = local.prefix
  env_short    = local.env_short
  key_vault_id = data.azurerm_key_vault.key_vault_pnpg.id
}

resource "azurerm_role_assignment" "queue_contributor_developers" {
  scope                = data.azurerm_resource_group.webhook_storage_rg.id
  role_definition_name = "Storage Queue Data Contributor"
  principal_id         = data.azuread_group.adgroup_developers.object_id
}

resource "azurerm_role_assignment" "queue_contributor_admin" {
  scope                = data.azurerm_resource_group.webhook_storage_rg.id
  role_definition_name = "Storage Queue Data Contributor"
  principal_id         = data.azuread_group.adgroup_admin.object_id
}

resource "azurerm_role_assignment" "synthetic_monitoring_developers" {
  scope                = data.azurerm_resource_group.synthetic_monitoring_storage_rg.id
  role_definition_name = "Storage Table Data Contributor"
  principal_id         = data.azuread_group.adgroup_developers.object_id
}

resource "azurerm_role_assignment" "synthetic_monitoring_admin" {
  scope                = data.azurerm_resource_group.synthetic_monitoring_storage_rg.id
  role_definition_name = "Storage Table Data Contributor"
  principal_id         = data.azuread_group.adgroup_admin.object_id
}
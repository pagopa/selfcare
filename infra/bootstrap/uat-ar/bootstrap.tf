module "tfstate_access" {
  source = "../_modules/tfstate_access"

  storage_account_name        = local.storage_state.storage_account_name
  storage_resource_group_name = local.storage_state.resource_group_name
  storage_container_name      = local.storage_state.container_name
  prefix                      = local.prefix
  env_short                   = local.env_short
  storage_role_name           = local.storage_role.name
}

module "keyvault" {
  source       = "../_modules/keyvault"
  prefix       = local.prefix
  env_short    = local.env_short
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

module "keyvault_pnpg" {
  source       = "../_modules/keyvault"
  prefix       = local.prefix
  env_short    = local.env_short
  key_vault_id = data.azurerm_key_vault.key_vault_pnpg.id
}

# resource "azurerm_role_assignment" "storage_blob_contributor_developers" {
#   scope                = module.logs_storage.storage_account_id
#   role_definition_name = "Storage Blob Data Reader"
#   principal_id         = data.azuread_group.adgroup_developers.object_id
# }

# resource "azurerm_role_assignment" "storage_blob_contributor_admin" {
#   scope                = module.logs_storage.storage_account_id
#   role_definition_name = "Storage Blob Data Reader"
#   principal_id         = data.azuread_group.adgroup_admin.object_id
# }

# resource "azurerm_role_assignment" "storage_blob_contributor_externals" {
#   scope                = module.logs_storage.storage_account_id
#   role_definition_name = "Storage Blob Data Reader"
#   principal_id         = data.azuread_group.adgroup_externals.object_id
# }
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
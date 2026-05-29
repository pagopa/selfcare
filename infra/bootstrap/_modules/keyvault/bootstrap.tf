
module "permissions" {
  source = "../keyvault_permissions"

  prefix       = var.prefix
  env_short    = var.env_short
  key_vault_id = var.key_vault_id
}

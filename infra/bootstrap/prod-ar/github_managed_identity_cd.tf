module "identity_cd" {
  source = "../_modules/github_managed_identity_cd"

  prefix    = local.prefix
  env_short = local.env_short
  domain    = local.domain
  app       = local.app
  env       = local.env
  tags      = local.tags

  key_vault_id      = data.azurerm_key_vault.key_vault.id
  key_vault_pnpg_id = data.azurerm_key_vault.key_vault_pnpg.id
  tenant_id         = data.azurerm_client_config.current.tenant_id
  subscription_id   = data.azurerm_subscription.current.id

  cd_github_federations    = local.cd_github_federations
  cd_github_federations_ms = local.cd_github_federations_ms
  cd_github_federations_fe = local.cd_github_federations_fe

  environment_cd_roles    = local.environment_cd_roles
  environment_cd_roles_ms = local.environment_cd_roles_ms

  depends_on = [azurerm_resource_group.identity_rg]
}

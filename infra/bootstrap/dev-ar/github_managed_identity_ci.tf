module "identity_ci" {
  source = "../_modules/github_managed_identity_ci"

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

  ci_github_federations    = local.ci_github_federations
  ci_github_federations_ms = local.ci_github_federations_ms
  ci_github_federations_fe = local.ci_github_federations_fe

  environment_ci_roles    = local.environment_ci_roles
  environment_ci_roles_ms = local.environment_ci_roles_ms

  depends_on = [
    azurerm_resource_group.identity_rg,
    module.identity_cd
  ]
}

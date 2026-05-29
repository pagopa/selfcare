module "github_secrets" {
  source = "../_modules/repository_secrets"

  github_federations_fe = local.github_federations_fe
  github_federations = {
    "selfcare" = "${local.env}"
  }

  fe_cd_identity_client_id = module.identity_cd.identity_fe_client_id
  fe_ci_identity_client_id = module.identity_ci.identity_fe_client_id
  cd_identity_client_id    = module.identity_cd.identity_client_id
  ci_identity_client_id    = module.identity_ci.identity_client_id

  tenant_id       = data.azurerm_client_config.current.tenant_id
  subscription_id = data.azurerm_client_config.current.subscription_id
  gh_pat_variable = data.azurerm_key_vault_secret.github_path_token.value
  opex            = true
}

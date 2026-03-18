# module "github_secrets" {
#   source = "../_modules/repository_secrets"

#   github_federations_fe = local.github_federations_fe
#   github_federations_ms = local.github_federations_ms

#   fe_cd_identity_client_id = module.identity_cd_fe.identity_client_id
#   fe_ci_identity_client_id = module.identity_ci_fe.identity_client_id
#   ms_cd_identity_client_id = module.identity_cd_ms.identity_client_id
#   ms_ci_identity_client_id = module.identity_ci_ms.identity_client_id

#   tenant_id       = data.azurerm_client_config.current.tenant_id
#   subscription_id = data.azurerm_client_config.current.subscription_id
#   gh_pat_variable = data.azurerm_key_vault_secret.github_path_token.value
# }

#############################################################################
# FRONTEND REPOSITORIES SECRETS
#############################################################################

resource "github_actions_environment_secret" "repo_fe_cd_secrets_client_id" {
  for_each        = local.github_federations_fe
  repository      = each.key
  environment     = "${each.value}-cd"
  secret_name     = "ARM_CLIENT_ID"
  plaintext_value = module.identity_cd_fe.identity_client_id
}

resource "github_actions_environment_secret" "repo_fe_cd_secrets_subscription_id" {
  for_each        = local.github_federations_fe
  repository      = each.key
  environment     = "${each.value}-cd"
  secret_name     = "ARM_SUBSCRIPTION_ID"
  plaintext_value = data.azurerm_client_config.current.subscription_id
}

resource "github_actions_environment_secret" "repo_fe_cd_secrets_tenant_id" {
  for_each        = local.github_federations_fe
  repository      = each.key
  environment     = "${each.value}-cd"
  secret_name     = "ARM_TENANT_ID"
  plaintext_value = data.azurerm_client_config.current.tenant_id
}

resource "github_actions_environment_secret" "repo_fe_ci_secrets_client_id" {
  for_each        = local.github_federations_fe
  repository      = each.key
  environment     = "${each.value}-ci"
  secret_name     = "ARM_CLIENT_ID"
  plaintext_value = module.identity_ci_fe.identity_client_id
}

resource "github_actions_environment_secret" "repo_fe_ci_secrets_subscription_id" {
  for_each        = local.github_federations_fe
  repository      = each.key
  environment     = "${each.value}-ci"
  secret_name     = "ARM_SUBSCRIPTION_ID"
  plaintext_value = data.azurerm_client_config.current.subscription_id
}

resource "github_actions_environment_secret" "repo_fe_ci_secrets_tenant_id" {
  for_each        = local.github_federations_fe
  repository      = each.key
  environment     = "${each.value}-ci"
  secret_name     = "ARM_TENANT_ID"
  plaintext_value = data.azurerm_client_config.current.tenant_id
}

#############################################################################
# BACKEND REPOSITORIES SECRETS
#############################################################################

resource "github_actions_environment_secret" "repo_ms_cd_secrets_client_id" {
  for_each        = local.github_federations_ms
  repository      = each.key
  environment     = "${each.value}-cd"
  secret_name     = "ARM_CLIENT_ID"
  plaintext_value = module.identity_cd_ms.identity_client_id
}

resource "github_actions_environment_secret" "repo_ms_cd_secrets_subscription_id" {
  for_each        = local.github_federations_ms
  repository      = each.key
  environment     = "${each.value}-cd"
  secret_name     = "ARM_SUBSCRIPTION_ID"
  plaintext_value = data.azurerm_client_config.current.subscription_id
}

resource "github_actions_environment_secret" "repo_ms_cd_secrets_tenant_id" {
  for_each        = local.github_federations_ms
  repository      = each.key
  environment     = "${each.value}-cd"
  secret_name     = "ARM_TENANT_ID"
  plaintext_value = data.azurerm_client_config.current.tenant_id
}

resource "github_actions_environment_secret" "repo_ms_ci_secrets_client_id" {
  for_each        = local.github_federations_ms
  repository      = each.key
  environment     = "${each.value}-ci"
  secret_name     = "ARM_CLIENT_ID"
  plaintext_value = module.identity_ci_ms.identity_client_id
}

resource "github_actions_environment_secret" "repo_ms_ci_secrets_subscription_id" {
  for_each        = local.github_federations_ms
  repository      = each.key
  environment     = "${each.value}-ci"
  secret_name     = "ARM_SUBSCRIPTION_ID"
  plaintext_value = data.azurerm_client_config.current.subscription_id
}

resource "github_actions_environment_secret" "repo_ms_ci_secrets_tenant_id" {
  for_each        = local.github_federations_ms
  repository      = each.key
  environment     = "${each.value}-ci"
  secret_name     = "ARM_TENANT_ID"
  plaintext_value = data.azurerm_client_config.current.tenant_id
}


#############################################################################
# GITHUB PAT VARIABLES REPOSITORIES SECRETS
#############################################################################

resource "github_actions_secret" "repo_fe_cd_secrets_client_id" {
  for_each        = local.github_federations_fe
  repository      = each.key
  secret_name     = "GH_PAT_VARIABLES"
  plaintext_value = data.azurerm_key_vault_secret.github_path_token.value
}

resource "github_actions_secret" "repo_ms_ci_secrets_client_id" {
  for_each        = local.github_federations_ms
  repository      = each.key
  secret_name     = "GH_PAT_VARIABLES"
  plaintext_value = data.azurerm_key_vault_secret.github_path_token.value
}

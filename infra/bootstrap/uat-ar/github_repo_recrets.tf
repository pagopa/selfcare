#############################################################################
# FRONTEND REPOSITORIES SECRETS
#############################################################################

resource "github_actions_environment_secret" "repo_fe_cd_secrets_client_id" {
  for_each        = toset(local.cd_github_federations_fe)
  repository      = each.value.repository
  environment     = each.value.subject
  secret_name     = "ARM_CLIENT_ID_CD"
  plaintext_value = module.identity_cd.identity_client_id
}

resource "github_actions_environment_secret" "repo_fe_cd_secrets_subscription_id" {
  for_each        = toset(local.cd_github_federations_fe)
  repository      = each.value.repository
  environment     = each.value.subject
  secret_name     = "ARM_SUBSCRIPTION_ID"
  plaintext_value = data.azurerm_client_config.current.subscription_id
}

resource "github_actions_environment_secret" "repo_fe_cd_secrets_tenant_id" {
  for_each        = toset(local.cd_github_federations_fe)
  repository      = each.value.repository
  environment     = each.value.subject
  secret_name     = "ARM_TENANT_ID"
  plaintext_value = data.azurerm_client_config.current.tenant_id
}

resource "github_actions_environment_secret" "repo_fe_ci_secrets_client_id" {
  for_each        = toset(local.ci_github_federations_fe)
  repository      = each.value.repository
  environment     = each.value.subject
  secret_name     = "ARM_CLIENT_ID_CI"
  plaintext_value = module.identity_cd.identity_client_id
}

resource "github_actions_environment_secret" "repo_fe_ci_secrets_subscription_id" {
  for_each        = toset(local.ci_github_federations_fe)
  repository      = each.value.repository
  environment     = each.value.subject
  secret_name     = "ARM_SUBSCRIPTION_ID"
  plaintext_value = data.azurerm_client_config.current.subscription_id
}

resource "github_actions_environment_secret" "repo_fe_ci_secrets_tenant_id" {
  for_each        = toset(local.ci_github_federations_fe)
  repository      = each.value.repository
  environment     = each.value.subject
  secret_name     = "ARM_TENANT_ID"
  plaintext_value = data.azurerm_client_config.current.tenant_id
}

#############################################################################
# BACKEND REPOSITORIES SECRETS
#############################################################################

resource "github_actions_environment_secret" "repo_be_cd_secrets_client_id" {
  for_each        = toset(local.cd_github_federations_ms)
  repository      = each.value.repository
  environment     = each.value.subject
  secret_name     = "ARM_CLIENT_ID_CD"
  plaintext_value = module.identity_cd.identity_client_id
}

resource "github_actions_environment_secret" "repo_be_cd_secrets_subscription_id" {
  for_each        = toset(local.cd_github_federations_ms)
  repository      = each.value.repository
  environment     = each.value.subject
  secret_name     = "ARM_SUBSCRIPTION_ID"
  plaintext_value = data.azurerm_client_config.current.subscription_id
}

resource "github_actions_environment_secret" "repo_be_cd_secrets_tenant_id" {
  for_each        = toset(local.cd_github_federations_ms)
  repository      = each.value.repository
  environment     = each.value.subject
  secret_name     = "ARM_TENANT_ID"
  plaintext_value = data.azurerm_client_config.current.tenant_id
}

resource "github_actions_environment_secret" "repo_be_ci_secrets_client_id" {
  for_each        = toset(local.ci_github_federations_ms)
  repository      = each.value.repository
  environment     = each.value.subject
  secret_name     = "ARM_CLIENT_ID_CI"
  plaintext_value = module.identity_cd.identity_client_id
}

resource "github_actions_environment_secret" "repo_be_ci_secrets_subscription_id" {
  for_each        = toset(local.ci_github_federations_ms)
  repository      = each.value.repository
  environment     = each.value.subject
  secret_name     = "ARM_SUBSCRIPTION_ID"
  plaintext_value = data.azurerm_client_config.current.subscription_id
}

resource "github_actions_environment_secret" "repo_be_ci_secrets_tenant_id" {
  for_each        = toset(local.ci_github_federations_ms)
  repository      = each.value.repository
  environment     = each.value.subject
  secret_name     = "ARM_TENANT_ID"
  plaintext_value = data.azurerm_client_config.current.tenant_id
}
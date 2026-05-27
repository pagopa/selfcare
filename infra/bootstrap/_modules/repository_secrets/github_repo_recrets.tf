#############################################################################
# FRONTEND REPOSITORIES SECRETS
#############################################################################

resource "github_actions_environment_secret" "repo_fe_cd_secrets_client_id" {
  for_each    = var.github_federations_fe
  repository  = each.key
  environment = "${each.value}-cd"
  secret_name = "ARM_CLIENT_ID"
  value       = var.fe_cd_identity_client_id
}

resource "github_actions_environment_secret" "repo_fe_cd_secrets_subscription_id" {
  for_each    = var.github_federations_fe
  repository  = each.key
  environment = "${each.value}-cd"
  secret_name = "ARM_SUBSCRIPTION_ID"
  value       = var.subscription_id
}

resource "github_actions_environment_secret" "repo_fe_cd_secrets_tenant_id" {
  for_each    = var.github_federations_fe
  repository  = each.key
  environment = "${each.value}-cd"
  secret_name = "ARM_TENANT_ID"
  value       = var.tenant_id
}

resource "github_actions_environment_secret" "repo_fe_ci_secrets_client_id" {
  for_each    = var.github_federations_fe
  repository  = each.key
  environment = "${each.value}-ci"
  secret_name = "ARM_CLIENT_ID"
  value       = var.fe_ci_identity_client_id
}

resource "github_actions_environment_secret" "repo_fe_ci_secrets_subscription_id" {
  for_each    = var.github_federations_fe
  repository  = each.key
  environment = "${each.value}-ci"
  secret_name = "ARM_SUBSCRIPTION_ID"
  value       = var.subscription_id
}

resource "github_actions_environment_secret" "repo_fe_ci_secrets_tenant_id" {
  for_each    = var.github_federations_fe
  repository  = each.key
  environment = "${each.value}-ci"
  secret_name = "ARM_TENANT_ID"
  value       = var.tenant_id
}

#############################################################################
# BACKEND REPOSITORIES SECRETS
############################################################################_

resource "github_actions_environment_secret" "repo_ms_cd_secrets_client_id" {
  for_each    = var.github_federations
  repository  = each.key
  environment = "${each.value}-cd"
  secret_name = "ARM_CLIENT_ID"
  value       = var.cd_identity_client_id
}

resource "github_actions_environment_secret" "repo_ms_cd_secrets_subscription_id" {
  for_each    = var.github_federations
  repository  = each.key
  environment = "${each.value}-cd"
  secret_name = "ARM_SUBSCRIPTION_ID"
  value       = var.subscription_id
}

resource "github_actions_environment_secret" "repo_ms_cd_secrets_tenant_id" {
  for_each    = var.github_federations
  repository  = each.key
  environment = "${each.value}-cd"
  secret_name = "ARM_TENANT_ID"
  value       = var.tenant_id
}

resource "github_actions_environment_secret" "repo_ms_ci_secrets_client_id" {
  for_each    = var.github_federations
  repository  = each.key
  environment = "${each.value}-ci"
  secret_name = "ARM_CLIENT_ID"
  value       = var.ci_identity_client_id
}

resource "github_actions_environment_secret" "repo_ms_ci_secrets_subscription_id" {
  for_each    = var.github_federations
  repository  = each.key
  environment = "${each.value}-ci"
  secret_name = "ARM_SUBSCRIPTION_ID"
  value       = var.subscription_id
}

resource "github_actions_environment_secret" "repo_ms_ci_secrets_tenant_id" {
  for_each    = var.github_federations
  repository  = each.key
  environment = "${each.value}-ci"
  secret_name = "ARM_TENANT_ID"
  value       = var.tenant_id
}

#############################################################################
# GITHUB PAT VARIABLES REPOSITORIES SECRETS
############################################################################_

resource "github_actions_secret" "repo_fe_cd_secrets_client_id" {
  for_each    = var.github_federations_fe
  repository  = each.key
  secret_name = "GH_PAT_VARIABLES"
  value       = var.gh_pat_variable
}

resource "github_actions_secret" "repo_ms_ci_secrets_client_id" {
  for_each    = var.github_federations
  repository  = each.key
  secret_name = "GH_PAT_VARIABLES"
  value       = var.gh_pat_variable
}

#############################################################################
# FRONTEND REPOSITORIES SECRETS
#############################################################################

resource "github_actions_environment_secret" "repo_fe_cd_secrets_client_id" {
  for_each        = var.github_federations_fe
  repository      = each.key
  environment     = "${each.value}-cd"
  secret_name     = "ARM_CLIENT_ID"
  plaintext_value = var.fe_cd_identity_client_id
}

resource "github_actions_environment_secret" "repo_fe_cd_secrets_subscription_id" {
  for_each        = var.github_federations_fe
  repository      = each.key
  environment     = "${each.value}-cd"
  secret_name     = "ARM_SUBSCRIPTION_ID"
  plaintext_value = var.subscription_id
}

resource "github_actions_environment_secret" "repo_fe_cd_secrets_tenant_id" {
  for_each        = var.github_federations_fe
  repository      = each.key
  environment     = "${each.value}-cd"
  secret_name     = "ARM_TENANT_ID"
  plaintext_value = var.tenant_id
}

resource "github_actions_environment_secret" "repo_fe_ci_secrets_client_id" {
  for_each        = var.github_federations_fe
  repository      = each.key
  environment     = "${each.value}-ci"
  secret_name     = "ARM_CLIENT_ID"
  plaintext_value = var.fe_ci_identity_client_id
}

resource "github_actions_environment_secret" "repo_fe_ci_secrets_subscription_id" {
  for_each        = var.github_federations_fe
  repository      = each.key
  environment     = "${each.value}-ci"
  secret_name     = "ARM_SUBSCRIPTION_ID"
  plaintext_value = var.subscription_id
}

resource "github_actions_environment_secret" "repo_fe_ci_secrets_tenant_id" {
  for_each        = var.github_federations_fe
  repository      = each.key
  environment     = "${each.value}-ci"
  secret_name     = "ARM_TENANT_ID"
  plaintext_value = var.tenant_id
}

#############################################################################
# BACKEND REPOSITORIES SECRETS
############################################################################_

resource "github_actions_environment_secret" "repo_ms_cd_secrets_client_id" {
  for_each        = var.github_federations_ms
  repository      = each.key
  environment     = "${each.value}-cd"
  secret_name     = "ARM_CLIENT_ID"
  plaintext_value = var.ms_cd_identity_client_id
}

resource "github_actions_environment_secret" "repo_ms_cd_secrets_subscription_id" {
  for_each        = var.github_federations_ms
  repository      = each.key
  environment     = "${each.value}-cd"
  secret_name     = "ARM_SUBSCRIPTION_ID"
  plaintext_value = var.subscription_id
}

resource "github_actions_environment_secret" "repo_ms_cd_secrets_tenant_id" {
  for_each        = var.github_federations_ms
  repository      = each.key
  environment     = "${each.value}-cd"
  secret_name     = "ARM_TENANT_ID"
  plaintext_value = var.tenant_id
}

resource "github_actions_environment_secret" "repo_ms_ci_secrets_client_id" {
  for_each        = var.github_federations_ms
  repository      = each.key
  environment     = "${each.value}-ci"
  secret_name     = "ARM_CLIENT_ID"
  plaintext_value = var.ms_ci_identity_client_id
}

resource "github_actions_environment_secret" "repo_ms_ci_secrets_subscription_id" {
  for_each        = var.github_federations_ms
  repository      = each.key
  environment     = "${each.value}-ci"
  secret_name     = "ARM_SUBSCRIPTION_ID"
  plaintext_value = var.subscription_id
}

resource "github_actions_environment_secret" "repo_ms_ci_secrets_tenant_id" {
  for_each        = var.github_federations_ms
  repository      = each.key
  environment     = "${each.value}-ci"
  secret_name     = "ARM_TENANT_ID"
  plaintext_value = var.tenant_id
}

#############################################################################
# GITHUB PAT VARIABLES REPOSITORIES SECRETS
############################################################################_

resource "github_actions_secret" "repo_fe_cd_secrets_client_id" {
  for_each        = var.github_federations_fe
  repository      = each.key
  secret_name     = "GH_PAT_VARIABLES"
  plaintext_value = var.gh_pat_variable
}

resource "github_actions_secret" "repo_ms_ci_secrets_client_id" {
  for_each        = var.github_federations_ms
  repository      = each.key
  secret_name     = "GH_PAT_VARIABLES"
  plaintext_value = var.gh_pat_variable
}

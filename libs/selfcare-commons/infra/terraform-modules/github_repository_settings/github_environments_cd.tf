resource "github_repository_environment" "github_repository_environment_dev_cd" {
  environment = "dev-cd"
  repository  = var.github.repository
}

resource "github_repository_environment" "github_repository_environment_uat_cd" {
  environment = "uat-cd"
  repository  = var.github.repository

  deployment_branch_policy {
    protected_branches     = local.uat.cd.protected_branches
    custom_branch_policies = local.uat.cd.custom_branch_policies
  }
}

resource "github_repository_environment" "github_repository_environment_prod_cd" {
  environment = "prod-cd"
  repository  = var.github.repository

  deployment_branch_policy {
    protected_branches     = local.prod.cd.protected_branches
    custom_branch_policies = local.prod.cd.custom_branch_policies
  }
}

resource "github_actions_environment_variable" "env_dev_cd_variables" {
  for_each = local.dev.cd.variables

  repository    = var.github.repository
  environment   = github_repository_environment.github_repository_environment_dev_cd.environment
  variable_name = each.key
  value         = each.value
}

resource "github_actions_environment_secret" "env_dev_cd_secrets" {
  for_each = local.dev.cd.secrets

  repository      = var.github.repository
  environment     = github_repository_environment.github_repository_environment_dev_cd.environment
  secret_name     = each.key
  plaintext_value = each.value
}

resource "github_actions_environment_variable" "env_uat_cd_variables" {
  for_each = local.uat.cd.variables

  repository    = var.github.repository
  environment   = github_repository_environment.github_repository_environment_uat_cd.environment
  variable_name = each.key
  value         = each.value
}

resource "github_actions_environment_secret" "env_uat_cd_secrets" {
  for_each = local.uat.cd.secrets

  repository      = var.github.repository
  environment     = github_repository_environment.github_repository_environment_uat_cd.environment
  secret_name     = each.key
  plaintext_value = each.value
}

resource "github_actions_environment_variable" "env_prod_cd_variables" {
  for_each = local.prod.cd.variables

  repository    = var.github.repository
  environment   = github_repository_environment.github_repository_environment_prod_cd.environment
  variable_name = each.key
  value         = each.value
}

resource "github_actions_environment_secret" "env_prod_cd_secrets" {
  for_each = local.prod.cd.secrets

  repository      = var.github.repository
  environment     = github_repository_environment.github_repository_environment_prod_cd.environment
  secret_name     = each.key
  plaintext_value = each.value
}
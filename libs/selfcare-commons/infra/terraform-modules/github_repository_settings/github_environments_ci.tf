resource "github_repository_environment" "github_repository_environment_dev_ci" {
  environment = "dev-ci"
  repository  = var.github.repository
}

resource "github_repository_environment" "github_repository_environment_uat_ci" {
  environment = "uat-ci"
  repository  = var.github.repository

  reviewers {
    teams = matchkeys(
      data.github_organization_teams.all.teams[*].id,
      data.github_organization_teams.all.teams[*].slug,
      local.uat.ci.reviewers_teams
    )
  }

  deployment_branch_policy {
    protected_branches     = local.uat.ci.protected_branches
    custom_branch_policies = local.uat.ci.custom_branch_policies
  }
}

resource "github_repository_environment" "github_repository_environment_prod_ci" {
  environment = "prod-ci"
  repository  = var.github.repository

  reviewers {
    teams = matchkeys(
      data.github_organization_teams.all.teams[*].id,
      data.github_organization_teams.all.teams[*].slug,
      local.prod.ci.reviewers_teams
    )
  }

  deployment_branch_policy {
    protected_branches     = local.prod.ci.protected_branches
    custom_branch_policies = local.prod.ci.custom_branch_policies
  }
}

resource "github_actions_environment_variable" "env_dev_ci_variables" {
  for_each = local.dev.ci.variables

  repository    = var.github.repository
  environment   = github_repository_environment.github_repository_environment_dev_ci.environment
  variable_name = each.key
  value         = each.value
}

resource "github_actions_environment_secret" "env_dev_ci_secrets" {
  for_each = local.dev.ci.secrets

  repository      = var.github.repository
  environment     = github_repository_environment.github_repository_environment_dev_ci.environment
  secret_name     = each.key
  plaintext_value = each.value
}

resource "github_actions_environment_variable" "env_uat_ci_variables" {
  for_each = local.uat.ci.variables

  repository    = var.github.repository
  environment   = github_repository_environment.github_repository_environment_uat_ci.environment
  variable_name = each.key
  value         = each.value
}

resource "github_actions_environment_secret" "env_uat_ci_secrets" {
  for_each = local.uat.ci.secrets

  repository      = var.github.repository
  environment     = github_repository_environment.github_repository_environment_uat_ci.environment
  secret_name     = each.key
  plaintext_value = each.value
}

resource "github_actions_environment_variable" "env_prod_ci_variables" {
  for_each = local.prod.ci.variables

  repository    = var.github.repository
  environment   = github_repository_environment.github_repository_environment_prod_ci.environment
  variable_name = each.key
  value         = each.value
}

resource "github_actions_environment_secret" "env_prod_ci_secrets" {
  for_each = local.prod.ci.secrets

  repository      = var.github.repository
  environment     = github_repository_environment.github_repository_environment_prod_ci.environment
  secret_name     = each.key
  plaintext_value = each.value
}
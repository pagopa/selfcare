data "github_organization_teams" "all" {
  root_teams_only = true
  summary_only    = true
}

resource "github_repository_environment" "this" {
  environment = "${var.env}-${var.env_suffix}"
  repository  = var.repository

  dynamic "reviewers" {
    for_each = (var.repository_environment.reviewers_teams == null ? [] : [1])
    content {
      teams = matchkeys(
        data.github_organization_teams.all.teams[*].id,
        data.github_organization_teams.all.teams[*].slug,
        var.repository_environment.reviewers_teams
      )
    }
  }

  dynamic "deployment_branch_policy" {
    for_each = var.branch_policy_enabled == true ? [1] : []

    content {
      protected_branches     = var.repository_environment.protected_branches
      custom_branch_policies = var.repository_environment.custom_branch_policies
    }
  }
}

resource "github_repository_environment_deployment_policy" "this" {
  count = var.repository_environment.branch_pattern == null ? 0 : 1

  repository     = var.repository
  environment    = github_repository_environment.this.environment
  branch_pattern = var.repository_environment.branch_pattern != null ? var.repository_environment.branch_pattern : "releases/*"
}

resource "github_actions_environment_secret" "env_secrets" {
  for_each        = var.env_secrets
  repository      = var.repository
  environment     = github_repository_environment.this.environment
  secret_name     = each.key
  plaintext_value = each.value
}

data "azurerm_key_vault_secret" "kv_secrets" {
  for_each     = var.kv_secrets
  name         = each.value
  key_vault_id = var.key_vault_id
}

resource "github_actions_environment_secret" "kv_secrets" {
  for_each        = var.kv_secrets
  repository      = var.repository
  environment     = github_repository_environment.this.environment
  secret_name     = each.key
  plaintext_value = data.azurerm_key_vault_secret.kv_secrets[each.key].value
}

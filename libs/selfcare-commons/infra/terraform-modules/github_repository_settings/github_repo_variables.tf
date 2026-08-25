resource "github_actions_variable" "repo_variables" {
  for_each = local.repo_variables

  repository    = var.github.repository
  variable_name = each.key
  value         = each.value
}
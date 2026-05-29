module "github_environment_opex_ci" {
  source = "../_modules/github_repository_environment"

  env                    = "opex-${local.env}"
  env_suffix             = "ci"
  repository             = local.github.repository
  branch_policy_enabled  = local.github.ci_branch_policy_enabled
  repository_environment = local.github_repository_environment_ci
  env_secrets            = local.env_ci_secrets
}

module "github_environment_opex_cd" {
  source = "../_modules/github_repository_environment"

  env                    = "opex-${local.env}"
  env_suffix             = "cd"
  repository             = local.github.repository
  branch_policy_enabled  = local.github.cd_branch_policy_enabled
  repository_environment = local.github_repository_environment_cd
  env_secrets            = local.env_cd_secrets
}
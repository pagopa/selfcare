module "github_environment_cd" {
  source = "../_modules/github_repository_environment"

  env                    = local.env
  env_suffix             = "cd"
  repository             = local.github.repository
  branch_policy_enabled  = local.github.cd_branch_policy_enabled
  repository_environment = local.github_repository_environment_cd
  env_secrets            = local.env_cd_secrets
}

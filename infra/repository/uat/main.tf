module "repository" {
  source = "../_modules/github-selfcare"

  repository_name = "selfcare"
  prefix          = local.prefix
}

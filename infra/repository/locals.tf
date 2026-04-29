locals {
  repository = {
    name                     = "selfcare"
    description              = "Repository for selfcare services"
    topics                   = ["selfcare"]
    reviewers_teams          = ["selfcare-admin", "selfcare-contributors", "selfcare-onboarding-external-contributors-be", "selfcare-dashboard-external-contributors-be", "selfcare-external-contributors-fe", "engineering-team-devex"]
    default_branch_name      = "main"
    infra_cd_policy_branches = ["main"]
    opex_cd_policy_branches  = ["main"]
    app_cd_policy_branches   = ["main"]
    app_cd_policy_tags       = ["io-services-app-backend@*", "io-services-cms-backoffice@*", "io-services-cms-webapp@*"]
    jira_boards_ids          = ["SELC"]
  }
}

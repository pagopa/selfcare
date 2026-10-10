# GitHub repository module configuration
module "github_repository" {
  source  = "pagopa-dx/github-environment-bootstrap/github"
  version = "~> 1.0"

  repository = {
    name            = "selfcare"
    description     = "Area riservata"
    topics          = ["selc", "selfcare"]
    reviewers_teams = ["selfcare-admin", "selfcare-contributors", "selfcare-onboarding-external-contributors-be", "selfcare-dashboard-external-contributors-be", "selfcare-external-contributors-fe"]
    pages_enabled   = true
    has_downloads   = true
    has_projects    = false
    has_issues      = false
    homepage_url    = "https://selfcare.pagopa.it/"
    environments    = ["dev", "uat", "prod"]
    jira_boards_ids = ["SELC"]
  }
}
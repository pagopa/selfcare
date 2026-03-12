resource "github_repository" "this" {
  name        = "selfcare"
  description = ""

  #tfsec:ignore:github-repositories-private
  visibility = "public"

  allow_auto_merge            = false
  allow_rebase_merge          = true
  allow_merge_commit          = true
  allow_squash_merge          = true
  squash_merge_commit_title   = "COMMIT_OR_PR_TITLE"
  squash_merge_commit_message = "COMMIT_MESSAGES"

  delete_branch_on_merge = true

  has_projects    = true
  has_wiki        = true
  has_discussions = false
  has_issues      = true
  has_downloads   = true

  topics = []

  vulnerability_alerts = true

  security_and_analysis {
    secret_scanning {
      status = "enabled"
    }

    secret_scanning_push_protection {
      status = "enabled"
    }
  }
}

module "repository" {
  source = "github.com/pagopa/selfcare-commons//infra/terraform-modules/github_repository_settings?ref=main"

  github = {
    repository = "selfcare"
  }
}
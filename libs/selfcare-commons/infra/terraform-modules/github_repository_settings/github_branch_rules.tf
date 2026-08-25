resource "github_branch_default" "default_main" {
  repository = var.github.repository
  branch     = "main"
}

resource "github_branch_protection" "protection_main" {
  repository_id = var.github.repository
  pattern       = "main"

  required_status_checks {
    strict   = false
    contexts = []
  }

  require_conversation_resolution = true
  require_signed_commits          = true

  required_pull_request_reviews {
    dismiss_stale_reviews           = false
    require_code_owner_reviews      = true
    required_approving_review_count = 1
  }

  allows_deletions = false
}

resource "github_branch_protection" "protection_releases" {
  repository_id = var.github.repository
  pattern       = "releases/*"

  required_status_checks {
    strict   = true
    contexts = []
  }

  require_conversation_resolution = true
  require_signed_commits          = true

  required_pull_request_reviews {
    dismiss_stale_reviews           = true
    require_code_owner_reviews      = true
    required_approving_review_count = 1
    require_last_push_approval      = true

    pull_request_bypassers = [
      data.github_team.team_admins.node_id
    ]
  }

  allows_deletions = false
}

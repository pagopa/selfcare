terraform {
  required_version = ">= 1.6.0"

  backend "azurerm" {}
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
}

module "repository" {
  source = "github.com/pagopa/selfcare-commons//infra/terraform-modules/github_repository_settings?ref=main"

  github = {
    repository = "selfcare-dashboard-backend"
  }
}
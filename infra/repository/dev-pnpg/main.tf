terraform {
  required_version = ">= 1.6.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "<= 3.112.0"
    }
    github = {
      source  = "integrations/github"
      version = "5.45.0"
    }
  }

  backend "azurerm" {
    resource_group_name  = "terraform-state-rg"
    storage_account_name = "tfappdevselfcare"
    container_name       = "terraform-state"
    key                  = "selfcare.repository.tfstate"
  }
}

provider "azurerm" {
  features {}
}

provider "github" {
  owner = "pagopa"
}

data "azurerm_client_config" "current" {}

data "azurerm_subscription" "current" {}

module "repository" {
  source = "../_modules/github-selfcare"

  repository_name = "selfcare"
  prefix          = local.prefix
}

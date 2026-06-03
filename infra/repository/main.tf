terraform {

  required_providers {

    github = {
      source  = "integrations/github"
      version = "~> 6.0"
    }
  }

  backend "azurerm" {
    resource_group_name  = "terraform-state-rg"
    storage_account_name = "tfappprodselfcare"
    container_name       = "terraform-state"
    key                  = "selfcare.repository.tfstate"
  }
}

provider "github" {
  owner = "pagopa"
}

data "azurerm_client_config" "current" {}

data "azurerm_subscription" "current" {}
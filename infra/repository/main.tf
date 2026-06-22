terraform {
  required_version = ">=1.10.0"
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

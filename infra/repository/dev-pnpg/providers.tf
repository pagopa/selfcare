terraform {
  required_version = ">= 1.6.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
    dx = {
      source  = "pagopa-dx/azure"
      version = "~> 0.0"
    }
    github = {
      source  = "integrations/github"
      version = "5.45.0"
    }
  }

  backend "azurerm" {
    resource_group_name  = "io-infra-rg"
    storage_account_name = "selcdstinfraterraform"
    container_name       = "azurermstate"
    key                  = "selc.infra.repository.dev.tfstate"
    use_azuread_auth     = true
  }
}

provider "azurerm" {
  features {}
}

provider "dx" {}

provider "github" {
  owner = "pagopa"
}

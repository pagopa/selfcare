terraform {
  required_version = ">=1.6.0"

  required_providers {
    azuread = {
      source  = "hashicorp/azuread"
      version = "2.30.0"
    }
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "<= 3.116.0"
    }
    github = {
      source  = "integrations/github"
      version = "5.18.3"
    }
  }

  backend "azurerm" {}
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
}

provider "github" {
  owner = "pagopa"
}

data "azurerm_subscription" "current" {}

data "azurerm_client_config" "current" {}
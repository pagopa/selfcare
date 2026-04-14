terraform {
  required_version = ">=1.10.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.64"
    }
    github = {
      source  = "integrations/github"
      version = "~> 6.0"
    }

  }

  backend "azurerm" {
    resource_group_name  = "io-infra-rg"
    storage_account_name = "selcdstinfraterraform"
    container_name       = "azurermstate"
    key                  = "selc.infra.bootstrap.dev.tfstate"
    use_azuread_auth     = true
  }
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy = false
    }
  }
  storage_use_azuread = true
}

provider "github" {
  owner = "pagopa"
}

data "azurerm_subscription" "current" {}

data "azurerm_client_config" "current" {}

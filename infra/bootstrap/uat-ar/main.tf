terraform {
  required_version = ">=1.10.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.64"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "~> 3.8"
    }
    github = {
      source  = "integrations/github"
      version = "~> 6.0"
    }

  }

  backend "azurerm" {
    # resource_group_name  = "io-infra-rg"
    # storage_account_name = "selcustinfraterraform"
    # container_name       = "azurermstate"
    # key                  = "selc.infra.bootstrap.uat.tfstate"
    resource_group_name  = "terraform-state-rg"
    storage_account_name = "tfappuatselfcare"
    container_name       = "terraform-state"
    key                  = "selc.infra.bootstrap.uat.tfstate"
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

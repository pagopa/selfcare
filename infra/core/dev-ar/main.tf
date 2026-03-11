terraform {
  required_version = ">=1.10.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }

    azuread = {
      source  = "hashicorp/azuread"
      version = ">= 3.3.0"
    }

    random = {
      source  = "hashicorp/random"
      version = ">= 3.0.0"
    }

    pkcs12 = {
      source  = "chilicat/pkcs12"
      version = "0.0.7"
    }

  }

  backend "azurerm" {
    resource_group_name  = "io-infra-rg"
    storage_account_name = "selcdstinfraterraform"
    container_name       = "azurermstate"
    key                  = "selc.infra.core.dev.tfstate"
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

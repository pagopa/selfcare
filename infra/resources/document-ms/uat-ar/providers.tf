terraform {
  required_version = ">=1.10.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
    azapi = {
      source  = "azure/azapi"
      version = "> 2.0.0"
    }
  }

  backend "azurerm" {
    resource_group_name  = "io-infra-rg"
    storage_account_name = "selcdstinfraterraform"
    container_name       = "azurermstate"
    key                  = "selc.infra.resources.document-ms.uat-ar.tfstate"
    use_azuread_auth     = true
  }
}

provider "azurerm" {
  features {}
  storage_use_azuread = true
}

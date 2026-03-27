terraform {
  required_version = ">= 1.10.0"

  required_providers {
    azurerm = { source = "hashicorp/azurerm", version = "~> 4.0" }
    dx      = { source = "pagopa-dx/azure", version = "~> 0.0" }
    random  = { source = "hashicorp/random", version = ">= 3.0.0" }
    azapi   = { source = "azure/azapi", version = "> 2.0.0" }
  }

  backend "azurerm" {
    resource_group_name  = "io-infra-rg"
    storage_account_name = "selcpstinfraterraform"
    container_name       = "azurermstate"
    key                  = "selc.infra.resources.webhook.prod-ar.tfstate"
    use_azuread_auth     = true
  }
}

provider "azurerm" { features {}; storage_use_azuread = true }
provider "dx" {}

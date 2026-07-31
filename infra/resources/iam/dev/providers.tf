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
    storage_account_name = "selcdstinfraterraform"
    container_name       = "azurermstate"

    # New state file. The resources it manages are NOT new: most are adopted from
    # selc.infra.resources.iam.dev-ar.tfstate, and the PNPG APIM API is adopted
    # from selc.infra.resources.iam.dev-pnpg.tfstate. Applying this stack without
    # first importing them would try to create resources that already exist. See
    # README.md for the exact import list.
    key              = "selc.infra.resources.iam.dev.tfstate"
    use_azuread_auth = true
  }
}

provider "azurerm" {
  features {}
  storage_use_azuread = true
}

provider "dx" {}

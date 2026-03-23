terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "> 4.0.0"
    }

    restapi = {
      source  = "mastercard/restapi"
      version = "2.0.1"
    }

    azapi = {
      source  = "azure/azapi"
      version = "~> 2.0"
    }
  }
}

provider "azurerm" {
  features {}
}

provider "restapi" {
  alias                = "search"
  uri                  = "https://${azurerm_search_service.srch_service.name}.search.windows.net"
  write_returns_object = true
  debug                = true
  insecure             = true

  headers = {
    "api-key"      = azurerm_search_service.srch_service.primary_key,
    "Content-Type" = "application/json"
  }

  create_method  = "POST"
  update_method  = "PUT"
  destroy_method = "DELETE"
}

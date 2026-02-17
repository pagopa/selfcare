variable "env" {
  type        = string
  description = "Environment name (e.g. dev, uat, prod)"
}


variable "env_short" {
  type        = string
  description = "Short environment name (e.g. d, u, p)"
}

variable "location_short" {
  type        = string
  description = "Short location name (e.g. weu, itn, neu)"
}

# CDN
variable "checkout_cdn_name" {
  type        = string
  description = "CDN endpoint name (module.checkout_cdn.name)"
}

variable "checkout_cdn_storage_primary_access_key" {
  type        = string
  sensitive   = true
  description = "CDN storage account primary access key (module.checkout_cdn.storage_primary_access_key)"
}

# Resource Group
variable "checkout_fe_rg_name" {
  type        = string
  description = "Checkout frontend resource group name (azurerm_resource_group.checkout_fe_rg.name)"
}

# Contract Storage
variable "selc_contracts_storage_name" {
  type        = string
  description = "Contracts storage account name (module.selc-contracts-storage.name)"
}

variable "selc_contracts_storage_primary_access_key" {
  type        = string
  sensitive   = true
  description = "Contracts storage account primary access key (module.selc-contracts-storage.primary_access_key)"
}

variable "selc_contracts_container_name" {
  type        = string
  description = "Contracts storage container name (azurerm_storage_container.selc-contracts-container.name)"
}

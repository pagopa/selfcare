variable "prefix" {
  type    = string
  default = "selc"
}

variable "env_short" {
  type = string
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "tags" {
  type    = map(any)
  default = {}
}

variable "dns_zone_prefix" {
  type    = string
  default = "selfcare"
}

variable "external_domain" {
  type    = string
  default = "pagopa.it"
}

variable "spa" {
  type    = list(string)
  default = ["auth", "onboarding", "dashboard"]
}

variable "robots_indexed_paths" {
  type        = list(string)
  description = "List of cdn paths to allow robots index"
}

variable "storage_use_case" {
  type        = string
  default     = "development"
  description = "Storage account use case (development, default, audit, etc.)"
}

# From other modules
variable "log_analytics_workspace_id" {
  type        = string
  description = "Log Analytics Workspace ID from monitor module"
}

variable "key_vault_id" {
  type        = string
  description = "Key Vault ID"
}

variable "key_vault_name" {
  type        = string
  description = "Key Vault name (for custom domain certificate)"
}

variable "key_vault_resource_group_name" {
  type        = string
  description = "Key Vault resource group name (for custom domain certificate)"
}

variable "cdn_certificate_name" {
  type        = string
  description = "Name of the Key Vault certificate for the custom domain"
}

variable "rg_vnet_name" {
  type        = string
  description = "VNet resource group name (for DNS zone)"
}
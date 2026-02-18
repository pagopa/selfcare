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

variable "storage_account_replication_type" {
  type        = string
  default     = "ZRS"
  description = "Storage account replication type"
}

# From other modules
variable "log_analytics_workspace_id" {
  type        = string
  description = "Log Analytics Workspace ID from monitor module"
}

variable "key_vault_id" {
  type        = string
  description = "Key Vault ID (used for secrets and custom domain certificates)"
}

variable "tenant_id" {
  type        = string
  description = "Azure AD tenant ID for Front Door managed identity access to Key Vault"
}

variable "rg_vnet_name" {
  type        = string
  description = "VNet resource group name (for DNS zone)"
}
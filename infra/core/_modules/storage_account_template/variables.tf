variable "project" {
  type        = string
  description = "Project name, used as prefix for all resources"
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "tags" {
  type    = map(any)
  default = {}
}

variable "name" {
  type        = string
  description = "Name of the storage account context (e.g. 'contracts', 'logs')"
}

variable "storage_account_name" {
  type        = string
  description = "Exact name of the storage account (before removing dashes)"
}

variable "account_replication_type" {
  type    = string
  default = "ZRS"
}

variable "enable_versioning" {
  type    = bool
  default = false
}

variable "advanced_threat_protection" {
  type    = bool
  default = false
}

variable "delete_retention_days" {
  type    = number
  default = 0
}

variable "public_network_access_enabled" {
  type    = bool
  default = false
}

variable "key_vault_id" {
  type        = string
  description = "Key Vault ID for storing connection strings"
}

variable "cidr_subnet" {
  type        = list(string)
  description = "Storage address space."
}

variable "rg_vnet_name" {
  type        = string
  description = "Resource group name for the VNet (for DNS zone)"
}

variable "vnet_name" {
  type        = string
  description = "VNet name for the VNet (for DNS zone)"
}

variable "private_endpoint_network_policies" {
  type    = string
  default = "Enabled"
}

variable "private_dns_zone_ids" {
  type    = list(string)
  default = []
}

variable "enable_management_lock" {
  type    = bool
  default = false
}

variable "enable_spid_logs_encryption_keys" {
  type    = bool
  default = false
}

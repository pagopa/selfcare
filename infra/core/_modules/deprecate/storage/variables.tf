variable "prefix" {
  type    = string
  default = "selc"
}

variable "env_short" {
  type = string
}

variable "env" {
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

variable "project" {
  type        = string
  description = "Project name, used as prefix for all resources"
}

variable "contracts_enable_versioning" {
  type        = bool
  description = "Enable contract versioning"
  default     = false
}

variable "contracts_advanced_threat_protection" {
  type        = bool
  description = "Enable contract threat advanced protection"
  default     = false
}

variable "contracts_delete_retention_days" {
  type        = number
  description = "Number of days to retain deleted blobs (0 to disable)"
  default     = 0
}

variable "cidr_subnet_contract_storage" {
  type        = list(string)
  description = "Contracts storage address space."
}

variable "private_endpoint_network_policies" {
  type        = string
  description = "Private endpoint network policies"
  default     = "Enabled"
}

variable "contracts_account_replication_type" {
  type        = string
  description = "Contracts replication type"
  default     = "LRS"
}

variable "rg_vnet_name" {
  type        = string
  description = "Resource group name for the VNet (for DNS zone)"
}

variable "vnet_name" {
  type        = string
  description = "VNet name for the VNet (for DNS zone)"
}

variable "key_vault_id" {
  type        = string
  description = "Key Vault ID for storing connection strings"
}

variable "private_dns_zone_ids" {
  type        = list(string)
  description = "List of private DNS zone IDs to associate with the storage private endpoint"
  default     = []
}

variable "logs_account_replication_type" {
  type        = string
  description = "logs replication type"
  default     = "ZRS"
}

variable "logs_enable_versioning" {
  type        = bool
  description = "Enable logs versioning"
  default     = false
}

variable "logs_advanced_threat_protection" {
  type        = bool
  description = "Enable logs threat advanced protection"
  default     = false
}

variable "cosmosdb_mongodb_public_network_access_enabled" {
  type        = bool
  description = "Whether or not public network access is allowed for this CosmosDB account"
  default     = false
}

variable "logs_delete_retention_days" {
  type        = number
  description = "Number of days to retain deleted logs"
  default     = 1
}

variable "cidr_subnet_logs_storage" {
  type        = list(string)
  description = "Logs storage address space."
}

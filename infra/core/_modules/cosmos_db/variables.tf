variable "prefix" {
  type    = string
  default = "selc"
}

variable "env_short" {
  type = string
}

variable "project" {
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

variable "external_domain" {
  type    = string
  default = "pagopa.it"
}

# Network
variable "rg_vnet_name" {
  type        = string
  description = "VNet resource group name"
}

variable "vnet_name" {
  type        = string
  description = "VNet name"
}

variable "cidr_subnet_cosmosdb_mongodb" {
  type        = list(string)
  description = "CIDR block for CosmosDB MongoDB subnet"
}

variable "private_endpoint_network_policies" {
  type        = string
  default     = "Enabled"
  description = "Private endpoint network policies for subnets"
}

# Key Vault
variable "key_vault_id" {
  type        = string
  description = "Key Vault ID for storing connection strings"
}

# Private DNS
variable "privatelink_mongo_cosmos_azure_com_id" {
  type        = string
  description = "Private DNS Zone ID for privatelink.mongo.cosmos.azure.com"
}

# CosmosDB MongoDB
variable "cosmosdb_mongodb_extra_capabilities" {
  type        = list(string)
  default     = []
  description = "Extra capabilities for the CosmosDB MongoDB account (e.g. EnableServerless)"
}

variable "cosmosdb_mongodb_offer_type" {
  type    = string
  default = "Standard"
}

variable "cosmosdb_mongodb_enable_free_tier" {
  type        = bool
  description = "Enable Free Tier pricing option for this Cosmos DB account"
  default     = true
}

variable "cosmosdb_mongodb_public_network_access_enabled" {
  type        = bool
  description = "Whether or not public network access is allowed for this CosmosDB account"
  default     = false
}

variable "cosmosdb_mongodb_private_endpoint_enabled" {
  type        = bool
  description = "Enable private endpoint for Comsmos DB"
  default     = true
}

variable "cosmosdb_mongodb_consistency_policy" {
  type = object({
    consistency_level       = string
    max_interval_in_seconds = number
    max_staleness_prefix    = number
  })

  default = {
    consistency_level       = "Session"
    max_interval_in_seconds = null
    max_staleness_prefix    = null
  }
}

variable "cosmosdb_mongodb_main_geo_location_zone_redundant" {
  type        = bool
  description = "Enable zone redundant Comsmos DB"
}

variable "cosmosdb_mongodb_additional_geo_locations" {
  type = list(object({
    location          = string
    failover_priority = number
    zone_redundant    = bool
  }))
  description = "The name of the Azure region to host replicated data and the priority to apply starting from 1. Not used when cosmosdb_mongodb_enable_serverless"
  default     = []
}

variable "cosmosdb_mongodb_enable_autoscaling" {
  type        = bool
  description = "It will enable autoscaling mode. If true, cosmosdb_mongodb_throughput must be unset"
  default     = false
}

variable "cosmosdb_mongodb_throughput" {
  type        = number
  description = "The throughput of the MongoDB database (RU/s). Must be set in increments of 100. The minimum value is 400. This must be set upon database creation otherwise it cannot be updated without a manual terraform destroy-apply."
  default     = 400
}

variable "cosmosdb_mongodb_max_throughput" {
  type        = number
  description = "The maximum throughput of the MongoDB database (RU/s). Must be between 4,000 and 1,000,000. Must be set in increments of 1,000. Conflicts with throughput"
  default     = 4000
}

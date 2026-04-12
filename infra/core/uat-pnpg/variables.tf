# ==============================================================================
# Environment identity
# ==============================================================================

variable "env" {
  type        = string
  description = "Environment name (dev, uat, prod)"
}

variable "env_short" {
  type        = string
  description = "Short environment name used in resource names (d, u, p)"
}

variable "aks_platform_env" {
  type        = string
  description = "AKS platform environment identifier (e.g. dev01, prod01)"
}

# ==============================================================================
# DNS
# ==============================================================================

variable "dns_zone_prefix" {
  type        = string
  description = "DNS zone prefix for the environment (e.g. dev.selfcare, selfcare)"
}

variable "dns_zone_prefix_ar" {
  type        = string
  description = "DNS zone prefix for Area Riservata (e.g. dev.areariservata, areariservata)"
}

variable "dns_ns_interop_selfcare" {
  type        = list(string)
  description = "NS records for the interop selfcare DNS delegation"
  default     = []
}

# ==============================================================================
# Certificates
# ==============================================================================

variable "app_gateway_api_certificate_name" {
  type        = string
  description = "Name of the App Gateway API certificate in Key Vault"
}

variable "app_gateway_api_pnpg_certificate_name" {
  type        = string
  description = "Name of the App Gateway PNPG API certificate in Key Vault"
}

# ==============================================================================
# Container App Environment
# NOTE: these must be updated whenever the CAE is recreated
# ==============================================================================

variable "ca_suffix_dns_private_name" {
  type        = string
  description = "DNS suffix of the main Container App Environment"
}

variable "ca_pnpg_suffix_dns_private_name" {
  type        = string
  description = "DNS suffix of the PNPG Container App Environment"
}

variable "auth_ms_private_dns_suffix" {
  type        = string
  description = "Private DNS suffix used by the auth microservice — must match the main CAE domain"
}

# ==============================================================================
# Redis
# ==============================================================================

variable "redis_sku_name" {
  type        = string
  description = "Redis SKU (Basic for dev/uat, Standard for prod)"
  default     = "Basic"
}

variable "redis_capacity" {
  type    = number
  default = 0
}

variable "redis_private_endpoint_enabled" {
  type        = bool
  description = "Enable private endpoint for Redis (false in dev-pnpg)"
  default     = true
}

# ==============================================================================
# AKS
# ==============================================================================

variable "aks_kubernetes_version" {
  type    = string
  default = "1.27.7"
}

variable "aks_sku_tier" {
  type    = string
  default = "Free"
}

variable "aks_upgrade_settings_max_surge" {
  type    = string
  default = null
}

variable "aks_system_node_pool_vm_size" {
  type    = string
  default = "Standard_B4ms"
}

variable "aks_system_node_pool_os_disk_type" {
  type    = string
  default = "Managed"
}

variable "aks_system_node_pool_node_count_min" {
  type    = number
  default = 1
}

variable "aks_system_node_pool_node_count_max" {
  type    = number
  default = 1
}

variable "system_node_pool_enable_host_encryption" {
  type    = bool
  default = false
}

variable "aks_user_node_pool_vm_size" {
  type    = string
  default = "Standard_B4ms"
}

variable "aks_user_node_pool_os_disk_type" {
  type    = string
  default = "Managed"
}

variable "aks_user_node_pool_node_count_min" {
  type    = number
  default = 1
}

variable "aks_user_node_pool_node_count_max" {
  type    = number
  default = 3
}

# ==============================================================================
# Docker Registry
# ==============================================================================

variable "docker_registry_sku" {
  type    = string
  default = "Basic"
}

variable "docker_registry_zone_redundancy_enabled" {
  type    = bool
  default = false
}

variable "docker_registry_geo_replication_enabled" {
  type    = bool
  default = false
}

# ==============================================================================
# Monitoring
# ==============================================================================

variable "law_daily_quota_gb" {
  type        = number
  description = "Log Analytics Workspace daily ingestion quota in GB. Use -1 for unlimited."
  default     = 2
}

# ==============================================================================
# CosmosDB MongoDB
# ==============================================================================

variable "cosmosdb_mongodb_extra_capabilities" {
  type    = list(string)
  default = ["EnableServerless"]
}

variable "cosmosdb_mongodb_main_geo_location_zone_redundant" {
  type    = bool
  default = false
}

variable "cosmosdb_mongodb_additional_geo_locations" {
  type = list(object({
    location          = string
    failover_priority = number
    zone_redundant    = bool
  }))
  default = []
}

variable "cosmosdb_mongodb_enable_autoscaling" {
  type    = bool
  default = true
}

variable "cosmosdb_mongodb_throughput" {
  type    = number
  default = 1000
}

variable "cosmosdb_mongodb_max_throughput" {
  type    = number
  default = 1000
}

variable "cosmosdb_mongodb_private_endpoint_enabled" {
  type    = bool
  default = true
}

# ==============================================================================
# Container App Environments — zone redundancy
# ==============================================================================

variable "cae_zone_redundant" {
  type    = bool
  default = false
}

variable "cae_zone_redundant_pnpg" {
  type    = bool
  default = false
}

# ==============================================================================
# Contracts storage
# ==============================================================================

variable "contracts_enable_versioning" {
  type    = bool
  default = false
}

variable "contracts_delete_retention_days" {
  type    = number
  default = 0
}

# ==============================================================================
# Network
# ==============================================================================

variable "private_endpoint_network_policies" {
  type        = string
  description = "Network policies for private endpoints (Enabled or Disabled)"
  default     = "Enabled"
}

# ==============================================================================
# Feature flags
# ==============================================================================

variable "enable_load_tests_db" {
  type    = bool
  default = true
}

# ==============================================================================
# PNPG — API & JWT configuration
# ==============================================================================

variable "api_gateway_url" {
  type        = string
  description = "Base URL of the PNPG API gateway"
  default     = ""
}

variable "jwt_token_exchange_duration" {
  type    = string
  default = "PT15M"
}

variable "jwt_audience" {
  type    = string
  default = ""
}

variable "jwt_issuer" {
  type    = string
  default = "SPID"
}

variable "jwt_social_expire" {
  type    = string
  default = "10000000"
}

variable "spid_testenv_url" {
  type        = string
  description = "URL of the SPID test environment (uat-pnpg only)"
  default     = ""
}

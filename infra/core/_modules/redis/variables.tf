variable "project" {
  type    = string
  default = "selc"
}

variable "tags" {
  type    = map(any)
  default = {}
}

variable "cidr_subnet_redis" {
  type        = list(string)
  description = "CIDR block for Redis subnet"
}

variable "rg_redis" {
  type = string
}


variable "rg_vnet_name" {
  type = string
}

variable "vnet_id" {
  type = string
}

variable "vnet_name" {
  type = string
}

variable "redis_private_endpoint_enabled" {
  type    = bool
  default = true
}

variable "private_endpoint_network_policies" {
  type        = string
  default     = "Enabled"
  description = "Private endpoint network policies for subnets"
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "redis_capacity" {
  type        = number
  description = "The size of the Redis cache to deploy. Valid values are: 0 (for Basic C0), 1 (for Basic C1), 2 (for Standard S1), 3 (for Standard S2), 4 (for Standard S3), 5 (for Premium P1), 6 (for Premium P2), 7 (for Premium P3), 8 (for Premium P4), 9 (for Premium P5), 10 (for Premium P6), 11 (for Premium P7), 12 (for Premium P8), 13 (for Premium P9), and 14 (for Premium P10)."
  default     = 1
}

variable "redis_version" {
  type        = string
  description = "The version of Redis to deploy. Valid values are: 4, 5, and 6."
  default     = "6"
}

variable "redis_family" {
  type    = string
  default = "C"
}

variable "redis_sku_name" {
  type    = string
  default = "Standard"
}

variable "privatelink_redis_cache_windows_net_ids" {
  type        = list(string)
  default     = []
  description = "Private DNS Zone IDs for privatelink.redis.cache.windows.net"
}

variable "key_vault_id" {
  type        = string
  description = "The ID of the Key Vault to use for Redis"
}
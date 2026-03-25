variable "ca_name" {
  type        = string
  description = "Container App name"
  default     = "cae-cp"
}

variable "ca_rg_name" {
  type        = string
  description = "Container App Resource group name"
  default     = "cae-rg"
}

variable "cae_name" {
  type        = string
  description = "Container App Environment name"
  default     = "cae-cp"
}

variable "cae_rg_name" {
  type        = string
  description = "Container App Environment Resource group name"
  default     = "cae-rg"
}

variable "consumer_group" {
  type        = string
  description = "Eventhub consumer group"
  default     = "party-proxy"
}

variable "env_short" {
  description = "Environment short name"
  default     = "d"
  type        = string
  validation {
    condition = (
      length(var.env_short) <= 1
    )
    error_message = "Max length is 1 chars."
  }
}


variable "key_vault_resource_group_name" {
  type        = string
  description = "Name of Key Vault resource group"
}

variable "key_vault_name" {
  type        = string
  description = "Name of Key Vault"
}

variable "key_vault_event_hub_consumer_key" {
  type        = string
  description = "Name of Key Vault"
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "prefix" {
  description = "Domain prefix"
  type        = string
  default     = "selc"
  validation {
    condition = (
      length(var.prefix) <= 6
    )
    error_message = "Max length is 6 chars."
  }
}

variable "project" {
  type        = string
  description = "Selfcare prefix and short environment"
}

variable "queue_port" {
  type        = string
  description = "Queue base url port"
}

variable "queue_consumer_group" {
  type        = string
  description = "Queue consumer group"
}

variable "queue_topic" {
  type        = string
  description = "Queue topic"
}


variable "queue_url" {
  type        = string
  description = "Queue base url"
}

variable "redis_capacity" {
  type    = number
  default = 0
}

variable "redis_version" {
  type    = number
  default = 6
}

variable "redis_sku_name" {
  type    = string
  default = "Basic"
}

variable "redis_family" {
  type    = string
  default = "C"
}

variable "redis_enable" {
  type    = bool
  default = false
}

variable "redis_private_endpoint_enabled" {
  type    = bool
  default = true
}

variable "cidr_subnet_redis" {
  type        = list(string)
  description = "Redis network address space."
  default     = []
}

variable "search_service_name" {
  type        = string
  description = "Name of ai search service"
}

variable "search_service_key" {
  type        = string
  description = "Key of ai search service"
}


variable "tags" {
  type = map(any)
}


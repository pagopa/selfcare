variable "is_pnpg" {
  type        = bool
  default     = false
  description = "(Optional) True if you want to apply changes to PNPG environment"
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

variable "env_short" {
  description = "Environment short name"
  type        = string
  validation {
    condition = (
      length(var.env_short) <= 1
    )
    error_message = "Max length is 1 chars."
  }
}

variable "tags" {
  type = map(any)
}

variable "container_app" {
  description = "Container App configuration"
  type = object({
    min_replicas = number
    max_replicas = number

    scale_rules = list(object({
      name = string
      custom = object({
        metadata = map(string)
        type     = string
      })
    }))

    cpu    = number
    memory = string
  })
}

variable "image_tag" {
  type    = string
  default = "latest"
}

variable "app_settings" {
  type = list(object({
    name  = string
    value = string
  }))
}

variable "secrets_names" {
  type        = map(string)
  description = "KeyVault secrets to get values from"
}

variable "cosmosdb_mongodb_throughput" {
  type        = number
  description = "The throughput of the MongoDB database (RU/s). Must be set in increments of 100. The minimum value is 400. This must be set upon database creation otherwise it cannot be updated without a manual terraform destroy-apply."
  default     = 1000
}

variable "workload_profile_name" {
  type        = string
  description = "Workload Profile name to use"
  default     = null
}

variable "cae_name" {
  type        = string
  description = "Container App Environment name"
  default     = "cae-cp"
}

variable "suffix_increment" {
  type        = string
  description = "Suffix increment Container App Environment name"
  default     = ""
}

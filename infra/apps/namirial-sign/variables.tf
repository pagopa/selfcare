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

variable "tags" {
  type = map(any)
}

variable "enable_sws" {
  type    = bool
  default = false
}

variable "enable_ca_sws" {
  type    = bool
  default = false
}

variable "environment_variables" {
  type        = map(string)
  description = "environment variables for container"
}

variable "container_config" {
  description = "Container configuration"
  type = object({
    cpu    = number
    memory = number
  })
}

variable "cidr_subnet_namirial_sws" {
  type        = list(string)
  description = "Cosmosdb pnpg address space."
  default     = ["10.1.154.0/29"]
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


variable "app_settings" {
  type = list(object({
    name  = string
    value = string
  }))
}

variable "secrets_names" {
  type        = map(string)
  description = "KeyVault secrets to get values from <env,secret-ref>"
  default     = {}
}

variable "port" {
  type        = number
  default     = 8080
  description = "Container binding port"
}

variable "workload_profile_name" {
  type        = string
  description = "Workload Profile name to use"
  default     = "Consumption"
}
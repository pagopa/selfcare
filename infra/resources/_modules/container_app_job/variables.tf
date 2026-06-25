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
  description = "Container App Job configuration"
  type = object({
    cpu    = number
    memory = string
  })
}

variable "image_tag" {
  type        = string
  default     = "latest"
  description = "Image tag to use for the container"
}

variable "app_settings" {
  type = list(object({
    name                  = string
    value                 = optional(string, "")
    key_vault_secret_name = optional(string)
  }))
}

variable "secrets_names" {
  type        = map(string)
  description = "KeyVault secrets to get values from <env,secret-ref>"
}

variable "image_name" {
  type        = string
  description = "Name of the image to use, hosted on GitHub container registry"
}

variable "container_app_name" {
  type        = string
  description = "Container App Job name suffix"
}

variable "workload_profile_name" {
  type        = string
  description = "Workload Profile name to use"
  default     = "Consumption"
}

variable "container_app_environment_name" {
  type        = string
  description = "Container app environment name to use"
}

variable "resource_group_name" {
  type        = string
  description = "Container app environment resource group name"
}

variable "key_vault_name" {
  type        = string
  description = "Key Vault name (for custom domain certificate)"
}

variable "key_vault_resource_group_name" {
  type        = string
  description = "Key Vault resource group name (for custom domain certificate)"
}

variable "replica_timeout_in_seconds" {
  type        = number
  default     = 28800
  description = "Maximum number of seconds a replica is allowed to run"
}

variable "replica_retry_limit" {
  type        = number
  default     = 0
  description = "Maximum number of retries for a failed replica"
}

variable "schedule_trigger_config" {
  type = list(object({
    cron_expression          = string
    parallelism              = optional(number, 1)
    replica_completion_count = optional(number, 1)
  }))
  default     = []
  description = "Schedule trigger configuration for the container app job"
}

variable "manual_trigger_config" {
  type = list(object({
    parallelism              = optional(number, 1)
    replica_completion_count = optional(number, 1)
  }))
  default     = []
  description = "Manual trigger configuration for the container app job"
}

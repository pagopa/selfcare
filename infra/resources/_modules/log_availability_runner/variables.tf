variable "env_short" {
  type = string
}

variable "environment" {
  type = string

  validation {
    condition     = contains(["DEV", "UAT", "PROD"], var.environment)
    error_message = "Environment must be DEV, UAT or PROD."
  }
}

variable "location" {
  type = string
}

variable "monitor_resource_group_name" {
  type = string
}

variable "container_app_resource_group_name" {
  type = string
}

variable "container_app_environment_name" {
  type = string
}

variable "container_app" {
  type = object({
    cpu    = number
    memory = string
  })
}

variable "key_vault_resource_group_name" {
  type = string
}

variable "key_vault_name" {
  type = string
}

variable "log_analytics_workspace_guid" {
  type = string
}

variable "log_analytics_workspace_resource_id" {
  type = string
}

variable "application_gateway_resource_id" {
  type = string
}

variable "storage_account_id" {
  type = string
}

variable "storage_account_name" {
  type = string
}

variable "image_tag" {
  type    = string
  default = "latest"
}

variable "tags" {
  type    = map(string)
  default = {}
}

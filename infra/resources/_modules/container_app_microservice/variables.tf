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
      type = optional(string)
      custom = object({
        metadata = map(string)
        type     = string
      })
    }))

    cpu    = number
    memory = string
  })
}

variable "probes" {
  type = list(object({
    type                = string
    timeoutSeconds      = number
    failureThreshold    = number
    initialDelaySeconds = number
    httpGet = object({
      path   = string
      scheme = string
      port   = number
    })
  }))
  default = [
    {
      httpGet = {
        path   = "actuator/health"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 30
      type                = "Liveness"
      failureThreshold    = 3
      initialDelaySeconds = 1
    },
    {
      httpGet = {
        path   = "actuator/health"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 30
      type                = "Readiness"
      failureThreshold    = 30
      initialDelaySeconds = 3
    },
    {
      httpGet = {
        path   = "actuator/health"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 30
      failureThreshold    = 30
      type                = "Startup"
      initialDelaySeconds = 30
    }
  ]
}

variable "dapr_settings" {
  type = list(object({
    app_id       = string
    app_port     = string
    app_protocol = string
  }))
  default = []
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
  description = "Container App name suffix"
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

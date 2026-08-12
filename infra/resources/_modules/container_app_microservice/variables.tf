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

variable "tenant_data_isolation_json" {
  type        = string
  default     = null
  description = "Per-tenant data-layer registry (Cosmos DB account, Storage naming, personal data vault, email sender domain) serialized as JSON, i.e. module.local.config.tenant_data_isolation_json. Injected as the SELFCARE_TENANT_DATA_ISOLATION environment variable and parsed by TenantDataIsolationRegistry in libs/selfcare-sdk-security. Non-secret routing data only: credentials appear here as Key Vault secret NAMES, never as values. Set it from module.local rather than writing a literal map, so the registry stays defined once (apps/docs/Multitenant/Step_1/EPIC.md sub-task 9). Leaving it null makes the service reject every tenant-scoped data lookup, which is the intended fail-closed behaviour, not a silent single-tenant mode."
}

variable "strict_tenant_data_isolation" {
  type        = bool
  default     = null
  description = "Whether the service stops treating documents without a tenantId as belonging to the requesting tenant. Injected as SELFCARE_TENANT_STRICT_DATA_ISOLATION and read by every tenant-scoped query builder. Set it from module.local.config.strict_tenant_data_isolation so a whole environment flips at once: turning it on for some services only would leave the environment reporting isolation it does not have. Turn it on for an environment only after apps/docs/Multitenant/Step_1/scripts/backfill_tenant_id.py --verify exits 0 for both tenants there, since before that it hides pre-existing untagged data from everyone. Leaving it null keeps the application default (false, the migration-phase behaviour). Temporary: once every environment runs strict, this variable and the branch it controls are deleted (apps/docs/Multitenant/Step_1/EPIC.md sub-tasks 2 and 10)."
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

variable "restart_alert" {
  description = "Container restart alert configuration"
  type = object({
    enabled              = optional(bool, true)
    action_group_name    = optional(string, "SlackPagoPA")
    action_group_rg_name = optional(string)
    frequency            = optional(string, "PT1M")
    window_size          = optional(string, "PT5M")
    severity             = optional(number, 2)
    threshold            = optional(number, 0)
  })
  default = {}
}

variable "additional_user_assigned_identity_ids" {
  description = "Additional user-assigned identity IDs to attach to the container app"
  type        = list(string)
  default     = []
}

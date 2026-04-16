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

variable "location" {
  type        = string
  description = "One of westeurope, northeurope"
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

variable "key_vault" {
  description = "KeyVault data to get secrets values from"
  type = object({
    resource_group_name = string
    name                = string
  })
}


variable "cidr_subnet_selc_onboarding_fn" {
  type        = list(string)
  description = "Address prefixes subnet selc ca and functions"
  default     = null
}

# Storage account
variable "storage_account_info" {
  type = object({
    account_kind                      = string
    account_tier                      = string
    account_replication_type          = string
    access_tier                       = string
    advanced_threat_protection_enable = bool
    use_legacy_defender_version       = bool
    public_network_access_enabled     = bool
  })

  default = {
    account_kind                      = "StorageV2"
    account_tier                      = "Standard"
    account_replication_type          = "LRS"
    access_tier                       = "Hot"
    advanced_threat_protection_enable = true
    use_legacy_defender_version       = true
    public_network_access_enabled     = false
  }
}

# App service plan
variable "app_service_plan_info" {
  type = object({
    kind = string
    # The kind of the App Service Plan to create. Possible values are Windows (also available as App), Linux, elastic (for Premium Consumption) and FunctionApp (for a Consumption Plan).
    sku_size                     = string # Specifies the plan's instance size.
    maximum_elastic_worker_count = number
    # The maximum number of total workers allowed for this ElasticScaleEnabled App Service Plan.
    worker_count           = number # The number of Workers (instances) to be allocated.
    zone_balancing_enabled = bool
    # Should the Service Plan balance across Availability Zones in the region. Changing this forces a new resource to be created.
  })

  description = "Allows to configurate the internal service plan"

  default = {
    kind                         = "Linux"
    sku_size                     = "S1"
    maximum_elastic_worker_count = 0
    worker_count                 = 0
    zone_balancing_enabled       = false
  }
}

variable "function_always_on" {
  type        = bool
  description = "Always on property"
  default     = false
}


variable "app_settings" {
  type = map(any)
}
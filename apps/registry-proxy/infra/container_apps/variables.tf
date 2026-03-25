variable "is_pnpg" {
  type        = bool
  default     = false
  description = "(Optional) True if you want to apply changes to PNPG environment"
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
  type        = string
  default     = "latest"
  description = "Image tag to use for the container"
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

variable "private_dns_name" {
  type        = string
  default     = "selc-d-party-reg-proxy-ca.gentleflower-c63e62fe.westeurope.azurecontainerapps.io"
  description = "Container Apps private DNS record"
}

variable "external_domain" {
  type        = string
  default     = "pagopa.it"
  description = "Domain for delegation"
}

variable "dns_zone_prefix" {
  type        = string
  default     = "selfcare"
  description = "The dns subdomain."
}

variable "api_dns_zone_prefix" {
  type        = string
  default     = "api.selfcare"
  description = "The dns subdomain."
}

variable "workload_profile_name" {
  type        = string
  description = "Workload Profile name to use"
  default     = null
}

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

variable "key_vault_resource_group_name" {
  type        = string
  description = "Name of Key Vault resource group"
  default = "kv-rg"
}

variable "key_vault_name" {
  type        = string
  description = "Name of Key Vault"
  default = "kv"
}

variable "existing_logs_rg" {
  type        = string
  description = "storage account logs resource group"
  default     = "storage-rg"
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

variable "location" {
  type    = string
  default = "westeurope"
}

variable "cae_rg_name" {
  type        = string
  description = "Container App Environment Resource group name"
  default     = "cae-rg"
}

variable "storage_logs" {
  type        = string
  description = "Storage audit logs name"
  default     = "storagename"
}



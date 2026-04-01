variable "project" {
  type        = string
  description = "Project name for resource naming"
}

variable "prefix" {
  type    = string
  default = "selc"
}

variable "env_short" {
  type = string
}

variable "location_short" {
  type    = string
  default = "weu"
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "tags" {
  type    = map(any)
  default = {}
}

variable "app_name" {
  type        = string
  description = "Application name"
}

variable "instance_number" {
  type        = string
  description = "Instance number for the application (e.g., 01, 02)"
}

variable "host_name" {
  type        = string
  description = "Hostname for the CDN custom domain eg. cdn.selfcare.pagopa.it"
}

variable "prefix_api" {
  type        = string
  description = "Prefix for custom domain endpoint for apim"
  default     = "api"
}

variable "dns_zone_prefix" {
  type    = string
  default = "selfcare"
}

variable "external_domain" {
  type    = string
  default = "pagopa.it"
}

variable "robots_indexed_paths" {
  type        = list(string)
  description = "List of cdn paths to allow robots index"
}

variable "storage_use_case" {
  type        = string
  default     = "development"
  description = "Storage account use case (development, default, audit, etc.)"
}

# From other modules
variable "log_analytics_workspace_id" {
  type        = string
  description = "Log Analytics Workspace ID from monitor module"
}

variable "log_analytics_workspace_enabled" {
  type        = bool
  description = "Flag to enable or disable Log Analytics Workspace integration"
}


variable "key_vault_id" {
  type        = string
  description = "Key Vault ID"
}

variable "key_vault_name" {
  type        = string
  description = "Key Vault name (for custom domain certificate)"
}

variable "key_vault_resource_group_name" {
  type        = string
  description = "Key Vault resource group name (for custom domain certificate)"
}

variable "cdn_certificate_name" {
  type        = string
  description = "Name of the Key Vault certificate for the custom domain"
  default     = null
}

variable "rg_vnet_name" {
  type        = string
  description = "VNet resource group name (for DNS zone)"
}

variable "vnet_name" {
  type        = string
  description = "VNet name (for DNS zone)"
}

variable "domain" {
  type        = string
  description = "Logic domain (e.g., ar, pg)"
}

variable "cidr_subnet_cdn" {
  type        = list(string)
  description = "Storage CDN address space."
}

variable "spa" {
  type        = list(string)
  description = "spa root dirs"
  default = [
    "auth",
    "onboarding",
    "dashboard"
  ]
}

variable "create_snet" {
  type        = bool
  default     = true
  description = "Create a snet or read default cdn snet"
}

variable "origin_health_probe" {
  type = object({
    path         = optional(string, "/")
    request_type = optional(string, "HEAD")
  })

  description = "Health probe configuration of the CDN origin group"

  default = {}
}
# general
locals {
  project       = "${var.prefix}-${var.env_short}"
  rg_apim_name  = "${local.project}-api-v2-rg"
  api_domain    = "api.${var.dns_zone_prefix}.${var.external_domain}"
  apim_base_url = "${local.api_domain}/external"

  # Tenant ids are derived from the shared tenant_registry (single source of truth,
  # same as local-env), instead of being hardcoded in each JWT policy template.
  tenant_id_ar   = one([for id, tenant in var.tenant_registry : id if tenant.authentication_provider == "ONE_IDENTITY"])
  tenant_id_pnpg = one([for id, tenant in var.tenant_registry : id if tenant.authentication_provider == "HUB_SPID_LOGIN"])
}

variable "prefix" {
  type    = string
  default = "selc"
  validation {
    condition = (
      length(var.prefix) <= 6
    )
    error_message = "Max length is 6 chars."
  }
}

variable "env" {
  type        = string
  description = "env directory name"
}

variable "env_short" {
  type = string
  validation {
    condition = (
      length(var.env_short) <= 1
    )
    error_message = "Max length is 1 chars."
  }
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "location_short" {
  type    = string
  default = "weu"
}

variable "tags" {
  type = map(any)
  default = {
    CreatedBy = "Terraform"
  }
}

# apim
variable "apim_publisher_name" {
  type = string
}

variable "apim_sku" {
  type = string
}

variable "private_dns_name" {
  type        = string
  description = "AKS private DNS record"
}

variable "private_onboarding_dns_name" {
  type        = string
  description = "AKS private onboarding DNS record"
}

variable "dns_zone_prefix" {
  type        = string
  default     = "selfcare"
  description = "The dns subdomain."
}

variable "external_domain" {
  type        = string
  default     = "pagopa.it"
  description = "Domain for delegation"
}

variable "cidr_subnet_apim" {
  type        = list(string)
  description = "Address prefixes subnet api management."
  default     = null
}

variable "app_gateway_api_certificate_name" {
  type        = string
  description = "Application gateway: api certificate name on Key Vault"
}

variable "ca_suffix_dns_private_name" {
  type        = string
  description = "CA suffix private DNS record"
}

variable "ca_pnpg_suffix_dns_private_name" {
  type        = string
  description = "CA PNPG suffix private DNS record"
}


variable "domain" {
  type = string
  validation {
    condition = (
      length(var.domain) <= 12
    )
    error_message = "Max length is 12 chars."
  }
  default = "pnpg"
}

variable "developer_path" {
  type        = string
  description = "Path where is located developer index.html file"
}

variable "tenant_registry" {
  type = map(object({
    frontend_uri            = string
    api_uri                 = string
    allowed_origins         = list(string)
    authentication_provider = string
    auth_enabled            = bool
  }))
  description = <<-EOT
    Registry of supported tenants for the current environment (same source as local-env's
    tenant_registries), used to derive the tenant_id embedded in the session JWTs minted by
    APIM and in the corresponding X-Tenant-Id header, instead of hardcoding "AR"/"PNPG" in
    the policy templates.
  EOT

  validation {
    condition     = length(var.tenant_registry) > 0
    error_message = "tenant_registry must not be empty."
  }
}
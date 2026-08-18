# General Variables
variable "apim_name" {
  type        = string
  description = "The name of the API Management instance."
}

variable "apim_rg" {
  type        = string
  description = "The name of the resource group in which the API Management instance exists."
}

variable "api_name" {
  type        = string
  description = "The name of the API in the API Management instance."
}

variable "display_name" {
  type        = string
  description = "The display name of the API in the API Management instance."
}

variable "base_path" {
  type        = string
  description = "The base path of the API in the API Management instance."
}

variable "private_dns_name" {
  type        = string
  description = "The private DNS name of the API in the API Management instance."
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

variable "external_domain" {
  type        = string
  default     = "pagopa.it"
  description = "Domain for delegation"
}

variable "openapi_path" {
  type        = string
  description = "Path to the OpenAPI specification file."
}

variable "tenant_hosts" {
  type = list(object({
    id   = string
    host = string
  }))
  default     = []
  description = <<-EOT
    Mapping between the APIM gateway hostname a request arrives on and the canonical tenant id.
    The hostname is bound to DNS and the TLS certificate, so unlike Origin/Referer it cannot be
    chosen by the caller. Required when tenant_enforcement_enabled is true.
  EOT

  validation {
    condition     = length(distinct([for tenant in var.tenant_hosts : lower(tenant.host)])) == length(var.tenant_hosts)
    error_message = "Each host in tenant_hosts must be unique."
  }

  validation {
    condition = alltrue([
      for tenant in var.tenant_hosts :
      can(regex("^[a-zA-Z0-9.-]+$", tenant.host)) && can(regex("^[A-Z][A-Z0-9_]*$", tenant.id))
    ])
    error_message = "Each tenant host must be a bare hostname (no scheme, port or path) and each tenant id must be uppercase."
  }
}

variable "tenant_enforcement_enabled" {
  type        = bool
  default     = false
  description = <<-EOT
    Enables the fail-closed tenant gate: the tenant is resolved from the APIM hostname the request
    arrived on and requests that cannot be attributed to a tenant are rejected with 403.
    Enable it only on APIs whose backend consumes the X-Tenant-Id header, and make sure every
    gateway hostname serving the API is listed in tenant_hosts.
    When disabled, any caller-supplied X-Tenant-Id is stripped.
  EOT

  validation {
    condition     = !var.tenant_enforcement_enabled || length(var.tenant_hosts) > 0
    error_message = "tenant_hosts must be provided when tenant_enforcement_enabled is true."
  }
}

variable "allowed_headers" {
  type        = list(string)
  default     = ["*"]
  description = "CORS allowed request headers. Defaults to '*' to preserve existing frontend behaviour."

  validation {
    condition     = length(var.allowed_headers) > 0
    error_message = "allowed_headers must not be empty."
  }
}

variable "tenant_ids" {
  type = list(object({
    id     = string
    origin = string
  }))
  description = "Allowed frontend origins and their canonical tenant identifiers."

  validation {
    condition     = length(var.tenant_ids) > 0 && length(distinct([for tenant in var.tenant_ids : lower(tenant.origin)])) == length(var.tenant_ids)
    error_message = "tenant_ids must contain at least one unique origin."
  }

  validation {
    condition = alltrue([
      for tenant in var.tenant_ids :
      (
        can(regex("^https://[^/]+$", tenant.origin)) ||
        can(regex("^http://localhost(:[0-9]+)?$", tenant.origin))
      ) &&
      can(regex("^[A-Z][A-Z0-9_]*$", tenant.id))
    ])
    error_message = "Each tenant origin must be HTTPS without a path (HTTP is allowed only for localhost) and each tenant id must be uppercase."
  }
}

variable "api_operation_policies" {
  type = list(object({
    operation_id = string
    xml_content  = string
    }
  ))
  default     = []
  description = "List of api policy for given operation."
}
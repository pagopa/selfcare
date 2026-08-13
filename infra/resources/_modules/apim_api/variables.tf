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

variable "default_tenant_id" {
  type        = string
  default     = null
  nullable    = true
  description = "Tenant used only for the explicitly allowed operations when caller origin cannot be resolved."

  validation {
    condition = (
      var.default_tenant_id == null
      || contains([for tenant in var.tenant_ids : tenant.id], var.default_tenant_id)
    )
    error_message = "default_tenant_id must be null or reference a tenant declared in tenant_ids."
  }
}

variable "default_tenant_operation_ids" {
  type        = list(string)
  default     = []
  description = "Operation IDs allowed to use default_tenant_id when caller origin cannot be resolved."

  validation {
    condition = alltrue([
      for operation_id in var.default_tenant_operation_ids :
      can(regex("^[A-Za-z0-9._-]+$", operation_id))
    ])
    error_message = "default_tenant_operation_ids may contain only letters, digits, dots, underscores and hyphens."
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
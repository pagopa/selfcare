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

variable "api_operation_policies" {
  type = list(object({
    operation_id = string
    xml_content  = string
    }
  ))
  default     = []
  description = "List of api policy for given operation."
}

variable "tenant_ids" {
  type = list(object({
    id     = string
    origin = string
  }))
  description = "Tenants served by this API group (multitenant migration, see apps/docs/Multitenant/Step_0/REQUIREMENTS.md SELC-1/SELC-2). Each entry maps a tenant id (AR/PNPG) to its frontend origin (scheme + host, e.g. https://selfcare.pagopa.it). APIM allow-lists every listed origin for CORS and resolves X-Tenant-Id at request time from the calling Origin/Referer against this list, always discarding any X-Tenant-Id sent by the caller. The first entry is used as the fallback tenant when a request carries no Origin/Referer header (server-to-server calls, health checks)."

  validation {
    condition     = length(var.tenant_ids) > 0 && alltrue([for t in var.tenant_ids : contains(["AR", "PNPG"], t.id)])
    error_message = "tenant_ids must be non-empty and every entry's id must be AR or PNPG."
  }
}
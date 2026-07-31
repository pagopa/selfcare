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
  description = "Tenants served by this API group (multitenant migration, see apps/docs/Multitenant/Step_0/REQUIREMENTS.md SELC-1/SELC-2). Each entry maps a tenant id (AR/PNPG) to its frontend origin (scheme + host, e.g. https://selfcare.pagopa.it). APIM allow-lists every listed origin for CORS and resolves X-Tenant-Id at request time by matching the calling origin EXACTLY (scheme + authority, Referer reduced to the same form) against this list, always discarding any X-Tenant-Id sent by the caller. Requests whose origin is not listed are rejected with 403; requests with no Origin/Referer at all are governed by var.default_tenant_id, not by this list."

  validation {
    condition     = length(var.tenant_ids) > 0 && alltrue([for t in var.tenant_ids : contains(["AR", "PNPG"], t.id)])
    error_message = "tenant_ids must be non-empty and every entry's id must be AR or PNPG."
  }
}
variable "local_development_origins" {
  type        = list(string)
  default     = []
  description = "Extra origins allowed for local frontend development (e.g. http://localhost:3000). These are added to the CORS allow-list AND resolve to tenant_ids[0]. CORS here runs with allow-credentials=true, so a localhost origin lets any process listening on that port on a user's machine issue credentialed calls to this API: it MUST stay empty outside dev. Populated centrally from module.local.config.local_development_origins, which is non-empty only when env is dev."
}

variable "service_caller_tenants" {
  type        = map(string)
  default     = {}
  description = "Maps an APIM subscription id (map key) to the tenant assigned to it (map value, AR or PNPG). Applies only to server-to-server callers, i.e. requests carrying neither Origin nor Referer, and is evaluated BEFORE default_tenant_id. This is the only supported way for a backend service calling this API through APIM to be attributed to a tenant: the inbound policy overrides X-Tenant-Id unconditionally, so a header set by the calling application is discarded. Prefer one subscription per (calling service, tenant) pair over widening default_tenant_id, so that each s2s caller's tenant is pinned to a credential it cannot change."

  validation {
    condition     = alltrue([for t in values(var.service_caller_tenants) : contains(["AR", "PNPG"], t)])
    error_message = "Every value in service_caller_tenants must be AR or PNPG."
  }
}

variable "default_tenant_id" {
  type        = string
  default     = null
  description = "Tenant assigned to requests that carry neither an Origin nor a Referer header AND whose subscription is not listed in service_caller_tenants. Defaults to null, meaning such requests are REJECTED with 403 (fail-closed). Prefer service_caller_tenants for known server-to-server callers. Only set this on APIs that provably receive non-browser traffic that cannot be enumerated, and keep it reviewable: a blanket default would let any non-browser caller pick a tenant simply by omitting Origin, which is exactly the silent fallback the multitenant DoD forbids (apps/docs/Multitenant/Step_0/EPIC.md sub-task 2)."

  validation {
    condition     = var.default_tenant_id == null || contains(["AR", "PNPG"], coalesce(var.default_tenant_id, "AR"))
    error_message = "default_tenant_id must be null, AR or PNPG."
  }
}

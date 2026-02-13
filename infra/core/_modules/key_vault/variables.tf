variable "prefix" {
  type    = string
  default = "selc"
}

variable "env_short" {
  type = string
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "tags" {
  type    = map(any)
  default = {}
}

variable "azdo_sp_tls_cert_enabled" {
  type    = string
  default = false
}

variable "azuread_service_principal_azure_cdn_frontdoor_id" {
  type    = string
  default = "f3b3f72f-4770-47a5-8c1e-aa298003be12"
}

variable "app_gateway_api_certificate_name" {
  type        = string
  description = "Application gateway: api certificate name on Key Vault"
}

variable "app_gateway_api_pnpg_certificate_name" {
  type        = string
  description = "Application gateway: api-pnpg certificate name on Key Vault"
}
